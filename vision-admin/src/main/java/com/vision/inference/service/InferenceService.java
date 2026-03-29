package com.vision.inference.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vision.camera.entity.Camera;
import com.vision.camera.mapper.CameraMapper;
import com.vision.inference.dto.DetectionVO;
import com.vision.inference.dto.InferenceQueryDTO;
import com.vision.inference.dto.InferenceRecordVO;
import com.vision.inference.entity.Detection;
import com.vision.inference.entity.InferenceRecord;
import com.vision.inference.mapper.DetectionMapper;
import com.vision.inference.mapper.InferenceMapper;
import com.vision.common.exception.BizException;
import com.vision.common.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
                dto.getSize(),
                offset
        );

        Long total = inferenceMapper.countByCondition(
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getCameraId(),
                dto.getAlertType()
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
                detection.setCreatedAt(LocalDateTime.now());
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

        // 加载摄像头名称
        if (record.getCameraId() != null) {
            Camera camera = cameraMapper.selectById(record.getCameraId());
            if (camera != null) {
                vo.setCameraName(camera.getName());
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
