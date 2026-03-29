package com.vision.camera.controller;

import com.vision.camera.entity.VideoPlatform;
import com.vision.camera.service.VideoPlatformService;
import com.vision.common.response.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 视频平台管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/video-platforms")
@RequiredArgsConstructor
public class VideoPlatformController {

    private final VideoPlatformService videoPlatformService;

    @GetMapping
    public R<List<VideoPlatform>> list() {
        return R.ok(videoPlatformService.listAll());
    }

    @GetMapping("/{id}")
    public R<VideoPlatform> get(@PathVariable String id) {
        return R.ok(videoPlatformService.getById(id));
    }

    @PostMapping
    public R<VideoPlatform> create(@RequestBody VideoPlatform platform) {
        log.info("创建视频平台: name={}", platform.getName());
        return R.ok(videoPlatformService.createPlatform(platform));
    }

    @PutMapping("/{id}")
    public R<VideoPlatform> update(@PathVariable String id, @RequestBody VideoPlatform platform) {
        log.info("更新视频平台: id={}", id);
        return R.ok(videoPlatformService.updatePlatform(id, platform));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        log.info("删除视频平台: id={}", id);
        videoPlatformService.deletePlatform(id);
        return R.ok();
    }

    @PostMapping("/{id}/test")
    public R<Map<String, Object>> testConnection(@PathVariable String id) {
        log.info("测试视频平台连接: id={}", id);
        return R.ok(videoPlatformService.testConnection(id));
    }

    @PostMapping("/{id}/sync")
    public R<Map<String, Object>> sync(@PathVariable String id) {
        log.info("同步视频平台: id={}", id);
        return R.ok(videoPlatformService.syncPlatform(id));
    }
}
