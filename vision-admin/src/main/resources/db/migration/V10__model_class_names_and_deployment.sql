-- ============================================================
-- V10: 模型类别信息 + 多节点部署表
-- 1. model 表新增 class_names / num_classes / parsed_status
-- 2. 新建 model_node_deployment 关联表（一对多部署）
-- 3. 迁移 model 表中已有的 status/device/device_name/node_id 到部署表
-- 4. 移除 model 表中不再需要的列
-- ============================================================

-- 1. model 表新增类别信息和解析状态
ALTER TABLE vision.model ADD COLUMN IF NOT EXISTS class_names  TEXT;           -- 逗号分隔: "person,car,bicycle"
ALTER TABLE vision.model ADD COLUMN IF NOT EXISTS num_classes  INT;            -- 类别总数
ALTER TABLE vision.model ADD COLUMN IF NOT EXISTS parsed_status VARCHAR(20) DEFAULT 'pending';  -- pending / parsed / failed

-- 2. 多节点部署表
CREATE TABLE IF NOT EXISTS vision.model_node_deployment (
    id          VARCHAR(36) PRIMARY KEY,
    model_id    VARCHAR(36) NOT NULL REFERENCES vision.model(id) ON DELETE CASCADE,
    node_id     VARCHAR(36) NOT NULL REFERENCES vision.inference_node(id) ON DELETE CASCADE,
    device      VARCHAR(20) NOT NULL DEFAULT 'cpu',     -- cpu / cuda
    device_name VARCHAR(200),                            -- e.g. "NVIDIA GeForce RTX 3090"
    status      VARCHAR(20) NOT NULL DEFAULT 'loading',  -- loading / loaded / error
    deployed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(model_id, node_id)                            -- 同一模型在同一节点只能部署一次
);

CREATE INDEX IF NOT EXISTS idx_mnd_model ON vision.model_node_deployment(model_id);
CREATE INDEX IF NOT EXISTS idx_mnd_node  ON vision.model_node_deployment(node_id);

-- 3. 将已有的加载记录迁移到部署表
INSERT INTO vision.model_node_deployment (id, model_id, node_id, device, device_name, status, deployed_at)
SELECT
    gen_random_uuid()::varchar(36),
    m.id,
    m.node_id,
    COALESCE(m.device, 'cpu'),
    m.device_name,
    CASE WHEN m.status = 'loaded' THEN 'loaded' ELSE 'loading' END,
    COALESCE(m.updated_at, NOW())
FROM vision.model m
WHERE m.node_id IS NOT NULL AND m.status = 'loaded';

-- 4. 移除 model 表中不再需要的部署相关列
ALTER TABLE vision.model DROP COLUMN IF EXISTS status;
ALTER TABLE vision.model DROP COLUMN IF EXISTS device;
ALTER TABLE vision.model DROP COLUMN IF EXISTS device_name;
ALTER TABLE vision.model DROP COLUMN IF EXISTS node_id;
