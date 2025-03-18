package cn.wolfcode.web.controller;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.common.web.resolver.RequestUser;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mq.DefaultSendCallback;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.redis.CommonRedisKey;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.AssertUtils;
import cn.wolfcode.util.DateUtil;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/order")
@Slf4j
public class OrderInfoController {
    /**
     * mark those products which has sold out
     */
    private static final Map<Long, Boolean> STOCK_OVER_FLOW_MAP = new ConcurrentHashMap<>();
    private final ISeckillProductService seckillProductService;
    private final StringRedisTemplate redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final IOrderInfoService orderInfoService;

    public OrderInfoController(ISeckillProductService seckillProductService, StringRedisTemplate redisTemplate, RocketMQTemplate rocketMQTemplate, IOrderInfoService orderInfoService) {
        this.seckillProductService = seckillProductService;
        this.redisTemplate = redisTemplate;
        this.rocketMQTemplate = rocketMQTemplate;
        this.orderInfoService = orderInfoService;
    }

    /**
     * search orderinfo
     * @param orderNo
     * @return
     */
    @RequireLogin
    @GetMapping("/find")
    public Result<OrderInfo> findById(String orderNo, @RequestUser
            UserInfo userInfo) {
        OrderInfo orderInfo = orderInfoService.selectByOrderNo(orderNo);
        Long userId = orderInfo.getUserId();
        if (!userInfo.getPhone().equals(userId)) {
            return Result.error(SeckillCodeMsg.REMOTE_DATA_ERROR);
        }
        return Result.success(orderInfo);
    }


    /**
     * 优化前:
     * QPS:90/s
     * 存在线程安全问题（超卖问题）
     *
     * @param time
     * @param seckillId
     * @param userInfo
     * @return
     */
    @RequireLogin
    @RequestMapping("/doSeckill")
    public Result<String> doSeckill(Integer time, Long seckillId, @RequestUser UserInfo userInfo, @RequestHeader("token") String token) throws Exception {

//      //1.判断库存是否已经卖完了,如果已经卖完,直接返回异常信息
        Boolean flag = STOCK_OVER_FLOW_MAP.get(seckillId);
        if (flag != null && flag) {
            return Result.error(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
//        UserInfo userInfo = this.getUserByToken(token);
        // 2. 基于场次+秒杀id获取到秒杀商品对象
        SeckillProductVo vo = seckillProductService.selectByIdAndTime(seckillId, time);
        if (vo == null) {
            return Result.error(SeckillCodeMsg.REMOTE_DATA_ERROR);
        }
        // 3. 判断时间是否大于开始时间 && 小于 开始时间+2小时
        if (!DateUtil.isLegalTime(vo.getStartDate(), time)) {
            return Result.error(SeckillCodeMsg.OUT_OF_SECKILL_TIME_ERROR);
            //throw new BusinessException(SeckillCodeMsg.OUT_OF_SECKILL_TIME_ERROR);
        }
        // 4. 判断用户是否重复下单
        // 基于用户 + 秒杀 id + 场次查询订单, 如果存在订单, 说明用户已经下过单
        String userOrderFlag = SeckillRedisKey.SECKILL_ORDER_HASH.join(seckillId + "");
        Long orderCount = redisTemplate.opsForHash().increment(userOrderFlag, userInfo.getPhone() + "", 1);
//        OrderInfo orderInfo = orderInfoService.selectByUserIdAndSeckillId(userInfo.getPhone(), seckillId, time);
        if (orderCount > 1) {
            return Result.error(SeckillCodeMsg.REPEAT_SECKILL);
        }
        AssertUtils.isTrue(orderCount <= 1, "Repeat orders are not possible");
        String orderNo = null;
        try {
            // 5. 判断库存是否充足
            String hashKey = SeckillRedisKey.SECKILL_ORDER_HASH.join(time + "");
            Long remain = redisTemplate.opsForHash().increment(hashKey, seckillId + "", -1);
            AssertUtils.isTrue(remain >= 0, "You are so late ,The merchandise is sold out.");
            //6.send message to mq
            rocketMQTemplate.asyncSend(
                    MQConstant.ORDER_PENDING_TOPIC,
                    new OrderMessage(time, seckillId, token, userInfo.getPhone()),
                    new DefaultSendCallback("create orderInfo"));
            return Result.success("create the order,please wait a moment!");
        } catch (BusinessException e) {
            STOCK_OVER_FLOW_MAP.put(seckillId, true);
            //delete repeated ORDER_FLAG
            redisTemplate.opsForHash().delete(userOrderFlag, userInfo.getPhone() + "");
            return Result.error(e.getCodeMsg());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.defaultError();
    }

    private UserInfo getUserByToken(String token) {
        return JSON.parseObject(redisTemplate.opsForValue().get(CommonRedisKey.USER_TOKEN.getRealKey(token)), UserInfo.class);
    }
}
