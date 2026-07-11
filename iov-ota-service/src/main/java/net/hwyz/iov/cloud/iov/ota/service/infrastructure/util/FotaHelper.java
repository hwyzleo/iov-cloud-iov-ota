package net.hwyz.iov.cloud.iov.ota.service.infrastructure.util;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.HexUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdPartService;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwarePackageExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.BaselineNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ConfigWordVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.DeviceInfoVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackageVo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.BaselineItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselineItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FOTA升级辅助类
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FotaHelper {

    private final VmdPartService vmdPartService;
    private final BaselineItemMapper baselineItemMapper;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;
    private final SoftwareBuildVersionPackageMapper softwareBuildVersionPackageMapper;

    /**
     * 软件零件映射
     */
    private static final Map<String, Set<String>> deviceSoftwarePartMap = new ConcurrentHashMap<>();

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        log.info("初始化软件零件工具类");
        loadSoftwarePart();
    }

    /**
     * 判断是否是升级软件零件
     *
     * @param deviceCode 设备编号
     * @param softwarePn 软件零件号
     * @return 是否是升级软件零件
     */
    public boolean isOtaSoftwarePart(String deviceCode, String softwarePn) {
        Set<String> deviceSoftwarePart = deviceSoftwarePartMap.get(deviceCode);
        if (deviceSoftwarePart == null) {
            return false;
        }
        return deviceSoftwarePart.contains(softwarePn);
    }

    /**
     * 判断设备是否与基线对齐
     * <p>
     * 从本地 MDM 投影表 tb_baseline_item 读取基线应装零件清单，
     * 校验车辆各 OTA 设备的 deviceCode 与 softwarePn 是否与基线一致。
     * 版本对齐校验由活动 tb_activity_target_version 在 check 主链路中完成（DSN-CR-002 §4.10）。
     *
     * @param baselineCode   基线编号
     * @param deviceInfoList 设备信息列表
     * @return 是否与基线对齐
     */
    public boolean isBaselineAlignment(String baselineCode, List<DeviceInfoVo> deviceInfoList) {
        LambdaQueryWrapper<BaselineItemPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaselineItemPo::getBaselineCode, baselineCode);
        List<BaselineItemPo> baselineItemList = baselineItemMapper.selectList(wrapper);
        if (baselineItemList == null || baselineItemList.isEmpty()) {
            throw new BaselineNotExistException(baselineCode);
        }
        Map<String, BaselineItemPo> baselineItemMap = baselineItemList.stream()
                .collect(Collectors.toMap(BaselineItemPo::getVehicleNodeCode, item -> item, (a, b) -> a));
        for (DeviceInfoVo deviceInfo : deviceInfoList) {
            if (!isOtaSoftwarePart(deviceInfo.getDeviceCode(), deviceInfo.getSoftwarePn())) {
                continue;
            }
            BaselineItemPo baselineItem = baselineItemMap.get(deviceInfo.getDeviceCode());
            if (baselineItem == null || !deviceInfo.getSoftwarePn().equals(baselineItem.getPartCode())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取软件内部版本包
     *
     * @param deviceCode       设备代码
     * @param softwarePn       软件零件号
     * @param softwareBuildVer 软件内部版本号
     * @return 软件内部版本包
     */
    public List<SoftwarePackageVo> getSoftwareBuildVersionPackages(String deviceCode, String softwarePn, String softwareBuildVer) {
        SoftwareBuildVersionPo softwareBuildVersion = softwareBuildVersionAppService.getSoftwareBuildVersionByDeviceCodeAndSoftwarePnAndVersion(deviceCode, softwarePn, softwareBuildVer);
        if (softwareBuildVersion == null) {
            return List.of();
        }
        List<SoftwarePackagePo> packageList = softwareBuildVersionPackageMapper.selectPoBySoftwareBuildVersionId(softwareBuildVersion.getId())
            .stream()
            .map(pkgRel -> {
                SoftwarePackagePo pkg = new SoftwarePackagePo();
                pkg.setId(pkgRel.getSoftwarePackageId());
                return pkg;
            })
            .toList();
        return SoftwarePackageExServiceAssembler.INSTANCE.toVoList(SoftwarePackageExServiceAssembler.INSTANCE.fromPoList(packageList));
    }

    /**
     * 配置字转换成字符串
     *
     * @param originConfigWord 原始配置字
     * @param configWordList   配置字列表
     * @return 转换后的字符串
     */
    public String configWordToStr(String originConfigWord, List<ConfigWordVo> configWordList) {
        byte[] origin = HexUtil.decodeHex(originConfigWord);
        for (ConfigWordVo configWord : configWordList) {
            String binaryValue = configWord.getConfigWordValue();
            int startByte = configWord.getStartByte();
            int startBit = configWord.getStartBit();
            if (binaryValue == null || binaryValue.isEmpty()) {
                continue;
            }
            if (startByte < 0 || startByte >= origin.length) {
                log.warn("起始字节索引超出范围：" + startByte + "，数组长度：" + origin.length);
                continue;
            }
            if (startBit < 0 || startBit > 7) {
                log.warn("起始位偏移非法（需0-7）：" + startBit);
                continue;
            }
            for (int i = 0; i < binaryValue.length(); i++) {
                int targetByteIndex = startByte + (startBit + i) / 8;
                int bitOffset = (startBit + i) % 8;
                char bitChar = binaryValue.charAt(i);
                int bitValue = (bitChar == '1') ? 1 : 0;

                if (targetByteIndex < 0 || targetByteIndex >= origin.length) {
                    log.warn("替换位超出字节数组范围：字节索引=" + targetByteIndex + "，数组长度=" + origin.length);
                    continue;
                }
                origin[targetByteIndex] = (byte) (origin[targetByteIndex] & ~(1 << bitOffset));
                origin[targetByteIndex] = (byte) (origin[targetByteIndex] | (bitValue << bitOffset));
            }
        }
        return HexUtil.encodeHexStr(origin);
    }

    /**
     * 加载所有软件零件
     */
    private void loadSoftwarePart() {
        log.info("加载所有软件零件");
//        vmdPartService.listAllFota(true).forEach(part -> {
//            if (!deviceSoftwarePartMap.containsKey(part.getDeviceCode())) {
//                deviceSoftwarePartMap.put(part.getDeviceCode(), new ConcurrentHashSet<>());
//            }
//            deviceSoftwarePartMap.get(part.getDeviceCode()).add(part.getPn());
//        });
    }

}
