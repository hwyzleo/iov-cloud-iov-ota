-- DSN-CR-004 补充: 删除冗余字段 release_date，由 release_time 取代

-- 1. 数据迁移：将 release_date 值回填至 release_time（仅 release_time 为空时）
UPDATE `tb_software_build_version`
    SET `release_time` = `release_date`
    WHERE `release_time` IS NULL AND `release_date` IS NOT NULL;

-- 2. 删除冗余字段
ALTER TABLE `tb_software_build_version` DROP COLUMN `release_date`;
