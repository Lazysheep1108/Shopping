package cn.wolfcode.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/{token}")
@Component
@Slf4j
public class WebsocketServer {
    public static Map<String,Session> SESSION_MAP = new ConcurrentHashMap<>();
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token){
        log.info("[WebSocket]有新的客戶端连接上来了:{}",token);
        // save connect object
        SESSION_MAP.put(token,session);
    }

    @OnClose
    public void onClose(@PathParam("token") String token){
        log.info("[WebSocket]连接准备关闭:{}",token);
        // save connect object
        SESSION_MAP.remove(token);
    }

    @OnError
    public void onError(Throwable throwable){
        log.info("[WebSocket]连接出现异常:",throwable);
    }

}
