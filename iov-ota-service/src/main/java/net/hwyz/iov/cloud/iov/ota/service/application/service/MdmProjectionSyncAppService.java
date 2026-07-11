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
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselineItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselinePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SwinDefinitionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SwinManagedSystemPo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

            String baselineCode = payload.path("code").asText("");
            String baselineStatus = payload.path("baselineStatus").asText("");

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

            upsertBaselineProjection(payload);
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

        BaselinePo po = existing != null ? existing : new BaselinePo();
        po.setBaselineCode(baselineCode);
        po.setName(payload.path("name").asText(null));
        po.setAnchorType(payload.path("anchorType").asText(""));
        po.setAnchorCode(payload.path("anchorCode").asText(""));
        po.setBaselineVersion(payload.path("baselineVersion").asText(null));
        po.setBaselineStatus(payload.path("baselineStatus").asText(""));
        po.setSource("MDM");
        po.setSyncTime(Date.from(Instant.now()));

        if (existing != null) {
            baselineMapper.updateById(po);
        } else {
            baselineMapper.insert(po);
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

        for (JsonNode item : itemsNode) {
            BaselineItemPo itemPo = new BaselineItemPo();
            itemPo.setBaselineCode(baselineCode);
            itemPo.setPartCode(item.path("partCode").asText(""));
            itemPo.setVehicleNodeCode(item.path("vehicleNodeCode").asText(null));
            itemPo.setRemark(item.path("remark").asText(null));
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

        SwinDefinitionPo po = existing != null ? existing : new SwinDefinitionPo();
        po.setSwinCode(swinCode);
        po.setSchemeCode(payload.path("schemeCode").asText(""));
        po.setTypeRefType(payload.path("typeRefType").asText(null));
        po.setTypeRefCode(payload.path("typeRefCode").asText(null));
        po.setName(payload.path("name").asText(null));
        po.setStatus(payload.path("status").asText(""));
        po.setSyncTime(Date.from(Instant.now()));

        if (existing != null) {
            swinDefinitionMapper.updateById(po);
        } else {
            swinDefinitionMapper.insert(po);
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

        for (JsonNode ms : managedSystemsNode) {
            SwinManagedSystemPo msPo = new SwinManagedSystemPo();
            msPo.setSwinCode(swinCode);
            msPo.setVehicleNodeCode(ms.path("vehicleNodeCode").asText(""));
            msPo.setIsTypeApprovalRelevant(ms.path("isTypeApprovalRelevant").asBoolean(false));
            msPo.setApprovedSoftwareBaseline(ms.path("approvedSoftwareBaseline").asText(null));
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
}
