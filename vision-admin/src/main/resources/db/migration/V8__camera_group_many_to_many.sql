-- 摄像头与分组多对多关系改造
-- 新增中间表 camera_group_mapping，迁移已有关联数据，删除 camera.group_id 列

-- 1. 创建中间表
CREATE TABLE vision.camera_group_mapping (
    id          VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    camera_id   VARCHAR(36) NOT NULL REFERENCES vision.camera(id) ON DELETE CASCADE,
    group_id    VARCHAR(36) NOT NULL REFERENCES vision.camera_group(id) ON DELETE CASCADE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_camera_group UNIQUE (camera_id, group_id)
);

CREATE INDEX idx_cgm_camera_id ON vision.camera_group_mapping(camera_id);
CREATE INDEX idx_cgm_group_id ON vision.camera_group_mapping(group_id);

-- 2. 迁移已有 group_id 数据到中间表
INSERT INTO vision.camera_group_mapping (id, camera_id, group_id)
SELECT gen_random_uuid()::varchar, id, group_id
FROM vision.camera
WHERE group_id IS NOT NULL AND group_id != '';

-- 3. 删除旧的外键约束和索引，然后删除列
ALTER TABLE vision.camera DROP CONSTRAINT IF EXISTS camera_group_id_fkey;
DROP INDEX IF EXISTS vision.idx_camera_group_id;
ALTER TABLE vision.camera DROP COLUMN group_id;
