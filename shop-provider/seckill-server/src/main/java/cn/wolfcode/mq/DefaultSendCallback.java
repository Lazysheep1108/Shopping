package cn.wolfcode.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;

@Slf4j
public class DefaultSendCallback implements SendCallback {
    private String tag;

    public DefaultSendCallback(String tag) {
        this.tag = tag;
    }

    @Override
    public void onSuccess(SendResult sendResult) {
        log.info("[{}] message send success,id={}", tag, sendResult.getMsgId());
    }

    @Override
    public void onException(Throwable throwable) {
        log.info("[{}] message send unsuccess,id={}", tag, throwable.getMessage());
    }
}
