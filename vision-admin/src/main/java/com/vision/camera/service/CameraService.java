package com.vision.camera.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vision.camera.dto.CameraCreateDTO;
import com.vision.camera.dto.CameraGroupVO;
import com.vision.camera.dto.CameraVO;
import com.vision.camera.entity.Camera;
import com.vision.camera.entity.CameraGroup;
import com.vision.camera.mapper.CameraGroupMapper;
import com.vision.camera.mapper.CameraMapper;
import com.vision.common.exception.BizException;
import com.vision.common.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 摄像头服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CameraService extends ServiceImpl<CameraMapper, Camera> {

    private final CameraMapper cameraMapper;
    private final CameraGroupMapper cameraGroupMapper;

    /**
     * 分页查询摄像头列表
     */
    public IPage<CameraVO> pageCameras(Integer page, Integer size, String groupId, String status, String keyword) {
        Page<Camera> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Camera> wrapper = new LambdaQueryWrapper<>();

        if (groupId != null && !groupId.isEmpty()) {
            wrapper.eq(Camera::getGroupId, groupId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Camera::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Camera::getName, keyword).or().like(Camera::getLabel, keyword));
        }

        wrapper.orderByDesc(Camera::getCreatedAt);

        IPage<Camera> cameraPage = cameraMapper.selectPage(pageParam, wrapper);

        // 转换为VO并填充分组名称
        List<CameraVO> voList = cameraPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<CameraVO> resultPage = new Page<>(cameraPage.getCurrent(), cameraPage.getSize(), cameraPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 根据ID查询摄像头详情
     */
    public CameraVO getCameraById(String id) {
        Camera camera = cameraMapper.selectById(id);
        if (camera == null) {
            throw new BizException("摄像头不存在");
        }
        return convertToVO(camera);
    }

    /**
     * 创建摄像头
     */
    @Transactional(rollbackFor = Exception.class)
    public CameraVO createCamera(CameraCreateDTO dto) {
        // 验证分组是否存在
        if (dto.getGroupId() != null) {
            CameraGroup group = cameraGroupMapper.selectById(dto.getGroupId());
            if (group == null) {
                throw new BizException("摄像头分组不存在");
            }
        }

        Camera camera = new Camera();
        BeanUtils.copyProperties(dto, camera);
        camera.setId(IdUtil.uuid());
        camera.setStatus("offline");
        camera.setSource("manual");
        camera.setAiEnabled(dto.getAiEnabled() != null ? dto.getAiEnabled() : false);
        camera.setCaptureFrequency(dto.getCaptureFrequency() != null ? dto.getCaptureFrequency() : "5min");

        cameraMapper.insert(camera);
        log.info("创建摄像头成功: id={}, name={}", camera.getId(), camera.getName());
        return convertToVO(camera);
    }

    /**
     * 更新摄像头
     */
    @Transactional(rollbackFor = Exception.class)
    public CameraVO updateCamera(String id, CameraCreateDTO dto) {
        Camera camera = cameraMapper.selectById(id);
        if (camera == null) {
            throw new BizException("摄像头不存在");
        }

        // 验证分组是否存在
        if (dto.getGroupId() != null) {
            CameraGroup group = cameraGroupMapper.selectById(dto.getGroupId());
            if (group == null) {
                throw new BizException("摄像头分组不存在");
            }
        }

        BeanUtils.copyProperties(dto, camera);
        cameraMapper.updateById(camera);
        log.info("更新摄像头成功: id={}", id);
        return convertToVO(camera);
    }

    /**
     * 删除摄像头
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCamera(String id) {
        Camera camera = cameraMapper.selectById(id);
        if (camera == null) {
            throw new BizException("摄像头不存在");
        }
        cameraMapper.deleteById(id);
        log.info("删除摄像头成功: id={}", id);
    }

    /**
     * 获取分组树
     */
    public List<CameraGroupVO> getGroupTree() {
        List<CameraGroup> allGroups = cameraGroupMapper.selectList(null);
        List<CameraGroupVO> voList = allGroups.stream()
                .map(CameraGroupVO::fromEntity)
                .collect(Collectors.toList());

        return buildGroupTree(voList, null);
    }

    /**
     * 创建分组
     */
    public CameraGroupVO createGroup(CameraGroup group) {
        if (group.getName() == null || group.getName().isBlank()) {
            throw new BizException("分组名称不能为空");
        }
        group.setId(IdUtil.uuid());
        if (group.getSortOrder() == null) {
            group.setSortOrder(0);
        }
        cameraGroupMapper.insert(group);
        log.info("创建分组成功: id={}, name={}", group.getId(), group.getName());
        return CameraGroupVO.fromEntity(group);
    }

    /**
     * 删除分组
     */
    public void deleteGroup(String id) {
        CameraGroup group = cameraGroupMapper.selectById(id);
        if (group == null) {
            throw new BizException("分组不存在");
        }
        cameraGroupMapper.deleteById(id);
        log.info("删除分组成功: id={}", id);
    }

    /**
     * 递归构建分组树
     */
    private List<CameraGroupVO> buildGroupTree(List<CameraGroupVO> allGroups, String parentId) {
        List<CameraGroupVO> result = new ArrayList<>();

        for (CameraGroupVO group : allGroups) {
            if ((parentId == null && group.getParentId() == null) ||
                (parentId != null && parentId.equals(group.getParentId()))) {
                // 递归查找子分组
                List<CameraGroupVO> children = buildGroupTree(allGroups, group.getId());
                group.setChildren(children);
                result.add(group);
            }
        }

        return result;
    }

    /**
     * 批量导入摄像头
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importCameras(MultipartFile file) throws IOException {
        log.info("开始批量导入摄像头: fileName={}", file.getOriginalFilename());
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 3) {
                    failCount++;
                    errors.add("行格式错误: " + line);
                    continue;
                }

                try {
                    CameraCreateDTO dto = new CameraCreateDTO();
                    dto.setName(parts[0].trim());
                    dto.setBusinessLine(parts[1].trim());
                    dto.setLocation(parts.length > 2 ? parts[2].trim() : "");
                    dto.setStreamUrl(parts.length > 3 ? parts[3].trim() : "");
                    dto.setCaptureFrequency(parts.length > 4 ? parts[4].trim() : "5min");
                    dto.setAiEnabled(parts.length > 5 ? Boolean.parseBoolean(parts[5].trim()) : false);

                    createCamera(dto);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("导入失败: " + line + ", 错误: " + e.getMessage());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        log.info("摄像头导入完成: success={}, fail={}", successCount, failCount);
        return result;
    }

    /**
     * 更新摄像头状态
     */
    public void updateCameraStatus(String id, String status) {
        Camera camera = cameraMapper.selectById(id);
        if (camera != null) {
            camera.setStatus(status);
            cameraMapper.updateById(camera);
            log.debug("更新摄像头状态: id={}, status={}", id, status);
        }
    }

    /**
     * 更新最后抓图时间
     */
    public void updateLastCaptureTime(String id) {
        Camera camera = cameraMapper.selectById(id);
        if (camera != null) {
            camera.setLastCaptureTime(LocalDateTime.now());
            cameraMapper.updateById(camera);
        }
    }

    /**
     * 转换为VO并填充分组名称
     */
    private CameraVO convertToVO(Camera camera) {
        CameraVO vo = CameraVO.fromEntity(camera);

        if (camera.getGroupId() != null) {
            CameraGroup group = cameraGroupMapper.selectById(camera.getGroupId());
            if (group != null) {
                vo.setGroupName(group.getName());
            }
        }

        return vo;
    }
}
