package com.vision.camera.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vision.camera.entity.VideoPlatform;
import com.vision.camera.mapper.VideoPlatformMapper;
import com.vision.common.exception.BizException;
import com.vision.common.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoPlatformService extends ServiceImpl<VideoPlatformMapper, VideoPlatform> {

    private final VideoPlatformMapper videoPlatformMapper;

    public List<VideoPlatform> listAll() {
        return videoPlatformMapper.selectList(null);
    }

    public VideoPlatform getById(String id) {
        VideoPlatform platform = videoPlatformMapper.selectById(id);
        if (platform == null) {
            throw new BizException("视频平台不存在");
        }
        return platform;
    }

    public VideoPlatform createPlatform(VideoPlatform platform) {
        platform.setId(IdUtil.uuid());
        platform.setStatus("disconnected");
        platform.setCamerasCount(0);
        videoPlatformMapper.insert(platform);
        log.info("创建视频平台: id={}, name={}", platform.getId(), platform.getName());
        return platform;
    }

    public VideoPlatform updatePlatform(String id, VideoPlatform dto) {
        VideoPlatform platform = getById(id);
        if (dto.getName() != null) platform.setName(dto.getName());
        if (dto.getApiBase() != null) platform.setApiBase(dto.getApiBase());
        if (dto.getAuthType() != null) platform.setAuthType(dto.getAuthType());
        if (dto.getCredential() != null) platform.setCredential(dto.getCredential());
        if (dto.getAutoSync() != null) platform.setAutoSync(dto.getAutoSync());
        if (dto.getSyncIntervalMin() != null) platform.setSyncIntervalMin(dto.getSyncIntervalMin());
        videoPlatformMapper.updateById(platform);
        log.info("更新视频平台: id={}", id);
        return platform;
    }

    public void deletePlatform(String id) {
        getById(id);
        videoPlatformMapper.deleteById(id);
        log.info("删除视频平台: id={}", id);
    }

    public Map<String, Object> testConnection(String id) {
        VideoPlatform platform = getById(id);
        // Placeholder: real implementation would attempt HTTP connection to platform.apiBase
        log.info("测试视频平台连接: id={}, apiBase={}", id, platform.getApiBase());
        return Map.of("connected", false, "message", "连接测试功能待实现");
    }

    public Map<String, Object> syncPlatform(String id) {
        VideoPlatform platform = getById(id);
        // Placeholder: real implementation would call platform API and sync cameras
        log.info("同步视频平台: id={}, name={}", id, platform.getName());
        return Map.of("total", 0, "added", 0, "updated", 0, "removed", 0, "failed", 0);
    }
}
