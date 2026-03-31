-- 监测任务表
CREATE TABLE vision.monitor_task (
    id                   VARCHAR(36) PRIMARY KEY,
    name                 VARCHAR(200) NOT NULL,
    description          TEXT,
    business_line        VARCHAR(100),
    group_id             VARCHAR(36) REFERENCES vision.camera_group(id),
    model_id             VARCHAR(36) REFERENCES vision.model(id),
    status               VARCHAR(20) DEFAULT 'stopped',
    capture_frequency    VARCHAR(50) DEFAULT '5min',
    schedule_start_time  VARCHAR(10),
    schedule_end_time    VARCHAR(10),
    schedule_weekdays    VARCHAR(20),
    effective_start      DATE,
    effective_end        DATE,

    -- 告警条件（简化版，直接嵌入）
    alert_target         VARCHAR(500),
    alert_confidence     DECIMAL(3,2) DEFAULT 0.50,
    alert_frames         INT DEFAULT 1,

    -- 告警推送
    alert_level          VARCHAR(20) DEFAULT 'warning',
    push_methods         VARCHAR(200),
    callback_url         VARCHAR(1000),
    callback_headers     JSONB,

    -- 节点策略
    node_ids             VARCHAR(500),

    -- 统计字段
    total_inference      BIGINT DEFAULT 0,
    total_alert          BIGINT DEFAULT 0,
    last_inference_time  TIMESTAMP,
    last_alert_time      TIMESTAMP,

    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INT DEFAULT 0
);

CREATE INDEX idx_monitor_task_status ON vision.monitor_task(status);
CREATE INDEX idx_monitor_task_group ON vision.monitor_task(group_id);
CREATE INDEX idx_monitor_task_model ON vision.monitor_task(model_id);
CREATE INDEX idx_monitor_task_business ON vision.monitor_task(business_line);

-- 推理记录新增 task_id 列
ALTER TABLE vision.inference_record ADD COLUMN IF NOT EXISTS task_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS idx_inference_record_task ON vision.inference_record(task_id);

-- 告警新增 task_id 列
ALTER TABLE vision.alert ADD COLUMN IF NOT EXISTS task_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS idx_alert_task ON vision.alert(task_id);
