package com.vision.task.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vision.common.response.PageResult;
import com.vision.common.response.R;
import com.vision.task.dto.MonitorTaskCreateDTO;
import com.vision.task.dto.MonitorTaskVO;
import com.vision.task.service.MonitorTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 监测任务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/monitor-tasks")
@RequiredArgsConstructor
public class MonitorTaskController {

    private final MonitorTaskService monitorTaskService;

    /**
     * 分页查询监测任务
     */
    @GetMapping
    public R<PageResult<MonitorTaskVO>> pageTasks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessLine,
            @RequestParam(required = false) String modelId) {

        IPage<MonitorTaskVO> result = monitorTaskService.pageTasks(page, size, status, businessLine, modelId);
        return R.ok(new PageResult<>(result.getRecords(), result.getTotal()));
    }

    /**
     * 查询任务详情
     */
    @GetMapping("/{id}")
    public R<MonitorTaskVO> getTask(@PathVariable String id) {
        MonitorTaskVO vo = monitorTaskService.getTaskById(id);
        return R.ok(vo);
    }

    /**
     * 创建监测任务
     */
    @PostMapping
    public R<MonitorTaskVO> createTask(@Valid @RequestBody MonitorTaskCreateDTO dto) {
        log.info("创建监测任务: name={}", dto.getName());
        MonitorTaskVO vo = monitorTaskService.createTask(dto);
        return R.ok(vo);
    }

    /**
     * 更新监测任务
     */
    @PutMapping("/{id}")
    public R<MonitorTaskVO> updateTask(
            @PathVariable String id,
            @Valid @RequestBody MonitorTaskCreateDTO dto) {

        log.info("更新监测任务: id={}", id);
        MonitorTaskVO vo = monitorTaskService.updateTask(id, dto);
        return R.ok(vo);
    }

    /**
     * 删除监测任务
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteTask(@PathVariable String id) {
        log.info("删除监测任务: id={}", id);
        monitorTaskService.deleteTask(id);
        return R.ok();
    }

    /**
     * 启动任务
     */
    @PostMapping("/{id}/start")
    public R<Void> startTask(@PathVariable String id) {
        log.info("启动监测任务: id={}", id);
        monitorTaskService.startTask(id);
        return R.ok();
    }

    /**
     * 停止任务
     */
    @PostMapping("/{id}/stop")
    public R<Void> stopTask(@PathVariable String id) {
        log.info("停止监测任务: id={}", id);
        monitorTaskService.stopTask(id);
        return R.ok();
    }
}
