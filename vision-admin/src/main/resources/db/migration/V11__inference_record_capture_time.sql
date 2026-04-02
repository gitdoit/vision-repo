-- V11: 推理记录增加抓帧耗时字段
ALTER TABLE inference_record ADD COLUMN IF NOT EXISTS capture_time_ms INTEGER;

COMMENT ON COLUMN inference_record.capture_time_ms IS '抓帧耗时(毫秒)';
