package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ConsentCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleTaskStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskConsentMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskConsentPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

/**
 * 用户授权应用服务（CR-012 §5.3、US-077）
 *
 * <p>授权受理、凭据生成、撤回和重新授权。
 * accepted 与 effectiveConsentStatus 分离。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentAppService {

    private final VehicleTaskRepository vehicleTaskRepository;
    private final VehicleTaskConsentMapper vehicleTaskConsentMapper;

    /**
     * 处理用户授权。
     *
     * @param cmd 授权命令
     * @return 授权结果
     */
    @Transactional
    public ConsentResult handleConsent(ConsentCmd cmd) {
        log.info("车辆[{}]授权，车辆任务[{}]，动作[{}]", cmd.getVin(), cmd.getVehicleTaskId(), cmd.getAction());

        VehicleTask vt = vehicleTaskRepository.getById(VehicleTaskId.of(cmd.getVehicleTaskId()))
                .orElseThrow(() -> new VehicleTaskStateException("车辆任务[" + cmd.getVehicleTaskId() + "]不存在"));

        String receiptId = cmd.getConsentReceiptId() != null
                ? cmd.getConsentReceiptId()
                : "consent-" + UUID.randomUUID().toString().replace("-", "");

        ConsentState effectiveState;
        boolean accepted;
        boolean reconsentRequired = false;

        switch (cmd.getAction()) {
            case "GRANT" -> {
                vt.grantConsent(true);
                effectiveState = ConsentState.GRANTED;
                accepted = true;
            }
            case "DENY" -> {
                vt.denyConsent();
                effectiveState = ConsentState.DENIED;
                accepted = false;
            }
            case "REVOKE" -> {
                vt.revokeConsent();
                effectiveState = ConsentState.REVOKED;
                accepted = false;
            }
            default -> throw new VehicleTaskStateException("未知授权动作: " + cmd.getAction());
        }

        // 持久化授权凭据
        VehicleTaskConsentPo consentPo = VehicleTaskConsentPo.builder()
                .vehicleTaskId(cmd.getVehicleTaskId())
                .consentReceiptId(receiptId)
                .termsId(cmd.getTermsId())
                .termsHash(cmd.getTermsHash())
                .consentScopeDigest(cmd.getConsentScopeDigest())
                .consentState(effectiveState.getValue())
                .accepted(accepted ? 1 : 0)
                .effectiveState(effectiveState.getValue())
                .revokedTime("REVOKE".equals(cmd.getAction()) ? new Date() : null)
                .reconsentRequired(reconsentRequired ? 1 : 0)
                .build();
        vehicleTaskConsentMapper.insert(consentPo);

        vehicleTaskRepository.save(vt);

        return ConsentResult.builder()
                .consentReceiptId(receiptId)
                .consentState(effectiveState.getValue())
                .accepted(accepted)
                .effectiveConsentState(effectiveState.getValue())
                .reconsentRequired(reconsentRequired)
                .build();
    }
}
