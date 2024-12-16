package cn.wolfcode.mq;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@RocketMQMessageListener(
        consumerGroup = MQConstant.ORDER_PENDING_CONSUMER_GROUP,
        topic = MQConstant.ORDER_PENDING_TOPIC

)

@Component
@Slf4j
public class OrderPendingMessageListener implements RocketMQListener<OrderMessage> {
    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Override
    public void onMessage(OrderMessage message) {
        // 7. take message from mq ,and doSeckill
        log.info("[create order] recieve create order news,begin to create order:{}", JSON.toJSONString(message));
        OrderMQResult result = new OrderMQResult();
        result.setToken(message.getToken());
        try {
            String orderNo = orderInfoService.doSeckill(message.getUserPhone(), message.getSeckillId(), message.getTime());
            //create order success
            result.setCode(Result.SUCCESS_CODE);
            result.setMsg("create order success");
        } catch (Exception e) {
            //create order success
            result.setCode(SeckillCodeMsg.SECKILL_ERROR.getCode());
            result.setMsg(SeckillCodeMsg.SECKILL_ERROR.getMsg());
            e.printStackTrace();
        }

        //send order result
        rocketMQTemplate.asyncSend(MQConstant.ORDER_RESULT_TOPIC,result,new DefaultSendCallback("order result!"));
    }
}
