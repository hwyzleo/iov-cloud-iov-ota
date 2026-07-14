-- CR-002: 允许 target_software_build_ver 为空
-- 基线活动创建时可能还没有指定目标软件版本
ALTER TABLE tb_activity_target_version
    MODIFY COLUMN `target_software_build_ver` VARCHAR(255) DEFAULT NULL COMMENT '目标软件内部版本';
