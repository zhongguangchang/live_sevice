package com.sky.websocket;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端实时提醒推送
 * <p>
 * 管理端（Navbar 组件）在页面加载时就建立了一条 WebSocket 长连接，
 * 这里负责在业务发生的瞬间把提醒推过去，运营不用刷新页面就能看到。
 * 如果没有连接（没人开着后台页面），推送就是空操作，不影响业务。
 * <p>
 * 消息格式和管理端约定如下，前端按 type 决定提示音和弹窗样式：
 * <pre>
 *   { "type": 1, "orderId": 9001, "content": "订单 xxx 已支付，请及时接单" }  // 新订单待接单
 *   { "type": 2, "orderId": 9001, "content": "订单 xxx 将于 1 小时后开始" }    // 服务前提醒
 * </pre>
 * <p>
 * 这里统一收口，而不是在各个业务类里各拼一次 JSON：
 * 消息格式一旦变化，只需要改这一个地方。
 */
@Component
@Slf4j
public class AdminNotifier {

    /** 新订单待接单（前端播放 preview.mp3 并弹窗） */
    public static final int TYPE_NEW_ORDER = 1;

    /** 服务前提醒 / 催单（前端播放 reminder.mp3 并弹窗） */
    public static final int TYPE_SERVICE_REMIND = 2;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 新订单待接单提醒
     */
    public void newOrder(Long orderId, String content) {
        push(TYPE_NEW_ORDER, orderId, content);
    }

    /**
     * 服务前提醒
     */
    public void serviceRemind(Long orderId, String content) {
        push(TYPE_SERVICE_REMIND, orderId, content);
    }

    private void push(int type, Long orderId, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("orderId", orderId);
        message.put("content", content);
        try {
            webSocketServer.sendToAllClient(JSON.toJSONString(message));
            log.info("管理端实时提醒已推送：type={}, orderId={}", type, orderId);
        } catch (Exception e) {
            // 推送失败不能影响主流程：管理端没开页面、连接已断开、
            // 或者某个会话写失败，都不该让订单支付/派单跟着失败
            log.warn("管理端实时提醒推送失败（不影响业务）：type={}, orderId={}", type, orderId, e);
        }
    }
}
