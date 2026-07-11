-- Fix: V1 tables may be missing row_valid and row_version columns.
-- This happens when tables existed before Flyway V1 migration ran,
-- causing CREATE TABLE IF NOT EXISTS to skip table creation.

-- tb_software_package
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'row_valid') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `row_valid` TINYINT DEFAULT 1 COMMENT ''记录是否有效''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_package' AND COLUMN_NAME = 'row_version') = 0,
    'ALTER TABLE `tb_software_package` ADD COLUMN `row_version` INT DEFAULT 1 COMMENT ''记录版本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_software_build_version
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_build_version' AND COLUMN_NAME = 'row_valid') = 0,
    'ALTER TABLE `tb_software_build_version` ADD COLUMN `row_valid` TINYINT DEFAULT 1 COMMENT ''记录是否有效''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_build_version' AND COLUMN_NAME = 'row_version') = 0,
    'ALTER TABLE `tb_software_build_version` ADD COLUMN `row_version` INT DEFAULT 1 COMMENT ''记录版本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_software_build_version_package
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_build_version_package' AND COLUMN_NAME = 'row_valid') = 0,
    'ALTER TABLE `tb_software_build_version_package` ADD COLUMN `row_valid` TINYINT DEFAULT 1 COMMENT ''记录是否有效''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_build_version_package' AND COLUMN_NAME = 'row_version') = 0,
    'ALTER TABLE `tb_software_build_version_package` ADD COLUMN `row_version` INT DEFAULT 1 COMMENT ''记录版本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_software_build_version_dependency
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_build_version_dependency' AND COLUMN_NAME = 'row_valid') = 0,
    'ALTER TABLE `tb_software_build_version_dependency` ADD COLUMN `row_valid` TINYINT DEFAULT 1 COMMENT ''记录是否有效''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_software_build_version_dependency' AND COLUMN_NAME = 'row_version') = 0,
    'ALTER TABLE `tb_software_build_version_dependency` ADD COLUMN `row_version` INT DEFAULT 1 COMMENT ''记录版本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_compatible_pn
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_compatible_pn' AND COLUMN_NAME = 'row_valid') = 0,
    'ALTER TABLE `tb_compatible_pn` ADD COLUMN `row_valid` TINYINT DEFAULT 1 COMMENT ''记录是否有效''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_compatible_pn' AND COLUMN_NAME = 'row_version') = 0,
    'ALTER TABLE `tb_compatible_pn` ADD COLUMN `row_version` INT DEFAULT 1 COMMENT ''记录版本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_data_sync_record
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_data_sync_record' AND COLUMN_NAME = 'row_valid') = 0,
    'ALTER TABLE `tb_data_sync_record` ADD COLUMN `row_valid` TINYINT DEFAULT 1 COMMENT ''记录是否有效''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_data_sync_record' AND COLUMN_NAME = 'row_version') = 0,
    'ALTER TABLE `tb_data_sync_record` ADD COLUMN `row_version` INT DEFAULT 1 COMMENT ''记录版本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_vehicle_part
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_vehicle_part' AND COLUMN_NAME = 'row_valid') = 0,
    'ALTER TABLE `tb_vehicle_part` ADD COLUMN `row_valid` TINYINT DEFAULT 1 COMMENT ''记录是否有效''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_vehicle_part' AND COLUMN_NAME = 'row_version') = 0,
    'ALTER TABLE `tb_vehicle_part` ADD COLUMN `row_version` INT DEFAULT 1 COMMENT ''记录版本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_vehicle_part_history
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_vehicle_part_history' AND COLUMN_NAME = 'row_valid') = 0,
    'ALTER TABLE `tb_vehicle_part_history` ADD COLUMN `row_valid` TINYINT DEFAULT 1 COMMENT ''记录是否有效''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_vehicle_part_history' AND COLUMN_NAME = 'row_version') = 0,
    'ALTER TABLE `tb_vehicle_part_history` ADD COLUMN `row_version` INT DEFAULT 1 COMMENT ''记录版本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
