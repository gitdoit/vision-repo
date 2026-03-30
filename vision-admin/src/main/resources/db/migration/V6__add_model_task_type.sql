-- 模型任务类型: detect(目标检测), segment(实例分割), classify(分类), pose(姿态估计)
ALTER TABLE vision.model ADD COLUMN task_type VARCHAR(20) DEFAULT 'detect';
