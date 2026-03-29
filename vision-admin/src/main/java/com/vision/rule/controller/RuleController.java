package com.vision.rule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vision.rule.dto.RuleCreateDTO;
import com.vision.rule.dto.RuleVO;
import com.vision.rule.service.RuleService;
import com.vision.common.response.PageResult;
import com.vision.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.List;
import java.util.Map;

/**
 * 规则管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    /**
     * 分页查询规则列表
     */
    @GetMapping
    public R<PageResult<RuleVO>> pageRules(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String businessLine,
            @RequestParam(required = false) Boolean enabled) {

        IPage<RuleVO> result = ruleService.pageRules(page, size, businessLine, enabled);
        return R.ok(new PageResult<>(result.getRecords(), result.getTotal()));
    }

    /**
     * 查询规则详情
     */
    @GetMapping("/{id}")
    public R<RuleVO> getRule(@PathVariable String id) {
        RuleVO vo = ruleService.getRuleById(id);
        return R.ok(vo);
    }

    /**
     * 创建规则
     */
    @PostMapping
    public R<RuleVO> createRule(@Valid @RequestBody RuleCreateDTO dto) {
        log.info("创建规则: name={}", dto.getName());
        RuleVO vo = ruleService.createRule(dto);
        return R.ok(vo);
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    public R<RuleVO> updateRule(
            @PathVariable String id,
            @Valid @RequestBody RuleCreateDTO dto) {

        log.info("更新规则: id={}", id);
        RuleVO vo = ruleService.updateRule(id, dto);
        return R.ok(vo);
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteRule(@PathVariable String id) {
        log.info("删除规则: id={}", id);
        ruleService.deleteRule(id);
        return R.ok();
    }

    /**
     * 部署规则
     */
    @PostMapping("/{id}/deploy")
    public R<Void> deployRule(@PathVariable String id) {
        log.info("部署规则: id={}", id);
        ruleService.deployRule(id);
        return R.ok();
    }

    /**
     * 测试规则
     */
    @PostMapping("/{id}/test")
    public R<Map<String, Object>> testRule(
            @PathVariable String id,
            @RequestBody Map<String, Object> payload) {

        log.info("测试规则: id={}", id);
        // 这里简化处理，实际需要解析payload中的detections
        boolean result = ruleService.testRule(id, List.of());
        return R.ok(Map.of("matched", result));
    }
}
