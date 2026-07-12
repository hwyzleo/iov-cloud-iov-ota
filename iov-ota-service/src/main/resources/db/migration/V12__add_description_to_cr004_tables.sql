-- 修复: V10 建表时遗漏了 BasePo 约定的 description 列

ALTER TABLE `tb_software_build_version_adaptation`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `sort`;

ALTER TABLE `tb_software_build_version_test_report`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `tested_by`;
