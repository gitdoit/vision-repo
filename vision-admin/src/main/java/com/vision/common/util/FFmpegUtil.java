package com.vision.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 工具类 - 封装 FFmpeg 命令调用
 *
 * 职责:
 * - 执行 FFmpeg 抓帧命令
 * - 处理超时和错误流
 * - 进程资源清理
 */
@Slf4j
public class FFmpegUtil {

    private static final int TIMEOUT_SECONDS = 30;

    /**
     * 从 RTSP 流抓取一帧图片
     *
     * @param ffmpegPath FFmpeg 可执行文件路径
     * @param rtspUrl    RTSP 流地址
     * @param outputPath 输出图片路径
     * @return 是否成功
     */
    public static boolean captureFrame(String ffmpegPath, String rtspUrl, String outputPath) {
        log.debug("开始抓帧: rtspUrl={}, outputPath={}", rtspUrl, outputPath);

        // 确保输出目录存在
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath,
                "-rtsp_transport", "tcp",
                "-i", rtspUrl,
                "-frames:v", "1",
                "-f", "image2",
                "-y",  // 覆盖已存在的文件
                outputPath
        );

        processBuilder.redirectErrorStream(true);

        Process process = null;
        try {
            process = processBuilder.start();

            // 读取输出流，避免缓冲区满导致阻塞
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待进程完成，设置超时
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                log.warn("FFmpeg 抓帧超时: rtspUrl={}", rtspUrl);
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                // 验证输出文件是否存在且有效
                if (outputFile.exists() && outputFile.length() > 0) {
                    log.debug("抓帧成功: outputPath={}, size={}", outputPath, outputFile.length());
                    return true;
                } else {
                    log.warn("FFmpeg 进程成功但输出文件无效: outputPath={}", outputPath);
                    return false;
                }
            } else {
                log.warn("FFmpeg 抓帧失败: exitCode={}, rtspUrl={}, output={}",
                        exitCode, rtspUrl, output);
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("FFmpeg 抓帧被中断: rtspUrl={}", rtspUrl, e);
            if (process != null) {
                process.destroyForcibly();
            }
            return false;
        } catch (Exception e) {
            log.error("FFmpeg 抓帧异常: rtspUrl={}", rtspUrl, e);
            if (process != null) {
                process.destroyForcibly();
            }
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 从 RTSP 流抓取一帧图片（使用默认 ffmpeg 路径）
     *
     * @param rtspUrl    RTSP 流地址
     * @param outputPath 输出图片路径
     * @return 是否成功
     */
    public static boolean captureFrame(String rtspUrl, String outputPath) {
        return captureFrame("ffmpeg", rtspUrl, outputPath);
    }

    /**
     * 检查 FFmpeg 是否可用
     *
     * @param ffmpegPath FFmpeg 可执行文件路径
     * @return 是否可用
     */
    public static boolean checkAvailable(String ffmpegPath) {
        try {
            Process process = new ProcessBuilder(ffmpegPath, "-version").start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                log.debug("FFmpeg 可用: {}", ffmpegPath);
                return true;
            }
        } catch (Exception e) {
            log.warn("FFmpeg 不可用: {}", ffmpegPath, e);
        }
        return false;
    }
}
