package cn.wolfcode.mq;

import cn.wolfcode.service.IOrderInfoService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
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
    @Override
    public void onMessage(OrderMessage message) {
        // 7. take message from mq ,and doSeckill
        log.info("[create order] recieve create order news,begin to create order:{}", JSON.toJSONString(message));
        orderInfoService.doSeckill(message.getUserPhone(), message.getSeckillId(),message.getTime());
    }
}
