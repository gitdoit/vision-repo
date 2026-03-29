package com.vision.inference.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vision.inference.dto.InferenceQueryDTO;
import com.vision.inference.dto.InferenceRecordVO;
import com.vision.inference.service.InferenceService;
import com.vision.common.response.PageResult;
import com.vision.common.response.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 推理记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inference")
@RequiredArgsConstructor
public class InferenceController {

    private final InferenceService inferenceService;

    /**
     * 分页查询推理记录
     */
    @GetMapping
    public R<PageResult<InferenceRecordVO>> pageRecords(InferenceQueryDTO dto) {
        IPage<InferenceRecordVO> result = inferenceService.pageRecords(dto);
        return R.ok(new PageResult<>(result.getRecords(), result.getTotal()));
    }

    /**
     * 查询推理详情
     */
    @GetMapping("/{id}")
    public R<InferenceRecordVO> getRecord(@PathVariable String id) {
        InferenceRecordVO vo = inferenceService.getRecordById(id);
        return R.ok(vo);
    }

    /**
     * 导出CSV
     */
    @GetMapping("/export/csv")
    public void exportCsv(InferenceQueryDTO dto, HttpServletResponse response) throws IOException {
        log.info("导出推理记录CSV");
        byte[] data = inferenceService.exportToCsv(dto);

        String filename = "inference_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        response.setContentLength(data.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
        }
    }

    /**
     * 导出Excel
     */
    @GetMapping("/export/excel")
    public void exportExcel(InferenceQueryDTO dto, HttpServletResponse response) throws IOException {
        log.info("导出推理记录Excel");
        byte[] data = inferenceService.exportToExcel(dto);

        String filename = "inference_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        response.setContentLength(data.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
        }
    }

    /**
     * 推理结果回调接口
     */
    @PostMapping("/callback")
    public R<Void> callback(@RequestBody String callbackData) {
        log.info("收到推理结果回调");
        inferenceService.handleCallback(callbackData);
        return R.ok();
    }
}
