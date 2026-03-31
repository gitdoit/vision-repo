package com.vision.camera.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vision.camera.dto.CameraCreateDTO;
import com.vision.camera.dto.CameraGroupVO;
import com.vision.camera.dto.CameraVO;
import com.vision.camera.entity.CameraGroup;
import com.vision.camera.service.CameraService;
import com.vision.common.response.PageResult;
import com.vision.common.response.R;
import com.vision.common.util.IdUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 摄像头管理控制器
 */
@Slf4j
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
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        IPage<CameraVO> result = cameraService.pageCameras(page, size, groupId, status, keyword);
        return R.ok(new PageResult<>(result.getRecords(), result.getTotal()));
    }

    /**
     * 查询摄像头详情
     */
    @GetMapping("/{id}")
    public R<CameraVO> getCamera(@PathVariable String id) {
        log.info("查询摄像头详情: id={}", id);
        CameraVO vo = cameraService.getCameraById(id);
        return R.ok(vo);
    }

    /**
     * 创建摄像头
     */
    @PostMapping
    public R<CameraVO> createCamera(@Valid @RequestBody CameraCreateDTO dto) {
        log.info("创建摄像头: name={}", dto.getName());
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

        log.info("更新摄像头: id={}", id);
        CameraVO vo = cameraService.updateCamera(id, dto);
        return R.ok(vo);
    }

    /**
     * 删除摄像头
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteCamera(@PathVariable String id) {
        log.info("删除摄像头: id={}", id);
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
     * 创建摄像头分组
     */
    @PostMapping("/groups")
    public R<CameraGroupVO> createGroup(@RequestBody CameraGroup group) {
        log.info("创建摄像头分组: name={}", group.getName());
        CameraGroupVO vo = cameraService.createGroup(group);
        return R.ok(vo);
    }

    /**
     * 更新摄像头分组
     */
    @PutMapping("/groups/{id}")
    public R<CameraGroupVO> updateGroup(@PathVariable String id, @RequestBody CameraGroup group) {
        log.info("更新摄像头分组: id={}, name={}", id, group.getName());
        CameraGroupVO vo = cameraService.updateGroup(id, group);
        return R.ok(vo);
    }

    /**
     * 删除摄像头分组
     */
    @DeleteMapping("/groups/{id}")
    public R<Void> deleteGroup(@PathVariable String id) {
        log.info("删除摄像头分组: id={}", id);
        cameraService.deleteGroup(id);
        return R.ok();
    }

    /**
     * 更新摄像头所属分组
     */
    @PutMapping("/{id}/groups")
    public R<Void> updateCameraGroups(
            @PathVariable String id,
            @RequestBody Map<String, List<String>> body) {
        List<String> groupIds = body.getOrDefault("groupIds", List.of());
        log.info("更新摄像头分组: cameraId={}, groupIds={}", id, groupIds);
        cameraService.updateCameraGroups(id, groupIds);
        return R.ok();
    }

    /**
     * 批量导入摄像头
     */
    @PostMapping("/import")
    public R<Map<String, Object>> importCameras(@RequestParam("file") MultipartFile file) {
        log.info("批量导入摄像头: fileName={}", file.getOriginalFilename());
        try {
            Map<String, Object> result = cameraService.importCameras(file);
            log.info("摄像头导入完成: successCount={}, failCount={}", result.get("successCount"), result.get("failCount"));
            return R.ok(result);
        } catch (Exception e) {
            log.error("摄像头导入失败", e);
            return R.fail(500, "导入失败: " + e.getMessage());
        }
    }
}
