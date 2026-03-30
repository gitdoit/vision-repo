-- 新增推理节点表
CREATE TABLE IF NOT EXISTS vision.inference_node (
    id              VARCHAR(36) PRIMARY KEY,
    node_name       VARCHAR(200) NOT NULL,
    host            VARCHAR(500) NOT NULL,
    port            INT NOT NULL DEFAULT 5000,
    status          VARCHAR(20) DEFAULT 'offline',
    device_type     VARCHAR(20),
    gpu_name        VARCHAR(200),
    gpu_count       INT DEFAULT 0,
    cpu_info        VARCHAR(200),
    memory_total    BIGINT,
    last_heartbeat  TIMESTAMP,
    registered_at   TIMESTAMP DEFAULT NOW(),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    deleted         INT DEFAULT 0
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_inference_node_status ON vision.inference_node(status);

-- 模型表新增 node_id 字段
ALTER TABLE vision.model ADD COLUMN IF NOT EXISTS node_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS idx_model_node ON vision.model(node_id);
