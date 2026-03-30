package com.vision.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * 开发环境自动启动 Python 推理服务。
 * 仅在 dev profile 且 vision.inference.auto-start=true 时生效。
 */
@Component
@Profile("dev")
public class InferenceProcessRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InferenceProcessRunner.class);

    @Value("${vision.inference.auto-start:false}")
    private boolean autoStart;

    @Value("${vision.inference.python-path:python}")
    private String pythonPath;

    @Value("${vision.inference.working-dir:../vision-inference}")
    private String workingDir;

    @Value("${vision.inference.service-url:http://localhost:5000}")
    private String serviceUrl;

    private Process process;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoStart) {
            return;
        }

        // 先检查推理服务是否已经在运行
        if (isServiceAlive()) {
            log.info("[InferenceProcess] 推理服务已在运行 ({}), 跳过启动", serviceUrl);
            return;
        }

        File dir = new File(workingDir);
        if (!dir.isDirectory()) {
            log.error("[InferenceProcess] 工作目录不存在: {}", dir.getAbsolutePath());
            return;
        }

        File appPy = new File(dir, "app.py");
        if (!appPy.exists()) {
            log.error("[InferenceProcess] app.py 不存在: {}", appPy.getAbsolutePath());
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(pythonPath, "app.py");
            pb.directory(dir);
            pb.redirectErrorStream(true);

            // 日志输出到文件，避免和 Java 日志混在一起
            File logFile = new File(dir, "logs/inference-subprocess.log");
            logFile.getParentFile().mkdirs();
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

            process = pb.start();
            log.info("[InferenceProcess] 推理服务已启动 (PID: {}, 日志: {})", process.pid(), logFile.getAbsolutePath());

            // 等待服务就绪
            waitForReady();

        } catch (Exception e) {
            log.error("[InferenceProcess] 启动推理服务失败: {}", e.getMessage(), e);
        }
    }

    private void waitForReady() {
        int maxRetries = 30;
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (!process.isAlive()) {
                log.error("[InferenceProcess] 推理服务进程已退出, exitCode={}", process.exitValue());
                return;
            }

            if (isServiceAlive()) {
                log.info("[InferenceProcess] 推理服务就绪 ✓ ({})", serviceUrl);
                return;
            }
        }
        log.warn("[InferenceProcess] 等待推理服务就绪超时 ({}s)", maxRetries);
    }

    private boolean isServiceAlive() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(serviceUrl + "/health").toURL().openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (process != null && process.isAlive()) {
            log.info("[InferenceProcess] 正在关闭推理服务 (PID: {})...", process.pid());
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            log.info("[InferenceProcess] 推理服务已关闭");
        }
    }
}
