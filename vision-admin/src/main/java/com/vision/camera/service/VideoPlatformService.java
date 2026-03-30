package com.vision.camera.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vision.camera.dto.PlatformImportDTO;
import com.vision.camera.entity.Camera;
import com.vision.camera.entity.VideoPlatform;
import com.vision.camera.mapper.CameraMapper;
import com.vision.camera.mapper.VideoPlatformMapper;
import com.vision.common.exception.BizException;
import com.vision.common.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoPlatformService extends ServiceImpl<VideoPlatformMapper, VideoPlatform> {

    private final VideoPlatformMapper videoPlatformMapper;
    private final CameraMapper cameraMapper;
    private final RestTemplate restTemplate = new RestTemplate();

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
        log.info("测试视频平台连接: id={}, apiBase={}", id, platform.getApiBase());
        try {
            // 解析凭证 username:password
            String[] parts = platform.getCredential().split(":", 2);
            if (parts.length != 2) {
                return Map.of("connected", false, "message", "凭证格式错误，需为 用户名:密码");
            }
            loginToPlatform(platform.getApiBase(), parts[0], parts[1]);
            platform.setStatus("connected");
            videoPlatformMapper.updateById(platform);
            return Map.of("connected", true, "message", "连接成功");
        } catch (Exception e) {
            platform.setStatus("disconnected");
            videoPlatformMapper.updateById(platform);
            return Map.of("connected", false, "message", "连接失败: " + e.getMessage());
        }
    }

    public Map<String, Object> syncPlatform(String id) {
        VideoPlatform platform = getById(id);
        log.info("同步视频平台: id={}, name={}", id, platform.getName());
        String[] parts = platform.getCredential().split(":", 2);
        if (parts.length != 2) {
            throw new BizException("平台凭证格式错误");
        }
        PlatformImportDTO dto = new PlatformImportDTO();
        dto.setApiBase(platform.getApiBase());
        dto.setUsername(parts[0]);
        dto.setPassword(parts[1]);
        return batchImportFromPlatform(dto);
    }

    /**
     * 从外部视频平台批量导入摄像头
     * 流程：登录 → 分页拉取设备列表 → 获取播放地址 → 按 ID 去重写入
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImportFromPlatform(PlatformImportDTO dto) {
        String apiBase = dto.getApiBase().replaceAll("/+$", "");
        log.info("开始批量导入: apiBase={}, username={}", apiBase, dto.getUsername());

        // 1. 登录获取 token
        String accessToken = loginToPlatform(apiBase, dto.getUsername(), dto.getPassword());

        // 2. 分页拉取全部设备
        List<Map<String, Object>> allDevices = fetchAllDevices(apiBase, accessToken);
        log.info("获取到设备总数: {}", allDevices.size());

        // 3. 逐个获取播放地址并写入摄像头表
        int added = 0, updated = 0, failed = 0;
        for (Map<String, Object> device : allDevices) {
            try {
                String deviceId = (String) device.get("id");
                String cnName = (String) device.get("cnName");
                String channelId = (String) device.get("channelId");
                String label = (String) device.get("labelDesc");
                Object onlineObj = device.get("online");
                boolean online = onlineObj != null && (Integer.valueOf(1).equals(onlineObj) || "1".equals(onlineObj.toString()));

                // 获取播放地址
                String streamUrl = null;
                try {
                    streamUrl = fetchDeviceUrl(apiBase, accessToken, deviceId);
                } catch (Exception e) {
                    log.warn("获取设备播放地址失败: id={}, error={}", deviceId, e.getMessage());
                }

                // 按 ID 去重：存在则更新，不存在则新增
                Camera existing = cameraMapper.selectById(deviceId);
                if (existing != null) {
                    existing.setName(cnName);
                    existing.setChannelNo(channelId);
                    existing.setLabel(label);
                    if (streamUrl != null) {
                        existing.setStreamUrl(streamUrl);
                    }
                    existing.setStatus(online ? "online" : "offline");
                    existing.setSource("synced");
                    cameraMapper.updateById(existing);
                    updated++;
                } else {
                    Camera camera = new Camera();
                    camera.setId(deviceId);
                    camera.setName(cnName);
                    camera.setChannelNo(channelId);
                    camera.setLabel(label);
                    camera.setStreamUrl(streamUrl);
                    camera.setStatus(online ? "online" : "offline");
                    camera.setSource("synced");
                    camera.setBusinessLine("未分类");
                    camera.setAiEnabled(false);
                    camera.setCaptureFrequency("5min");
                    cameraMapper.insert(camera);
                    added++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("导入设备失败: device={}", device.get("id"), e);
            }
        }

        // 4. 创建或更新平台记录
        saveOrUpdatePlatformRecord(dto, allDevices.size(), added, updated, failed);

        log.info("批量导入完成: total={}, added={}, updated={}, failed={}", allDevices.size(), added, updated, failed);
        return Map.of(
                "total", allDevices.size(),
                "added", added,
                "updated", updated,
                "failed", failed,
                "syncTime", LocalDateTime.now().toString()
        );
    }

    // ==================== 外部平台 API 调用 ====================

    /**
     * 登录外部视频平台，返回 accessToken
     */
    private String loginToPlatform(String apiBase, String username, String password) {
        String loginUrl = UriComponentsBuilder.fromHttpUrl(apiBase + "/api/user/login")
                .queryParam("username", username)
                .queryParam("password", password)
                .toUriString();

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    loginUrl, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});
            Map<String, Object> body = resp.getBody();
            if (body == null || !Integer.valueOf(0).equals(body.get("code"))) {
                String msg = body != null ? (String) body.get("msg") : "未知错误";
                throw new BizException("视频平台登录失败: " + msg);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            String token = (String) data.get("accessToken");
            if (token == null || token.isBlank()) {
                throw new BizException("视频平台登录失败: 未返回 accessToken");
            }
            return token;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("视频平台连接失败: " + e.getMessage());
        }
    }

    /**
     * 分页拉取全部设备列表
     */
    private List<Map<String, Object>> fetchAllDevices(String apiBase, String accessToken) {
        List<Map<String, Object>> allDevices = new ArrayList<>();
        int page = 1;
        int pageSize = 100;
        int total;

        HttpHeaders headers = new HttpHeaders();
        headers.set("access-token", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        do {
            String url = UriComponentsBuilder
                    .fromHttpUrl(apiBase + "/secureCenter/hkCenter/getDeviceInfo")
                    .queryParam("type", "all")
                    .queryParam("online", "")
                    .queryParam("page", page)
                    .queryParam("count", pageSize)
                    .queryParam("keyWord", "")
                    .queryParam("label", "")
                    .toUriString();

            try {
                ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                        url, HttpMethod.GET, entity,
                        new ParameterizedTypeReference<>() {});
                Map<String, Object> body = resp.getBody();
                if (body == null || !Integer.valueOf(0).equals(body.get("code"))) {
                    throw new BizException("拉取设备列表失败: " + (body != null ? body.get("msg") : ""));
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                total = ((Number) data.get("total")).intValue();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> devices = (List<Map<String, Object>>) data.get("data");
                if (devices != null) {
                    allDevices.addAll(devices);
                }
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                throw new BizException("拉取设备列表失败: " + e.getMessage());
            }
            page++;
        } while (allDevices.size() < total);

        return allDevices;
    }

    /**
     * 获取单个设备的播放地址
     */
    private String fetchDeviceUrl(String apiBase, String accessToken, String deviceId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(apiBase + "/secureCenter/hkCenter/getUrl")
                .queryParam("id", deviceId)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("access-token", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<>() {});
        Map<String, Object> body = resp.getBody();
        if (body == null || !Integer.valueOf(0).equals(body.get("code"))) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return data != null ? (String) data.get("url") : null;
    }

    /**
     * 创建或更新平台记录（按 apiBase 去重）
     */
    private void saveOrUpdatePlatformRecord(PlatformImportDTO dto, int total, int added, int updated, int failed) {
        VideoPlatform platform = videoPlatformMapper.selectList(null).stream()
                .filter(p -> dto.getApiBase().equals(p.getApiBase()))
                .findFirst()
                .orElse(null);

        String syncResultJson = String.format(
                "{\"total\":%d,\"added\":%d,\"updated\":%d,\"failed\":%d,\"syncTime\":\"%s\"}",
                total, added, updated, failed, LocalDateTime.now());

        if (platform == null) {
            platform = new VideoPlatform();
            platform.setId(IdUtil.uuid());
            platform.setName("视频平台 - " + dto.getApiBase());
            platform.setApiBase(dto.getApiBase());
            platform.setAuthType("basic");
            platform.setCredential(dto.getUsername() + ":" + dto.getPassword());
            platform.setAutoSync(false);
            platform.setSyncIntervalMin(60);
            platform.setStatus("connected");
            platform.setCamerasCount(total - failed);
            platform.setLastSyncTime(LocalDateTime.now());
            platform.setLastSyncResult(syncResultJson);
            videoPlatformMapper.insert(platform);
        } else {
            platform.setCredential(dto.getUsername() + ":" + dto.getPassword());
            platform.setStatus("connected");
            platform.setCamerasCount(total - failed);
            platform.setLastSyncTime(LocalDateTime.now());
            platform.setLastSyncResult(syncResultJson);
            videoPlatformMapper.updateById(platform);
        }
    }
}
