-- 添加 device 和 device_name 字段，记录模型当前加载在哪个设备上
ALTER TABLE model ADD COLUMN device VARCHAR(20);
ALTER TABLE model ADD COLUMN device_name VARCHAR(100);
