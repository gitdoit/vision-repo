package com.vision.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
     * 从视频流抓取一帧图片（支持 RTSP / HTTP-FLV / WS-FLV）
     *
     * @param ffmpegPath FFmpeg 可执行文件路径
     * @param streamUrl  视频流地址（rtsp:// / http:// / ws:// 等）
     * @param outputPath 输出图片路径
     * @return 是否成功
     */
    public static boolean captureFrame(String ffmpegPath, String streamUrl, String outputPath) {
        log.debug("开始抓帧: streamUrl={}, outputPath={}", streamUrl, outputPath);

        // 确保输出目录存在
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 根据协议构建不同的 FFmpeg 命令参数
        String effectiveUrl = streamUrl;
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);

        String lower = streamUrl.toLowerCase();
        if (lower.startsWith("rtsp://")) {
            // RTSP 流：使用 TCP 传输
            command.add("-rtsp_transport");
            command.add("tcp");
        } else if (lower.startsWith("ws://") || lower.startsWith("wss://")) {
            // WebSocket FLV 流：FFmpeg 不支持 ws://，转换为 http:// / https://
            effectiveUrl = streamUrl.replaceFirst("(?i)^wss://", "https://")
                                    .replaceFirst("(?i)^ws://", "http://");
            log.debug("WebSocket 流转换为 HTTP: {} -> {}", streamUrl, effectiveUrl);
        }
        // http:// / https:// 流无需额外参数

        command.add("-i");
        command.add(effectiveUrl);
        command.add("-frames:v");
        command.add("1");
        command.add("-f");
        command.add("image2");
        command.add("-y");  // 覆盖已存在的文件
        command.add(outputPath);

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        // 清除代理环境变量，防止 FFmpeg 子进程经由系统代理访问内网流媒体地址（导致 502）
        java.util.Map<String, String> env = processBuilder.environment();
        env.remove("http_proxy");
        env.remove("https_proxy");
        env.remove("HTTP_PROXY");
        env.remove("HTTPS_PROXY");
        env.remove("all_proxy");
        env.remove("ALL_PROXY");

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
                log.warn("FFmpeg 抓帧超时: streamUrl={}", streamUrl);
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
                log.warn("FFmpeg 抓帧失败: exitCode={}, streamUrl={}, output={}",
                        exitCode, streamUrl, output);
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("FFmpeg 抓帧被中断: streamUrl={}", streamUrl, e);
            if (process != null) {
                process.destroyForcibly();
            }
            return false;
        } catch (Exception e) {
            log.error("FFmpeg 抓帧异常: streamUrl={}", streamUrl, e);
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
     * 从视频流抓取一帧图片（使用默认 ffmpeg 路径）
     *
     * @param streamUrl  视频流地址
     * @param outputPath 输出图片路径
     * @return 是否成功
     */
    public static boolean captureFrame(String streamUrl, String outputPath) {
        return captureFrame("ffmpeg", streamUrl, outputPath);
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
