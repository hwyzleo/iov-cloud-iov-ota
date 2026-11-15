package net.hwyz.iov.cloud.iov.ota.service.infrastructure.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ManifestDigestBackfillService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 包清单摘要存量回填启动任务（CR-024 §4.28.3.4）。
 *
 * <p>默认关闭（{@code ota.permit.manifest-digest.backfill.enabled=false}）：由运维在窗口期显式开启
 * 一次完成回填，避免每次启动扫描全表。回填幂等（只填充空值），失败不阻断启动。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.permit.manifest-digest.backfill.enabled", havingValue = "true")
public class ManifestDigestBackfillRunner implements ApplicationRunner {

    private final ManifestDigestBackfillService backfillService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int updated = backfillService.backfillAll();
            log.info("包清单摘要存量回填启动任务完成：回填[{}]条", updated);
        } catch (RuntimeException e) {
            // 回填失败不得阻断服务启动；未回填任务按安装许可 fail-open 继续运行
            log.error("包清单摘要存量回填启动任务失败（不影响启动）", e);
        }
    }
}
