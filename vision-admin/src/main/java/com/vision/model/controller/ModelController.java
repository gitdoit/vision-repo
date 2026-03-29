package com.vision.model.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vision.model.dto.ModelConfigDTO;
import com.vision.model.dto.ModelCreateDTO;
import com.vision.model.dto.ModelVO;
import com.vision.model.entity.ModelVersion;
import com.vision.model.service.ModelService;
import com.vision.common.response.PageResult;
import com.vision.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    /**
     * 分页查询模型列表
     */
    @GetMapping
    public R<PageResult<ModelVO>> pageModels(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status) {

        IPage<ModelVO> result = modelService.pageModels(page, size, status);
        return R.ok(new PageResult<>(result.getRecords(), result.getTotal()));
    }

    /**
     * 查询模型详情
     */
    @GetMapping("/{id}")
    public R<ModelVO> getModel(@PathVariable String id) {
        ModelVO vo = modelService.getModelById(id);
        return R.ok(vo);
    }

    /**
     * 创建模型
     */
    @PostMapping
    public R<ModelVO> createModel(@Valid @RequestBody ModelCreateDTO dto) {
        log.info("创建模型: name={}, version={}", dto.getName(), dto.getVersion());
        ModelVO vo = modelService.createModel(dto);
        return R.ok(vo);
    }

    /**
     * 更新模型
     */
    @PutMapping("/{id}")
    public R<ModelVO> updateModel(
            @PathVariable String id,
            @Valid @RequestBody ModelCreateDTO dto) {

        log.info("更新模型: id={}", id);
        ModelVO vo = modelService.updateModel(id, dto);
        return R.ok(vo);
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteModel(@PathVariable String id) {
        log.info("删除模型: id={}", id);
        modelService.deleteModel(id);
        return R.ok();
    }

    /**
     * 加载模型
     */
    @PostMapping("/{id}/load")
    public R<Void> loadModel(@PathVariable String id) {
        log.info("加载模型: id={}", id);
        modelService.loadModel(id);
        return R.ok();
    }

    /**
     * 卸载模型
     */
    @PostMapping("/{id}/unload")
    public R<Void> unloadModel(@PathVariable String id) {
        log.info("卸载模型: id={}", id);
        modelService.unloadModel(id);
        return R.ok();
    }

    /**
     * 更新模型配置
     */
    @PutMapping("/{id}/config")
    public R<Void> updateModelConfig(
            @PathVariable String id,
            @Valid @RequestBody ModelConfigDTO dto) {

        log.info("更新模型配置: id={}", id);
        modelService.updateModelConfig(id, dto);
        return R.ok();
    }

    /**
     * 获取模型版本历史
     */
    @GetMapping("/{id}/versions")
    public R<List<ModelVersion>> getModelVersions(@PathVariable String id) {
        List<ModelVersion> versions = modelService.getModelVersions(id);
        return R.ok(versions);
    }
}
