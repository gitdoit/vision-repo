package com.vision.inference.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vision.camera.entity.Camera;
import com.vision.camera.mapper.CameraMapper;
import com.vision.camera.mapper.CameraGroupMappingMapper;
import com.vision.camera.mapper.CameraGroupMapper;
import com.vision.camera.entity.CameraGroup;
import com.vision.camera.entity.CameraGroupMapping;
import com.vision.inference.dto.DetectionVO;
import com.vision.inference.dto.InferenceQueryDTO;
import com.vision.inference.dto.InferenceRecordVO;
import com.vision.inference.entity.Detection;
import com.vision.inference.entity.InferenceRecord;
import com.vision.inference.mapper.DetectionMapper;
import com.vision.inference.mapper.InferenceMapper;
import com.vision.common.exception.BizException;
import com.vision.common.util.IdUtil;
import com.vision.model.entity.Model;
import com.vision.model.entity.ModelNodeDeployment;
import com.vision.model.mapper.ModelMapper;
import com.vision.model.service.InferenceClient;
import com.vision.model.service.ModelService;
import com.vision.storage.StorageService;
import com.vision.task.entity.MonitorTask;
import com.vision.task.mapper.MonitorTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 推理记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InferenceService extends ServiceImpl<InferenceMapper, InferenceRecord> {

    private final InferenceMapper inferenceMapper;
    private final DetectionMapper detectionMapper;
    private final CameraMapper cameraMapper;
    private final CameraGroupMappingMapper cameraGroupMappingMapper;
    private final CameraGroupMapper cameraGroupMapper;
    private final ModelMapper modelMapper;
    private final ModelService modelService;
    private final InferenceClient inferenceClient;
    private final StorageService storageService;
    private final MonitorTaskMapper monitorTaskMapper;

    /**
     * 模型测试（单张图片推理）
     */
    public Map<String, Object> testInference(MultipartFile image, String modelId, BigDecimal confidenceThreshold) {
        // 1. 查找模型，获取 taskType
        Model model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new BizException("模型不存在");
        }
        ModelNodeDeployment deployment = modelService.findLoadedDeployment(modelId);
        if (deployment == null) {
            throw new BizException("模型未加载，请先加载模型");
        }

        // 2. 保存上传图片
        String path = "test/" + LocalDate.now() + "/" + UUID.randomUUID() + ".jpg";
        String imageUrl;
        try {
            imageUrl = storageService.upload(image.getInputStream(), path, image.getContentType());
        } catch (IOException e) {
            throw new BizException("图片保存失败: " + e.getMessage());
        }

        // 3. 将 URL 解析为本地文件路径（Python 服务通过本地路径访问）
        String filePath = storageService.resolveToFilePath(imageUrl);

        // 4. 调用 Python 推理服务（路由到模型所在节点）
        String nodeId = deployment.getNodeId();
        String taskType = model.getTaskType() != null ? model.getTaskType() : "detect";
        Map<String, Object> pythonResult = inferenceClient.predict(nodeId, filePath, modelId, confidenceThreshold, taskType);

        // 5. 转换 snake_case → camelCase，匹配前端类型定义
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("taskType", pythonResult.getOrDefault("task_type", taskType));
        result.put("inferenceTimeMs", pythonResult.getOrDefault("inference_time_ms", 0));
        result.put("objects", pythonResult.getOrDefault("objects", List.of()));
        result.put("classifications", pythonResult.get("classifications"));
        return result;
    }

    /**
     * 分页查询推理记录
     */
    public IPage<InferenceRecordVO> pageRecords(InferenceQueryDTO dto) {
        int offset = (dto.getPage() - 1) * dto.getSize();

        List<InferenceRecord> records = inferenceMapper.selectPageByCondition(
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getCameraId(),
                dto.getAlertType(),
                dto.getTaskId(),
                dto.getSize(),
                offset
        );

        Long total = inferenceMapper.countByCondition(
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getCameraId(),
                dto.getAlertType(),
                dto.getTaskId()
        );

        List<InferenceRecordVO> voList = records.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<InferenceRecordVO> resultPage = new Page<>(dto.getPage(), dto.getSize(), total);
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 查询推理详情
     */
    public InferenceRecordVO getRecordById(String id) {
        InferenceRecord record = inferenceMapper.selectById(id);
        if (record == null) {
            throw new BizException("推理记录不存在");
        }
        return convertToVO(record);
    }

    /**
     * 保存推理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public String saveInferenceResult(InferenceRecordVO vo) {
        InferenceRecord record = new InferenceRecord();
        record.setId(IdUtil.uuid());
        record.setEventId(vo.getEventId());
        record.setCameraId(vo.getCameraId());
        record.setBusinessType(vo.getBusinessType());
        record.setAvgConfidence(vo.getAvgConfidence());
        record.setAlertStatus(vo.getAlertStatus());
        record.setThumbnailUrl(vo.getThumbnailUrl());
        record.setOriginalImageUrl(vo.getOriginalImageUrl());
        record.setAnnotatedImageUrl(vo.getAnnotatedImageUrl());
        record.setRawJson(vo.getRawJson());
        record.setModelName(vo.getModelName());
        record.setInferenceTimeMs(vo.getInferenceTimeMs());
        record.setCreatedAt(LocalDateTime.now());

        inferenceMapper.insert(record);

        // 保存检测目标
        if (vo.getDetections() != null && !vo.getDetections().isEmpty()) {
            for (DetectionVO detectionVO : vo.getDetections()) {
                Detection detection = new Detection();
                detection.setId(IdUtil.uuid());
                detection.setRecordId(record.getId());
                detection.setLabel(detectionVO.getLabel());
                detection.setConfidence(detectionVO.getConfidence());
                detection.setBbox(detectionVO.getBbox());
                detection.setCount(detectionVO.getCount());
                detection.setAttributes(detectionVO.getAttributes());
                detectionMapper.insert(detection);
            }
        }

        return record.getId();
    }

    /**
     * 推理结果回调接口
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(String callbackData) {
        // 解析回调数据并保存
        log.info("收到推理结果回调: {}", callbackData);
    }

    /**
     * 导出CSV
     */
    public byte[] exportToCsv(InferenceQueryDTO dto) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,摄像头ID,业务类型,平均置信度,告警状态,推理耗时,创建时间\n");

        IPage<InferenceRecordVO> records = pageRecords(dto);

        for (InferenceRecordVO vo : records.getRecords()) {
            sb.append(vo.getId()).append(",");
            sb.append(vo.getCameraId()).append(",");
            sb.append(vo.getBusinessType()).append(",");
            sb.append(vo.getAvgConfidence()).append(",");
            sb.append(vo.getAlertStatus()).append(",");
            sb.append(vo.getInferenceTimeMs()).append(",");
            sb.append(vo.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        }

        return sb.toString().getBytes("UTF-8");
    }

    /**
     * 导出Excel
     */
    public byte[] exportToExcel(InferenceQueryDTO dto) throws IOException {
        IPage<InferenceRecordVO> records = pageRecords(dto);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("推理记录");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "摄像头ID", "业务类型", "平均置信度", "告警状态", "推理耗时", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 填充数据
            int rowNum = 1;
            for (InferenceRecordVO vo : records.getRecords()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(vo.getId());
                row.createCell(1).setCellValue(vo.getCameraId() != null ? vo.getCameraId() : "");
                row.createCell(2).setCellValue(vo.getBusinessType() != null ? vo.getBusinessType() : "");
                row.createCell(3).setCellValue(vo.getAvgConfidence() != null ? vo.getAvgConfidence().doubleValue() : 0);
                row.createCell(4).setCellValue(vo.getAlertStatus() != null ? vo.getAlertStatus() : "");
                row.createCell(5).setCellValue(vo.getInferenceTimeMs() != null ? vo.getInferenceTimeMs() : 0);
                row.createCell(6).setCellValue(vo.getCreatedAt() != null ?
                    vo.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            }

            // 写入字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 统计时间范围内的推理次数
     */
    public Long countByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return inferenceMapper.countByTimeRange(startTime, endTime);
    }

    /**
     * 转换为VO
     */
    private InferenceRecordVO convertToVO(InferenceRecord record) {
        InferenceRecordVO vo = InferenceRecordVO.fromEntity(record);

        // 加载摄像头名称 + 分组名称
        if (record.getCameraId() != null) {
            Camera camera = cameraMapper.selectById(record.getCameraId());
            if (camera != null) {
                vo.setCameraName(camera.getName());
                // 查询摄像头所属分组名称（取第一个分组）
                LambdaQueryWrapper<CameraGroupMapping> gw = new LambdaQueryWrapper<>();
                gw.eq(CameraGroupMapping::getCameraId, record.getCameraId()).last("LIMIT 1");
                CameraGroupMapping mapping = cameraGroupMappingMapper.selectOne(gw);
                if (mapping != null) {
                    CameraGroup group = cameraGroupMapper.selectById(mapping.getGroupId());
                    if (group != null) {
                        vo.setGroupName(group.getName());
                    }
                }
            }
        }

        // 加载任务名称
        if (record.getTaskId() != null) {
            MonitorTask task = monitorTaskMapper.selectById(record.getTaskId());
            if (task != null) {
                vo.setTaskName(task.getName());
            }
        }

        // 加载检测目标列表
        LambdaQueryWrapper<Detection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Detection::getRecordId, record.getId());
        List<Detection> detections = detectionMapper.selectList(wrapper);

        List<DetectionVO> detectionVOs = detections.stream()
                .map(DetectionVO::fromEntity)
                .collect(Collectors.toList());

        vo.setDetections(detectionVOs);

        return vo;
    }
}
