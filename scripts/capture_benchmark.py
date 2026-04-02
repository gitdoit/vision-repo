"""
FFmpeg 抓帧性能分析脚本

诊断 WS-FLV 流抓帧耗时瓶颈，测试不同参数组合的影响。
用法: python scripts/capture_benchmark.py
"""
import subprocess
import time
import os
import sys

STREAMS = [
    "ws://10.100.121.12:8086/rtp/34011146002008113700_34011146001328139460.live.flv",
    "ws://10.100.121.12:8086/rtp/34012300001180000002_34012300001320000004.live.flv",
    "ws://10.100.121.12:8086/rtp/34011146002008113700_34011146001328139462.live.flv",
    "ws://10.100.121.12:8086/rtp/34011146002008113700_34011146001328139458.live.flv",
]

FFMPEG = "ffmpeg"
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "tmp", "benchmark")
ROUNDS = 2  # 每个配置跑几轮


def ws_to_http(url: str) -> str:
    """ws:// -> http://, wss:// -> https://"""
    return url.replace("wss://", "https://", 1).replace("ws://", "http://", 1)


# ---------- 参数组合 ----------
# 每组: (名称, 插入到 -i 之前的参数列表)
PROFILES = {
    "baseline（当前线上）": [],

    "analyzeduration=500ms + probesize=32K": [
        "-analyzeduration", "500000",   # 微秒，默认 5000000 (5s)
        "-probesize", "32768",          # 字节，默认 5000000 (5MB)
    ],

    "analyzeduration=100ms + probesize=32K": [
        "-analyzeduration", "100000",
        "-probesize", "32768",
    ],

    "fflags=nobuffer + flags=low_delay": [
        "-fflags", "nobuffer",
        "-flags", "low_delay",
    ],

    "全部优化: nobuffer+low_delay+analyze100ms+probe32K": [
        "-fflags", "nobuffer",
        "-flags", "low_delay",
        "-analyzeduration", "100000",
        "-probesize", "32768",
    ],

    "全部优化 + avioflags=direct": [
        "-fflags", "nobuffer",
        "-flags", "low_delay",
        "-avioflags", "direct",
        "-analyzeduration", "100000",
        "-probesize", "32768",
    ],

    "rw_timeout=3s + 全部优化": [
        "-rw_timeout", "3000000",       # 微秒，网络读写超时
        "-fflags", "nobuffer",
        "-flags", "low_delay",
        "-analyzeduration", "100000",
        "-probesize", "32768",
    ],
}


def run_capture(stream_url: str, profile_name: str, extra_args: list, idx: int) -> dict:
    """执行一次抓帧并收集耗时明细"""
    http_url = ws_to_http(stream_url)
    out_file = os.path.join(
        OUTPUT_DIR,
        f"bench_{idx}_{profile_name.replace(' ', '_')[:30]}.jpg"
    )

    cmd = [FFMPEG, "-hide_banner"]
    cmd.extend(extra_args)
    cmd.extend(["-i", http_url, "-frames:v", "1", "-f", "image2", "-y", out_file])

    env = os.environ.copy()
    for k in ["http_proxy", "https_proxy", "HTTP_PROXY", "HTTPS_PROXY", "all_proxy", "ALL_PROXY"]:
        env.pop(k, None)

    t0 = time.perf_counter()
    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=30,
            env=env,
        )
        elapsed = time.perf_counter() - t0
        success = result.returncode == 0 and os.path.exists(out_file) and os.path.getsize(out_file) > 0
        file_size = os.path.getsize(out_file) if os.path.exists(out_file) else 0

        # 从 stderr 提取关键时间信息
        stderr = result.stderr or ""
        return {
            "success": success,
            "elapsed_s": round(elapsed, 3),
            "exit_code": result.returncode,
            "file_size": file_size,
            "stderr_tail": stderr[-500:] if stderr else "",
        }
    except subprocess.TimeoutExpired:
        elapsed = time.perf_counter() - t0
        return {
            "success": False,
            "elapsed_s": round(elapsed, 3),
            "exit_code": -1,
            "file_size": 0,
            "stderr_tail": "TIMEOUT",
        }
    except Exception as e:
        elapsed = time.perf_counter() - t0
        return {
            "success": False,
            "elapsed_s": round(elapsed, 3),
            "exit_code": -1,
            "file_size": 0,
            "stderr_tail": str(e),
        }


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # 先用 baseline 对每个流做连通性测试
    print("=" * 80)
    print("FFmpeg 抓帧性能分析")
    print("=" * 80)

    # 1. 分阶段诊断 — 只用第一个流
    test_stream = STREAMS[0]
    http_url = ws_to_http(test_stream)
    print(f"\n▶ 阶段 1: 网络连通性测试 (curl HEAD)")
    print(f"  流地址: {http_url}")
    try:
        t0 = time.perf_counter()
        r = subprocess.run(
            ["curl", "-sS", "-o", "/dev/null", "-w",
             "http_code=%{http_code} time_connect=%{time_connect}s time_starttransfer=%{time_starttransfer}s time_total=%{time_total}s",
             "--max-time", "10", http_url],
            capture_output=True, text=True, timeout=15,
            env={k: v for k, v in os.environ.items() if k.lower() not in
                 ("http_proxy", "https_proxy", "all_proxy")},
        )
        print(f"  curl 输出: {r.stdout.strip()}")
        if r.returncode != 0:
            print(f"  curl stderr: {r.stderr.strip()[:200]}")
    except Exception as e:
        print(f"  curl 异常: {e}")

    # 2. FFmpeg 详细输出分析 (带 -loglevel verbose)
    print(f"\n▶ 阶段 2: FFmpeg verbose 分析 (baseline，看阶段耗时)")
    verbose_out = os.path.join(OUTPUT_DIR, "verbose_test.jpg")
    cmd_verbose = [
        FFMPEG, "-hide_banner", "-loglevel", "verbose",
        "-i", http_url, "-frames:v", "1", "-f", "image2", "-y", verbose_out
    ]
    env = {k: v for k, v in os.environ.items() if k.lower() not in
           ("http_proxy", "https_proxy", "all_proxy")}
    t0 = time.perf_counter()
    try:
        r = subprocess.run(cmd_verbose, capture_output=True, text=True, timeout=30, env=env)
        elapsed = time.perf_counter() - t0
        stderr = r.stderr or ""
        print(f"  总耗时: {elapsed:.3f}s, 退出码: {r.returncode}")
        # 提取关键行
        key_lines = []
        for line in stderr.splitlines():
            lower = line.lower()
            if any(kw in lower for kw in [
                "duration", "start", "stream #", "output #",
                "frame=", "time=", "speed=", "opening", "parsed",
                "probe", "analyze", "header", "connected"
            ]):
                key_lines.append(line.strip())
        if key_lines:
            print("  关键日志:")
            for l in key_lines[:25]:
                print(f"    {l}")
        else:
            print(f"  完整 stderr (后500字符):\n    {stderr[-500:]}")
    except subprocess.TimeoutExpired:
        print("  FFmpeg verbose 超时 (30s)")
    except Exception as e:
        print(f"  异常: {e}")

    # 3. 各参数组合对比测试
    print(f"\n▶ 阶段 3: 参数组合对比 ({ROUNDS} 轮 × {len(STREAMS)} 流 × {len(PROFILES)} 配置)")
    print("-" * 80)

    results = {}  # profile -> list of elapsed
    for profile_name, extra_args in PROFILES.items():
        results[profile_name] = []
        print(f"\n  配置: [{profile_name}]")
        for round_i in range(ROUNDS):
            for si, stream in enumerate(STREAMS):
                r = run_capture(stream, profile_name, extra_args, si)
                tag = "✓" if r["success"] else "✗"
                print(f"    轮{round_i+1} 流{si+1}: {tag} {r['elapsed_s']:.3f}s  (size={r['file_size']})")
                if r["success"]:
                    results[profile_name].append(r["elapsed_s"])
                elif "TIMEOUT" not in r["stderr_tail"]:
                    # 打印失败原因（截取最后一行有用信息）
                    last_lines = [l for l in r["stderr_tail"].splitlines() if l.strip()]
                    if last_lines:
                        print(f"      失败原因: {last_lines[-1][:120]}")

    # 4. 汇总
    print("\n" + "=" * 80)
    print("汇总结果")
    print("=" * 80)
    print(f"{'配置':<50} {'成功数':>6} {'平均(s)':>8} {'最小(s)':>8} {'最大(s)':>8}")
    print("-" * 80)
    for profile_name in PROFILES:
        times = results[profile_name]
        if times:
            avg = sum(times) / len(times)
            mn = min(times)
            mx = max(times)
            print(f"{profile_name:<50} {len(times):>6} {avg:>8.3f} {mn:>8.3f} {mx:>8.3f}")
        else:
            print(f"{profile_name:<50} {'0':>6} {'N/A':>8} {'N/A':>8} {'N/A':>8}")

    print("\n分析建议:")
    if results.get("baseline（当前线上）"):
        baseline_avg = sum(results["baseline（当前线上）"]) / len(results["baseline（当前线上）"])
        best_name = None
        best_avg = baseline_avg
        for name, times in results.items():
            if name == "baseline（当前线上）" or not times:
                continue
            avg = sum(times) / len(times)
            if avg < best_avg:
                best_avg = avg
                best_name = name
        if best_name:
            improvement = (baseline_avg - best_avg) / baseline_avg * 100
            print(f"  - baseline 平均: {baseline_avg:.3f}s")
            print(f"  - 最佳配置: [{best_name}], 平均: {best_avg:.3f}s, 提升: {improvement:.1f}%")
        else:
            print("  - 所有优化配置均未优于 baseline，瓶颈可能在网络/流媒体服务器侧")
    else:
        print("  - baseline 全部失败，请检查网络连通性和流地址是否有效")

    # 清理
    print(f"\n测试文件保存在: {os.path.abspath(OUTPUT_DIR)}")


if __name__ == "__main__":
    main()
