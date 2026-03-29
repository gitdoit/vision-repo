package com.vision.alert.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vision.alert.dto.AlertQueryDTO;
import com.vision.alert.dto.AlertVO;
import com.vision.alert.service.AlertService;
import com.vision.common.response.PageResult;
import com.vision.common.response.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * 分页查询告警列表
     */
    @GetMapping
    public R<PageResult<AlertVO>> pageAlerts(AlertQueryDTO dto) {
        log.info("分页查询告警列表: page={}, size={}", dto.getPage(), dto.getSize());
        IPage<AlertVO> result = alertService.pageAlerts(dto);
        return R.ok(new PageResult<>(result.getRecords(), result.getTotal()));
    }

    /**
     * 查询告警详情
     */
    @GetMapping("/{id}")
    public R<AlertVO> getAlert(@PathVariable String id) {
        log.info("查询告警详情: id={}", id);
        AlertVO vo = alertService.getAlertById(id);
        if (vo == null) {
            log.warn("告警不存在: id={}", id);
            return R.fail(404, "告警不存在");
        }
        return R.ok(vo);
    }

    /**
     * 标记已读
     */
    @PostMapping("/{id}/read")
    public R<Void> markAsRead(@PathVariable String id) {
        log.info("标记告警已读: id={}", id);
        alertService.markAsRead(id);
        return R.ok();
    }

    /**
     * 批量标记已读
     */
    @PostMapping("/batch-read")
    public R<Void> markBatchAsRead(@RequestBody List<String> ids) {
        log.info("批量标记告警已读: count={}", ids.size());
        alertService.markBatchAsRead(ids);
        return R.ok();
    }

    /**
     * 全部标记已读
     */
    @PostMapping("/all-read")
    public R<Void> markAllAsRead() {
        log.info("全部标记告警已读");
        alertService.markAllAsRead();
        return R.ok();
    }

    /**
     * 获取未读告警数量
     */
    @GetMapping("/unread-count")
    public R<Long> getUnreadCount() {
        Long count = alertService.getUnreadCount();
        return R.ok(count);
    }

    /**
     * 获取最新告警列表
     */
    @GetMapping("/latest")
    public R<List<AlertVO>> getLatestAlerts(@RequestParam(defaultValue = "10") Integer limit) {
        List<AlertVO> alerts = alertService.getLatestAlerts(limit);
        return R.ok(alerts);
    }
}
