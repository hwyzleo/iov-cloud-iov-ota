package net.hwyz.iov.cloud.iov.ota.service.infrastructure.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mdm.api.service.MdmTypeApprovalBaselineService;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.TypeApprovalBaselineResponse;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SwinDefinitionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SwinDefinitionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TypeApprovalBaselineItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TypeApprovalBaselinePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TypeApprovalBaselineMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TypeApprovalBaselineItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * TA基线投影Bootstrap服务
 * <p>
 * 应用启动时遍历OTA本地SWIN投影中的ACTIVE SWIN，
 * 逐个调用MDM bySwin接口拉取TA基线全量数据。
 * </p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.bootstrap.typeApprovalBaseline.enabled", havingValue = "true", matchIfMissing = true)
public class TypeApprovalBaselineBootstrap implements CommandLineRunner {

    private final SwinDefinitionMapper swinDefinitionMapper;
    private final TypeApprovalBaselineMapper typeApprovalBaselineMapper;
    private final TypeApprovalBaselineItemMapper typeApprovalBaselineItemMapper;
    private final MdmTypeApprovalBaselineService mdmTypeApprovalBaselineService;

    @Override
    public void run(String... args) {
        log.info("开始Bootstrap TA基线投影全量拉取...");
        try {
            bootstrapTypeApprovalBaselines();
            log.info("Bootstrap TA基线投影全量拉取完成");
        } catch (Exception e) {
            log.error("Bootstrap TA基线投影全量拉取失败: {}", e.getMessage(), e);
        }
    }

    private void bootstrapTypeApprovalBaselines() {
        // 查询OTA本地SWIN投影中的所有ACTIVE SWIN
        LambdaQueryWrapper<SwinDefinitionPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwinDefinitionPo::getStatus, "ACTIVE");
        List<SwinDefinitionPo> activeSwins = swinDefinitionMapper.selectList(wrapper);

        log.info("发现 {} 个ACTIVE SWIN，开始拉取TA基线", activeSwins.size());

        int successCount = 0;
        int failCount = 0;

        for (SwinDefinitionPo swin : activeSwins) {
            try {
                List<TypeApprovalBaselineResponse> baselines = mdmTypeApprovalBaselineService.listBySwinCode(swin.getSwinCode());
                if (baselines != null && !baselines.isEmpty()) {
                    for (TypeApprovalBaselineResponse baseline : baselines) {
                        upsertTypeApprovalBaseline(baseline);
                    }
                    successCount++;
                    log.debug("成功拉取SWIN[{}]的{}条TA基线", swin.getSwinCode(), baselines.size());
                }
            } catch (Exception e) {
                failCount++;
                log.warn("拉取SWIN[{}]的TA基线失败: {}", swin.getSwinCode(), e.getMessage());
            }
        }

        log.info("Bootstrap TA基线完成: 成功={}, 失败={}, 总数={}", successCount, failCount, activeSwins.size());
    }

    private void upsertTypeApprovalBaseline(TypeApprovalBaselineResponse response) {
        String taBaselineCode = response.getTaBaselineCode();

        // 幂等检查
        LambdaQueryWrapper<TypeApprovalBaselinePo> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(TypeApprovalBaselinePo::getTaBaselineCode, taBaselineCode);
        TypeApprovalBaselinePo existing = typeApprovalBaselineMapper.selectOne(existWrapper);

        Integer upVersion = response.getVersion();
        if (existing != null && existing.getUpVersion() != null && upVersion != null
                && existing.getUpVersion() >= upVersion.longValue()) {
            log.debug("跳过旧版本TA基线: code={}, existingVersion={}, incomingVersion={}",
                    taBaselineCode, existing.getUpVersion(), upVersion);
            return;
        }

        Date now = Date.from(Instant.now());
        TypeApprovalBaselinePo po = existing != null ? existing : new TypeApprovalBaselinePo();
        po.setTaBaselineCode(taBaselineCode);
        po.setSwinCode(response.getSwinCode());
        po.setAnchorType(response.getAnchorType());
        po.setAnchorCode(response.getAnchorCode());
        po.setStatus(response.getStatus());
        po.setProjectionDigest(response.getProjectionDigest());
        po.setEffectiveFrom(response.getEffectiveFrom());
        po.setSourceBaselineScope(response.getSourceBaselineScope());
        po.setUpVersion(upVersion != null ? upVersion.longValue() : null);
        po.setSyncTime(now);

        if (existing != null) {
            typeApprovalBaselineMapper.updateById(po);
        } else {
            po.setCreateTime(now);
            po.setModifyTime(now);
            typeApprovalBaselineMapper.insert(po);
        }

        // 保存明细：先删后插
        if (response.getItems() != null && !response.getItems().isEmpty()) {
            LambdaQueryWrapper<TypeApprovalBaselineItemPo> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(TypeApprovalBaselineItemPo::getTaBaselineCode, taBaselineCode);
            typeApprovalBaselineItemMapper.delete(deleteWrapper);

            for (var item : response.getItems()) {
                TypeApprovalBaselineItemPo itemPo = new TypeApprovalBaselineItemPo();
                itemPo.setTaBaselineCode(taBaselineCode);
                itemPo.setVehicleNodeCode(item.getVehicleNodeCode());
                itemPo.setPartCode(item.getPartCode());
                itemPo.setApprovedVersion(item.getApprovedVersion());
                itemPo.setSourceBaselineCode(item.getSourceBaselineCode());
                itemPo.setCreateTime(now);
                itemPo.setModifyTime(now);
                typeApprovalBaselineItemMapper.insert(itemPo);
            }
        }
    }
}
