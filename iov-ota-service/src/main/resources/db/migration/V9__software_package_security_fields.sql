-- DSN-CR-003: 软件包字段规则与安全校验字段
-- 1. 新增完整性/来源校验字段
ALTER TABLE `tb_software_package`
    ADD COLUMN `package_sha256`   VARCHAR(255) DEFAULT NULL COMMENT '软件包SHA-256（权威完整性校验）' AFTER `package_md5`,
    ADD COLUMN `package_signature` TEXT         DEFAULT NULL COMMENT '软件包数字签名' AFTER `package_sha256`,
    ADD COLUMN `sign_algo`        VARCHAR(20)  DEFAULT NULL COMMENT '签名算法（RSA/ECDSA/SM2）' AFTER `package_signature`,
    ADD COLUMN `signer_cert_id`   VARCHAR(255) DEFAULT NULL COMMENT '签名证书标识（引用KMS/PKI）' AFTER `sign_algo`,
    ADD COLUMN `base_software_ver` VARCHAR(50) DEFAULT NULL COMMENT '基础软件版本（仅DELTA，与base_software_pn成对）' AFTER `base_software_pn`;

-- 2. 放宽约束：base_software_pn / package_adaptive_level / adaptive_assembly_pn 仅DELTA必填，FULL可空
ALTER TABLE `tb_software_package`
    MODIFY COLUMN `base_software_pn`       VARCHAR(50) DEFAULT NULL COMMENT '基础软件零件号（仅DELTA必填）',
    MODIFY COLUMN `package_adaptive_level` SMALLINT    DEFAULT NULL COMMENT '软件包适配级别：1-LE,2-GE,3-EQ（DELTA必填，FULL默认LE）',
    MODIFY COLUMN `adaptive_assembly_pn`   VARCHAR(50) DEFAULT NULL COMMENT '适配的硬件总成零件号';

-- 3. 为 package_code 添加唯一索引（系统生成·唯一·不可变）
ALTER TABLE `tb_software_package`
    ADD UNIQUE INDEX `uk_package_code` (`package_code`);
