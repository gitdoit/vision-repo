package com.vision.alert.service;

import com.vision.alert.dto.AlertVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 告警推送服务
 * 通过WebSocket实时推送告警到前端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertPushService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送告警到前端
     */
    public void pushAlert(AlertVO alert) {
        try {
            messagingTemplate.convertAndSend("/topic/alerts", alert);
            log.debug("推送告警成功: {}", alert.getId());
        } catch (Exception e) {
            log.error("推送告警失败: {}", alert.getId(), e);
        }
    }

    /**
     * 推送未读告警数量
     */
    public void pushUnreadCount(Long count) {
        try {
            messagingTemplate.convertAndSend("/topic/alerts/unread-count", count);
            log.debug("推送未读告警数量: {}", count);
        } catch (Exception e) {
            log.error("推送未读告警数量失败", e);
        }
    }
}
