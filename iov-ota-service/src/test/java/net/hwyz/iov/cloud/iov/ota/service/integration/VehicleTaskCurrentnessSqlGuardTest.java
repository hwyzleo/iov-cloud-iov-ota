package net.hwyz.iov.cloud.iov.ota.service.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 车辆任务「当前性」SQL 口径守卫（IOV-OTA-DSN-CR-025 §4.29.7）。
 *
 * <p>重置会为同一 (task_id, vin) 追加一条新的当前车辆任务，历史行保留 SUPERSEDED。
 * 若指标/统计/单行查询仍按行计数或缺少当前性过滤，将出现分母翻倍、
 * 单结果映射异常等回归。本测试以 SQL 文本断言锁定口径。</p>
 *
 * @author hwyz_leo
 */
@DisplayName("CR-025 当前车辆任务 SQL 口径守卫")
class VehicleTaskCurrentnessSqlGuardTest {

    private String read(String resource) throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "资源必须存在于 classpath：" + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 取出指定 statement 的 SQL 片段（以 id 开头到下一个 </select>/</update>/</insert> 结束）。 */
    private String statement(String xml, String id) {
        int idx = xml.indexOf("id=\"" + id + "\"");
        assertTrue(idx > 0, "未找到 statement：" + id);
        int end = xml.length();
        for (String tag : new String[]{"</select>", "</update>", "</insert>", "</delete>"}) {
            int e = xml.indexOf(tag, idx);
            if (e > 0 && e < end) {
                end = e;
            }
        }
        return xml.substring(idx, end);
    }

    @Test
    @DisplayName("指标计数按当前车辆任务（superseded_by IS NULL），不按行计数")
    void metric_counts_are_scoped_to_current_vehicle_task() throws IOException {
        String xml = read("mappers/TaskVehicleMapper.xml");
        for (String id : new String[]{"countAllByTaskId", "countByTaskIdAndVehicleTaskStatus",
                "countTimeoutExecutionByTaskId"}) {
            String s = statement(xml, id);
            assertTrue(s.toUpperCase().contains("SUPERSEDED_BY IS NULL"),
                    id + " 必须限定当前车辆任务，避免重置后分母翻倍");
        }
    }

    @Test
    @DisplayName("selectByTaskIdAndVin 取当前行并带排序与 LIMIT，避免单结果映射异常")
    void select_by_task_and_vin_returns_current_row() throws IOException {
        String s = statement(read("mappers/TaskVehicleMapper.xml"), "selectByTaskIdAndVin");
        String upper = s.toUpperCase();
        assertTrue(upper.contains("SUPERSEDED_BY IS NULL"), "必须限定当前车辆任务");
        assertTrue(upper.contains("ORDER BY"), "必须显式排序");
        assertTrue(upper.contains("LIMIT 1"), "必须 LIMIT 1");
        assertTrue(upper.contains("ROW_VALID = 1"), "必须过滤逻辑删除行");
    }

    @Test
    @DisplayName("关联查询保留全历史并返回当前性与取代者")
    void relation_query_exposes_currentness_and_keeps_history() throws IOException {
        String xml = read("mappers/VehicleUpgradeRelationMapper.xml");
        String tasks = statement(xml, "selectTaskRelations");
        assertTrue(tasks.toUpperCase().contains("AS IS_CURRENT"), "任务关联必须返回 is_current");
        assertTrue(tasks.toUpperCase().contains("AS SUPERSEDED_BY"), "任务关联必须返回 superseded_by");
        String upperTasks = tasks.toUpperCase();
        int filterIdx = upperTasks.indexOf("AND TV.SUPERSEDED_BY IS NULL");
        if (filterIdx > 0) {
            String before = upperTasks.substring(0, filterIdx);
            assertTrue(before.contains("<IF TEST=\"ONLYCURRENT"),
                    "任务关联的当前性过滤必须置于 onlyCurrent 条件中，默认仍返回全部历史");
        }

        String activities = statement(xml, "selectActivityRelations");
        assertTrue(activities.toUpperCase().contains("SUPERSEDED_BY IS NULL"),
                "活动分组计数必须按当前车辆任务，避免重置后任务数翻倍");
    }

    @Test
    @DisplayName("重置插入语句写入冻结摘要与初始化口径，不写 superseded_by")
    void insert_reset_target_uses_release_initialization() throws IOException {
        String s = statement(read("mappers/TaskVehicleMapper.xml"), "insertResetTarget");
        String upper = s.toUpperCase();
        assertTrue(upper.contains("SNAPSHOT_DIGEST"), "必须复用父任务冻结摘要");
        assertTrue(upper.contains("DOWNLOAD_READY_STATE"), "必须写入下载准备初始化态");
        assertTrue(upper.contains("LAST_ATTEMPT_NO"), "必须写入尝试序号初始化值");
        assertFalse(upper.contains("SUPERSEDED_BY"), "新车辆任务不得带取代者");
        assertTrue(upper.contains("USEGENERATEDKEYS"), "必须回填 AUTO_INCREMENT 主键");
    }
}
