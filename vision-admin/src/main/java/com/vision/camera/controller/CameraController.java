package com.vision.camera.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vision.camera.dto.CameraCreateDTO;
import com.vision.camera.dto.CameraGroupVO;
import com.vision.camera.dto.CameraVO;
import com.vision.camera.service.CameraService;
import com.vision.common.response.PageResult;
import com.vision.common.response.R;
import com.vision.common.util.IdUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 摄像头管理控制器
 */
@RestController
@RequestMapping("/api/v1/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final CameraService cameraService;

    /**
     * 分页查询摄像头列表
     */
    @GetMapping
    public R<PageResult<CameraVO>> pageCameras(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String status) {

        IPage<CameraVO> result = cameraService.pageCameras(page, size, groupId, status);
        return R.ok(new PageResult<>(result.getRecords(), result.getTotal()));
    }

    /**
     * 查询摄像头详情
     */
    @GetMapping("/{id}")
    public R<CameraVO> getCamera(@PathVariable String id) {
        CameraVO vo = cameraService.getCameraById(id);
        return R.ok(vo);
    }

    /**
     * 创建摄像头
     */
    @PostMapping
    public R<CameraVO> createCamera(@Valid @RequestBody CameraCreateDTO dto) {
        CameraVO vo = cameraService.createCamera(dto);
        return R.ok(vo);
    }

    /**
     * 更新摄像头
     */
    @PutMapping("/{id}")
    public R<CameraVO> updateCamera(
            @PathVariable String id,
            @Valid @RequestBody CameraCreateDTO dto) {

        CameraVO vo = cameraService.updateCamera(id, dto);
        return R.ok(vo);
    }

    /**
     * 删除摄像头
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteCamera(@PathVariable String id) {
        cameraService.deleteCamera(id);
        return R.ok();
    }

    /**
     * 获取摄像头分组树
     */
    @GetMapping("/groups")
    public R<List<CameraGroupVO>> getGroupTree() {
        List<CameraGroupVO> tree = cameraService.getGroupTree();
        return R.ok(tree);
    }

    /**
     * 批量导入摄像头
     */
    @PostMapping("/import")
    public R<Map<String, Object>> importCameras(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = cameraService.importCameras(file);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail(500, "导入失败: " + e.getMessage());
        }
    }
}
