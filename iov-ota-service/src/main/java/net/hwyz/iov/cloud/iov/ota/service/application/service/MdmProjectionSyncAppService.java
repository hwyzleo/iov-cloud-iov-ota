package net.hwyz.iov.cloud.iov.ota.service.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.Baseline;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.BaselineItem;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SwinDefinition;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SwinManagedSystem;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.BaselineItemRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.BaselineRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SwinDefinitionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SwinManagedSystemRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.BaselineItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.BaselineMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SwinDefinitionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SwinManagedSystemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TypeApprovalBaselineMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TypeApprovalBaselineItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselineItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselinePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SwinDefinitionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SwinManagedSystemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TypeApprovalBaselinePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TypeApprovalBaselineItemPo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

/**
 * MDM主数据投影同步应用服务
 * <p>
 * 消费MDM SoftwareBaseline和SWIN事件，幂等upsert/delete本地投影表。
 * 仅消费 baseline_status=RELEASED 和 SWIN status=ACTIVE。
 * </p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmProjectionSyncAppService {

    private final BaselineMapper baselineMapper;
    private final BaselineItemMapper baselineItemMapper;
    private final SwinDefinitionMapper swinDefinitionMapper;
    private final SwinManagedSystemMapper swinManagedSystemMapper;
    private final TypeApprovalBaselineMapper typeApprovalBaselineMapper;
    private final TypeApprovalBaselineItemMapper typeApprovalBaselineItemMapper;
    private final ObjectMapper objectMapper;

    /**
     * 处理SoftwareBaseline事件
     * <p>
     * 事件payload为SoftwareBaseline对象JSON，含code/name/anchorType/anchorCode/baselineVersion/baselineStatus/items。
     * 仅消费 baseline_status=RELEASED；SUPERSEDED/DELETED 时删除投影。
     * </p>
     */
    @Transactional
    public void handleSoftwareBaselineEvent(String messageJson) {
        try {
            JsonNode root = objectMapper.readTree(messageJson);
            String eventType = root.path("eventType").asText("");
            JsonNode payload = root.path("payload");

            JsonNode dataNode = (payload.isMissingNode() || payload.isNull()) ? root : payload;

            String baselineCode = dataNode.path("code").asText("");
            String baselineStatus = dataNode.path("baselineStatus").asText("");

            log.info("处理SoftwareBaseline事件: eventType={}, code={}, status={}", eventType, baselineCode, baselineStatus);

            if ("SoftwareBaselineDeleted".equals(eventType)) {
                deleteBaselineProjection(baselineCode);
                return;
            }

            if (!"RELEASED".equals(baselineStatus)) {
                log.info("跳过非RELEASED基线: code={}, status={}", baselineCode, baselineStatus);
                deleteBaselineProjection(baselineCode);
                return;
            }

            upsertBaselineProjection(dataNode);
        } catch (Exception e) {
            log.error("处理SoftwareBaseline事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理SWIN定义事件
     * <p>
     * 仅消费 status=ACTIVE；INACTIVE 时删除投影。
     * </p>
     */
    @Transactional
    public void handleSwinDefinitionEvent(String messageJson) {
        try {
            JsonNode root = objectMapper.readTree(messageJson);
            String eventType = root.path("eventType").asText("");
            JsonNode payload = root.path("payload");

            if (payload.isMissingNode() || payload.isNull()) {
                String swinCode = root.path("swinCode").asText("");
                if ("SwinDefinitionDeleted".equals(eventType)) {
                    deleteSwinProjection(swinCode);
                    return;
                }
            }

            String swinCode = payload.path("swinCode").asText(root.path("swinCode").asText(""));
            String status = payload.path("status").asText(root.path("status").asText(""));

            log.info("处理SWIN定义事件: eventType={}, swinCode={}, status={}", eventType, swinCode, status);

            if ("SwinDefinitionDeleted".equals(eventType)) {
                deleteSwinProjection(swinCode);
                return;
            }

            if (!"ACTIVE".equals(status)) {
                log.info("跳过非ACTIVE SWIN: swinCode={}, status={}", swinCode, status);
                deleteSwinProjection(swinCode);
                return;
            }

            upsertSwinProjection(payload);
        } catch (Exception e) {
            log.error("处理SWIN定义事件失败: {}", e.getMessage(), e);
        }
    }

    private void upsertBaselineProjection(JsonNode payload) {
        String baselineCode = payload.path("code").asText("");

        LambdaQueryWrapper<BaselinePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaselinePo::getBaselineCode, baselineCode);
        BaselinePo existing = baselineMapper.selectOne(wrapper);

        Date now = Date.from(Instant.now());
        BaselinePo po = existing != null ? existing : new BaselinePo();
        po.setBaselineCode(baselineCode);
        po.setName(payload.path("name").asText(null));
        po.setAnchorType(payload.path("anchorType").asText(""));
        po.setAnchorCode(payload.path("anchorCode").asText(""));
        po.setBaselineVersion(payload.path("baselineVersion").asText(null));
        po.setBaselineStatus(payload.path("baselineStatus").asText(""));
        po.setSource("MDM");
        po.setSyncTime(now);

        if (existing != null) {
            baselineMapper.updateById(po);
        } else {
            po.setCreateTime(now);
            po.setModifyTime(now);
            try {
                baselineMapper.insert(po);
            } catch (DuplicateKeyException e) {
                log.warn("并发插入基线投影冲突，回退恢复更新: code={}", baselineCode);
                baselineMapper.restoreAndUpdate(po);
            }
        }

        upsertBaselineItems(baselineCode, payload.path("items"));
    }

    private void upsertBaselineItems(String baselineCode, JsonNode itemsNode) {
        if (itemsNode.isMissingNode() || !itemsNode.isArray()) {
            return;
        }

        LambdaQueryWrapper<BaselineItemPo> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(BaselineItemPo::getBaselineCode, baselineCode);
        baselineItemMapper.delete(deleteWrapper);

        Date now = Date.from(Instant.now());
        for (JsonNode item : itemsNode) {
            BaselineItemPo itemPo = new BaselineItemPo();
            itemPo.setBaselineCode(baselineCode);
            itemPo.setPartCode(item.path("partCode").asText(""));
            itemPo.setVehicleNodeCode(item.path("vehicleNodeCode").asText(null));
            itemPo.setRemark(item.path("remark").asText(null));
            itemPo.setCreateTime(now);
            itemPo.setModifyTime(now);
            baselineItemMapper.insert(itemPo);
        }
    }

    private void deleteBaselineProjection(String baselineCode) {
        LambdaQueryWrapper<BaselineItemPo> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(BaselineItemPo::getBaselineCode, baselineCode);
        baselineItemMapper.delete(itemWrapper);

        LambdaQueryWrapper<BaselinePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaselinePo::getBaselineCode, baselineCode);
        baselineMapper.delete(wrapper);
        log.info("已删除基线投影: code={}", baselineCode);
    }

    private void upsertSwinProjection(JsonNode payload) {
        String swinCode = payload.path("swinCode").asText("");

        LambdaQueryWrapper<SwinDefinitionPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwinDefinitionPo::getSwinCode, swinCode);
        SwinDefinitionPo existing = swinDefinitionMapper.selectOne(wrapper);

        Date now = Date.from(Instant.now());
        SwinDefinitionPo po = existing != null ? existing : new SwinDefinitionPo();
        po.setSwinCode(swinCode);
        po.setSchemeCode(payload.path("schemeCode").asText(""));
        po.setTypeRefType(payload.path("typeRefType").asText(null));
        po.setTypeRefCode(payload.path("typeRefCode").asText(null));
        po.setName(payload.path("name").asText(null));
        po.setStatus(payload.path("status").asText(""));
        po.setSyncTime(now);

        if (existing != null) {
            swinDefinitionMapper.updateById(po);
        } else {
            po.setCreateTime(now);
            po.setModifyTime(now);
            try {
                swinDefinitionMapper.insert(po);
            } catch (DuplicateKeyException e) {
                log.warn("并发插入SWIN投影冲突，回退恢复更新: swinCode={}", swinCode);
                swinDefinitionMapper.restoreAndUpdate(po);
            }
        }

        JsonNode managedSystems = payload.path("managedSystems");
        if (managedSystems.isArray()) {
            upsertSwinManagedSystems(swinCode, managedSystems);
        }
    }

    private void upsertSwinManagedSystems(String swinCode, JsonNode managedSystemsNode) {
        LambdaQueryWrapper<SwinManagedSystemPo> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SwinManagedSystemPo::getSwinCode, swinCode);
        swinManagedSystemMapper.delete(deleteWrapper);

        Date now = Date.from(Instant.now());
        for (JsonNode ms : managedSystemsNode) {
            SwinManagedSystemPo msPo = new SwinManagedSystemPo();
            msPo.setSwinCode(swinCode);
            msPo.setVehicleNodeCode(ms.path("vehicleNodeCode").asText(""));
            msPo.setIsTypeApprovalRelevant(ms.path("isTypeApprovalRelevant").asBoolean(false));
            msPo.setApprovedSoftwareBaseline(ms.path("approvedSoftwareBaseline").asText(null));
            msPo.setCreateTime(now);
            msPo.setModifyTime(now);
            swinManagedSystemMapper.insert(msPo);
        }
    }

    private void deleteSwinProjection(String swinCode) {
        LambdaQueryWrapper<SwinManagedSystemPo> msWrapper = new LambdaQueryWrapper<>();
        msWrapper.eq(SwinManagedSystemPo::getSwinCode, swinCode);
        swinManagedSystemMapper.delete(msWrapper);

        LambdaQueryWrapper<SwinDefinitionPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwinDefinitionPo::getSwinCode, swinCode);
        swinDefinitionMapper.delete(wrapper);
        log.info("已删除SWIN投影: swinCode={}", swinCode);
    }

    /**
     * 处理TypeApprovalBaseline事件
     * <p>
     * 仅消费 status=RELEASED/FROZEN；其他状态时删除投影。
     * 按 ta_baseline_code + up_version 幂等。
     * </p>
     */
    @Transactional
    public void handleTypeApprovalBaselineEvent(String messageJson) {
        try {
            JsonNode root = objectMapper.readTree(messageJson);
            String eventType = root.path("eventType").asText("");
            JsonNode payload = root.path("payload");

            JsonNode dataNode = (payload.isMissingNode() || payload.isNull()) ? root : payload;

            String taBaselineCode = dataNode.path("code").asText("");
            String status = dataNode.path("status").asText("");

            log.info("处理TypeApprovalBaseline事件: eventType={}, code={}, status={}", eventType, taBaselineCode, status);

            if ("TypeApprovalBaselineDeleted".equals(eventType)) {
                deleteTypeApprovalBaselineProjection(taBaselineCode);
                return;
            }

            if (!"RELEASED".equals(status) && !"FROZEN".equals(status)) {
                log.info("跳过非RELEASED/FROZEN TA基线: code={}, status={}", taBaselineCode, status);
                deleteTypeApprovalBaselineProjection(taBaselineCode);
                return;
            }

            upsertTypeApprovalBaselineProjection(dataNode);
        } catch (Exception e) {
            log.error("处理TypeApprovalBaseline事件失败: {}", e.getMessage(), e);
        }
    }

    private void upsertTypeApprovalBaselineProjection(JsonNode payload) {
        String taBaselineCode = payload.path("code").asText("");
        Long upVersion = payload.path("version").asLong(0);

        // 幂等检查：如果已存在且上游版本号更大，则跳过
        LambdaQueryWrapper<TypeApprovalBaselinePo> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(TypeApprovalBaselinePo::getTaBaselineCode, taBaselineCode);
        TypeApprovalBaselinePo existing = typeApprovalBaselineMapper.selectOne(existWrapper);
        if (existing != null && existing.getUpVersion() != null && existing.getUpVersion() >= upVersion) {
            log.info("跳过旧版本TA基线: code={}, existingVersion={}, incomingVersion={}", taBaselineCode, existing.getUpVersion(), upVersion);
            return;
        }

        Date now = Date.from(Instant.now());
        TypeApprovalBaselinePo po = existing != null ? existing : new TypeApprovalBaselinePo();
        po.setTaBaselineCode(taBaselineCode);
        po.setSwinCode(payload.path("swinCode").asText(""));
        po.setAnchorType(payload.path("anchorType").asText(""));
        po.setAnchorCode(payload.path("anchorCode").asText(""));
        po.setStatus(payload.path("status").asText(""));
        po.setProjectionDigest(payload.path("projectionDigest").asText(null));
        po.setEffectiveFrom(payload.path("effectiveFrom").asText(null) != null ?
                new Date(payload.path("effectiveFrom").asLong(0)) : null);
        po.setSourceBaselineScope(payload.path("sourceBaselineScope").asText(null));
        po.setUpVersion(upVersion);
        po.setSyncTime(now);

        if (existing != null) {
            typeApprovalBaselineMapper.updateById(po);
        } else {
            po.setCreateTime(now);
            po.setModifyTime(now);
            try {
                typeApprovalBaselineMapper.insert(po);
            } catch (DuplicateKeyException e) {
                log.warn("并发插入TA基线投影冲突，回退恢复更新: taBaselineCode={}", taBaselineCode);
                typeApprovalBaselineMapper.restoreAndUpdate(po);
            }
        }

        upsertTypeApprovalBaselineItems(taBaselineCode, payload.path("items"));
    }

    private void upsertTypeApprovalBaselineItems(String taBaselineCode, JsonNode itemsNode) {
        if (itemsNode.isMissingNode() || !itemsNode.isArray()) {
            return;
        }

        LambdaQueryWrapper<TypeApprovalBaselineItemPo> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(TypeApprovalBaselineItemPo::getTaBaselineCode, taBaselineCode);
        typeApprovalBaselineItemMapper.delete(deleteWrapper);

        Date now = Date.from(Instant.now());
        for (JsonNode item : itemsNode) {
            TypeApprovalBaselineItemPo itemPo = new TypeApprovalBaselineItemPo();
            itemPo.setTaBaselineCode(taBaselineCode);
            itemPo.setVehicleNodeCode(item.path("vehicleNodeCode").asText(""));
            itemPo.setPartCode(item.path("partCode").asText(""));
            itemPo.setApprovedVersion(item.path("approvedVersion").asText(null));
            itemPo.setSourceBaselineCode(item.path("sourceBaselineCode").asText(null));
            itemPo.setCreateTime(now);
            itemPo.setModifyTime(now);
            typeApprovalBaselineItemMapper.insert(itemPo);
        }
    }

    private void deleteTypeApprovalBaselineProjection(String taBaselineCode) {
        LambdaQueryWrapper<TypeApprovalBaselineItemPo> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(TypeApprovalBaselineItemPo::getTaBaselineCode, taBaselineCode);
        typeApprovalBaselineItemMapper.delete(itemWrapper);

        LambdaQueryWrapper<TypeApprovalBaselinePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TypeApprovalBaselinePo::getTaBaselineCode, taBaselineCode);
        typeApprovalBaselineMapper.delete(wrapper);
        log.info("已删除TA基线投影: taBaselineCode={}", taBaselineCode);
    }
}
