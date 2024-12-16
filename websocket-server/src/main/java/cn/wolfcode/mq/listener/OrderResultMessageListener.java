package cn.wolfcode.mq.listener;

import cn.wolfcode.core.WebsocketServer;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMQResult;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.io.IOException;

@RocketMQMessageListener(
        consumerGroup = MQConstant.ORDER_RESULT_CONSUMER_GROUP,
        topic = MQConstant.ORDER_RESULT_TOPIC
)
@Component
@Slf4j
public class OrderResultMessageListener implements RocketMQListener<OrderMQResult> {
    @Override
    public void onMessage(OrderMQResult orderMQResult) {
        String json = JSON.toJSONString(orderMQResult);
        log.info("[order result] recieve create order  result:{}", json);
        try {
            int count = 0;
            do {
                //get session object
                Session session = WebsocketServer.SESSION_MAP.get(orderMQResult.getToken());
                if (session != null) {

                    //according session object,make data to html
                    session.getBasicRemote().sendText(json);
                    return;
                }
                count++;
                log.info("[order result] this is the {}time to querry session object failure,begin to get the next",count);
                Thread.sleep(300);
            } while (count <= 5);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
