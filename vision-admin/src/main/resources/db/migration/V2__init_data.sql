-- Vision Analysis Platform - Initial Data
-- Version 1.0

-- 插入默认摄像头分组
INSERT INTO camera_group (id, name, parent_id, icon, sort_order) VALUES
('0a1b2c3d4e5f6g7h8i9j0k1l', '默认分组', NULL, 'folder', 0),
('1a2b3c4d5e6f7g8h9i0j1k2l', '生产区域', '0a1b2c3d4e5f6g7h8i9j0k1l', 'factory', 1),
('2a3b4c5d6e7f8g9h0i1j2k3l', '办公区域', '0a1b2c3d4e5f6g7h8i9j0k1l', 'office', 2),
('3a4b5c6d7e8f9g0h1i2j3k4l', '仓储区域', '0a1b2c3d4e5f6g7h8i9j0k1l', 'warehouse', 3);

-- 插入系统配置
INSERT INTO system_config (config_key, config_value, description) VALUES
('capture.enabled', 'true', '是否启用自动抓图'),
('capture.default_frequency', '5min', '默认抓图频率'),
('inference.confidence_threshold', '0.5', '默认推理置信度阈值'),
('alert.websocket_enabled', 'true', '是否启用 WebSocket 告警推送'),
('alert.retention_days', '30', '告警保留天数'),
('inference.retention_days', '7', '推理记录保留天数'),
('system.timezone', 'Asia/Shanghai', '系统时区'),
('system.version', '1.0.0', '系统版本');
