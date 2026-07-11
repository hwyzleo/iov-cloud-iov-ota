-- Fix: tb_software_package may be missing columns from V1 DDL.
-- This happens when the table existed before Flyway V1 migration ran,
-- causing CREATE TABLE IF NOT EXISTS to skip table creation.
-- V5 only fixed row_valid and row_version; this migration fixes the remaining columns.

-- device_code
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'device_code') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `device_code` VARCHAR(20) NOT NULL COMMENT ''设备编码''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- software_pn
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'software_pn') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `software_pn` VARCHAR(50) NOT NULL COMMENT ''软件零件号''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- package_name
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'package_name') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `package_name` VARCHAR(255) NOT NULL COMMENT ''软件包名称''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- package_code
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'package_code') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `package_code` VARCHAR(255) DEFAULT NULL COMMENT ''软件包代码''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- package_url
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'package_url') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `package_url` VARCHAR(255) DEFAULT NULL COMMENT ''软件包URL''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- package_size
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'package_size') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `package_size` BIGINT DEFAULT NULL COMMENT ''软件包大小（Byte）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- package_md5
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'package_md5') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `package_md5` VARCHAR(255) DEFAULT NULL COMMENT ''软件包MD5''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- package_desc
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'package_desc') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `package_desc` VARCHAR(255) DEFAULT NULL COMMENT ''软件包说明''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- package_type
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'package_type') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `package_type` VARCHAR(20) NOT NULL COMMENT ''软件包类型''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- package_source
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'package_source') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `package_source` VARCHAR(20) NOT NULL COMMENT ''软件包来源''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- base_software_pn
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'base_software_pn') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `base_software_pn` VARCHAR(50) NOT NULL COMMENT ''基础软件零件号''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- package_adaptive_level
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'package_adaptive_level') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `package_adaptive_level` SMALLINT NOT NULL COMMENT ''软件包适配级别：1-基础版本及以下，2-基础版本及以上，3-与基础版本一致''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- adaptive_assembly_pn
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'adaptive_assembly_pn') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `adaptive_assembly_pn` VARCHAR(50) NOT NULL COMMENT ''适配的总成零件号''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- release_date
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'release_date') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `release_date` TIMESTAMP NOT NULL COMMENT ''发布日期''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- estimated_install_time
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'estimated_install_time') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `estimated_install_time` INT DEFAULT NULL COMMENT ''预计升级时间（分钟）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ota
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'ota') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `ota` TINYINT NOT NULL COMMENT ''是否是OTA包''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- description
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'description') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT ''备注''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- create_time
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'create_time') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- create_by
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'create_by') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `create_by` VARCHAR(64) DEFAULT NULL COMMENT ''创建者''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- modify_time
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'modify_time') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''修改时间''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- modify_by
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'modify_by') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `modify_by` VARCHAR(64) DEFAULT NULL COMMENT ''修改者''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add indexes if missing
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND INDEX_NAME = 'idx_device_code') = 0,
    'ALTER TABLE `tb_software_package` ADD INDEX `idx_device_code` (`device_code`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND INDEX_NAME = 'idx_software_pn') = 0,
    'ALTER TABLE `tb_software_package` ADD INDEX `idx_software_pn` (`software_pn`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
