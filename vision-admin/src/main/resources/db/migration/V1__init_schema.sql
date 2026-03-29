-- Vision Analysis Platform - Database Schema
-- Version 1.0
-- Author: Vision Team

-- 摄像头分组表
CREATE TABLE camera_group (
    id            VARCHAR(36) PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    parent_id     VARCHAR(36),
    icon          VARCHAR(50),
    sort_order    INT DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 摄像头表
CREATE TABLE camera (
    id                VARCHAR(36) PRIMARY KEY,
    name              VARCHAR(200) NOT NULL,
    business_line     VARCHAR(50) NOT NULL,
    location          VARCHAR(500),
    stream_url        VARCHAR(1000),
    capture_frequency VARCHAR(50) DEFAULT '5min',
    ai_enabled        BOOLEAN DEFAULT FALSE,
    status            VARCHAR(20) DEFAULT 'offline',
    last_capture_time TIMESTAMP,
    group_id          VARCHAR(36) REFERENCES camera_group(id),
    source            VARCHAR(20) DEFAULT 'manual',
    platform_id       VARCHAR(36),
    channel_no        VARCHAR(100),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 视频平台表
CREATE TABLE video_platform (
    id                VARCHAR(36) PRIMARY KEY,
    name              VARCHAR(200) NOT NULL,
    api_base          VARCHAR(500) NOT NULL,
    auth_type         VARCHAR(20) DEFAULT 'none',
    credential        VARCHAR(500),
    auto_sync         BOOLEAN DEFAULT FALSE,
    sync_interval_min INT DEFAULT 60,
    last_sync_time    TIMESTAMP,
    last_sync_result  JSONB,
    cameras_count     INT DEFAULT 0,
    status            VARCHAR(20) DEFAULT 'disconnected',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 模型表
CREATE TABLE model (
    id                   VARCHAR(36) PRIMARY KEY,
    name                 VARCHAR(200) NOT NULL,
    version              VARCHAR(50) NOT NULL,
    business_tag         VARCHAR(100),
    engine_support       VARCHAR(200),
    target_hardware      VARCHAR(100),
    status               VARCHAR(20) DEFAULT 'unloaded',
    confidence_threshold DECIMAL(3,2) DEFAULT 0.50,
    input_resolution     VARCHAR(20) DEFAULT '640x640',
    max_concurrency      INT DEFAULT 1,
    model_path           VARCHAR(500),
    author               VARCHAR(100),
    avg_latency          INT DEFAULT 0,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 模型版本历史表
CREATE TABLE model_version (
    id          VARCHAR(36) PRIMARY KEY,
    model_id    VARCHAR(36) REFERENCES model(id) ON DELETE CASCADE,
    version     VARCHAR(50) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 规则表
CREATE TABLE rule (
    id              VARCHAR(36) PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    business_line   VARCHAR(50),
    enabled         BOOLEAN DEFAULT TRUE,
    priority        VARCHAR(20) DEFAULT 'warning',
    schedule        VARCHAR(50),
    weekdays        VARCHAR(20),
    effective_start DATE,
    effective_end   DATE,
    actions         JSONB NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 规则条件表
CREATE TABLE rule_condition (
    id        VARCHAR(36) PRIMARY KEY,
    rule_id   VARCHAR(36) REFERENCES rule(id) ON DELETE CASCADE,
    type      VARCHAR(20) NOT NULL,
    operator  VARCHAR(10) NOT NULL,
    value     VARCHAR(200) NOT NULL
);

-- 推理记录表
CREATE TABLE inference_record (
    id                 VARCHAR(36) PRIMARY KEY,
    event_id           VARCHAR(36),
    camera_id          VARCHAR(36) REFERENCES camera(id),
    business_type      VARCHAR(100),
    avg_confidence     DECIMAL(3,2),
    alert_status       VARCHAR(20) DEFAULT 'normal',
    thumbnail_url      VARCHAR(500),
    original_image_url VARCHAR(500),
    annotated_image_url VARCHAR(500),
    raw_json           JSONB,
    model_name         VARCHAR(200),
    inference_time_ms  INT,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 检测目标表
CREATE TABLE detection (
    id            VARCHAR(36) PRIMARY KEY,
    record_id     VARCHAR(36) REFERENCES inference_record(id) ON DELETE CASCADE,
    label         VARCHAR(100) NOT NULL,
    confidence    DECIMAL(4,3),
    bbox          VARCHAR(100),
    count         INT DEFAULT 1,
    attributes    JSONB
);

-- 告警表
CREATE TABLE alert (
    id                VARCHAR(36) PRIMARY KEY,
    alert_level       VARCHAR(20) NOT NULL,
    alert_type        VARCHAR(100),
    scene             VARCHAR(50),
    camera_id         VARCHAR(36) REFERENCES camera(id),
    stream_id         VARCHAR(100),
    capture_time      TIMESTAMP,
    alert_time        TIMESTAMP,
    trigger_condition TEXT,
    related_objects   JSONB,
    evidence          JSONB,
    location          JSONB,
    rule_id           VARCHAR(36),
    read_status       BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 系统配置表
CREATE TABLE system_config (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value TEXT,
    description  VARCHAR(500),
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_camera_group ON camera(group_id);
CREATE INDEX idx_camera_status ON camera(status);
CREATE INDEX idx_camera_business_line ON camera(business_line);
CREATE INDEX idx_inference_camera ON inference_record(camera_id);
CREATE INDEX idx_inference_created ON inference_record(created_at);
CREATE INDEX idx_inference_alert_status ON inference_record(alert_status);
CREATE INDEX idx_alert_created ON alert(created_at);
CREATE INDEX idx_alert_level ON alert(alert_level);
CREATE INDEX idx_alert_camera ON alert(camera_id);
CREATE INDEX idx_alert_read_status ON alert(read_status);
CREATE INDEX idx_detection_record ON detection(record_id);
CREATE INDEX idx_rule_enabled ON rule(enabled);
CREATE INDEX idx_rule_condition_rule ON rule_condition(rule_id);
CREATE INDEX idx_model_status ON model(status);
CREATE INDEX idx_camera_group_parent ON camera_group(parent_id);
