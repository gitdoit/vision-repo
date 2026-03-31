#!/bin/bash
# deploy.sh - Visual Analysis Platform Deployment Script

set -e

echo "=============================="
echo "  视觉分析平台 - 部署工具"
echo "=============================="

# Default values
MODE="lite"
DEVICE="cpu"
COMPOSE_FILES=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --mode)
            MODE="$2"
            shift 2
            ;;
        --device)
            DEVICE="$2"
            shift 2
            ;;
        --help|-h)
            echo "用法: $0 [选项]"
            echo ""
            echo "选项:"
            echo "  --mode MODE     部署模式: lite (轻量) 或 full (完整)"
            echo "  --device DEVICE 推理设备: cpu 或 gpu"
            echo "  --help, -h      显示帮助信息"
            echo ""
            echo "示例:"
            echo "  $0 --mode lite --device cpu"
            echo "  $0 --mode full --device gpu"
            exit 0
            ;;
        *)
            echo "未知选项: $1"
            echo "使用 --help 查看帮助"
            exit 1
            ;;
    esac
done

# Interactive mode if no arguments provided
if [ $# -eq 0 ]; then
    echo ""
    echo "请选择部署模式:"
    echo "  1) lite  - 轻量模式（本地磁盘存储，无 MinIO）"
    echo "  2) full  - 完整模式（含 MinIO 对象存储）"
    read -p "请输入 [1/2] (默认 1): " mode_choice

    echo ""
    echo "请选择推理设备:"
    echo "  1) cpu   - CPU 推理"
    echo "  2) gpu   - GPU 推理（需要 nvidia-docker）"
    read -p "请输入 [1/2] (默认 1): " device_choice

    if [ "$mode_choice" = "2" ]; then
        MODE="full"
    else
        MODE="lite"
    fi

    if [ "$device_choice" = "2" ]; then
        DEVICE="cuda"
    else
        DEVICE="cpu"
    fi
fi

# Set variables
if [ "$MODE" = "full" ]; then
    STORAGE_TYPE="minio"
    COMPOSE_PROFILES="full"
else
    STORAGE_TYPE="local"
    COMPOSE_PROFILES=""
fi

INFERENCE_DEVICE="$DEVICE"

# Copy .env file if not exists
if [ ! -f .env ]; then
    cp .env.example .env
    echo "已创建 .env 文件，请根据需要修改配置"
fi

# Export variables
export STORAGE_TYPE=$STORAGE_TYPE
export INFERENCE_DEVICE=$INFERENCE_DEVICE

# Select compose file
if [ "$MODE" = "full" ]; then
    COMPOSE_FILES="-f docker-compose.yml --profile full"
else
    COMPOSE_FILES="-f docker-compose.lite.yml"
fi

# Build and start
echo ""
echo "部署配置:"
echo "  模式: $MODE"
echo "  存储: $STORAGE_TYPE"
echo "  设备: $INFERENCE_DEVICE"
echo ""
echo "开始构建和部署..."

docker compose $COMPOSE_FILES up -d --build

echo ""
echo "=============================="
echo "  部署完成!"
echo "=============================="
echo "  管理后台: http://localhost:26330"
echo "  推理服务: http://localhost:26331/health"
if [ "$STORAGE_TYPE" = "minio" ]; then
    echo "  MinIO 控制台: http://localhost:9001"
    echo "  MinIO 用户: ${MINIO_ACCESS_KEY:-minioadmin}"
    echo "  MinIO 密码: ${MINIO_SECRET_KEY:-minioadmin}"
fi
echo "  数据库: postgresql://localhost:5432/vision"
echo "  数据库用户: ${DB_USER:-vision}"
echo "=============================="
echo ""
echo "查看日志: docker compose logs -f"
echo "停止服务: docker compose down"
