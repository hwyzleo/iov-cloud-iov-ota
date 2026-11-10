package net.hwyz.iov.cloud.iov.ota.service.integration;

import com.github.pagehelper.parser.DefaultCountSqlParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CR-023 关联查询 SQL 守卫测试
 *
 * <p>本仓库测试环境无数据库，以 Mapper XML 内容断言固定：
 * 活动分组去重、任务一次 JOIN、默认排序、禁止 LIMIT 1 任取；
 * 并用 PageHelper 的 count SQL 解析器验证分组分页总数 = 活动分组数。
 *
 * @author hwyz_leo
 */
@DisplayName("CR-023 车辆关联查询 SQL 守卫")
class VehicleUpgradeRelationSqlGuardTest {

    private static final String MAPPER = "mappers/VehicleUpgradeRelationMapper.xml";

    private String readMapper() throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(MAPPER)) {
            assertNotNull(in, "Mapper XML 必须存在于 classpath：" + MAPPER);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("活动：以 tb_task_vehicle 为起点 JOIN tb_activity 分组聚合")
    void activity_relation_is_grouped() throws IOException {
        String xml = readMapper();
        assertTrue(xml.contains("id=\"selectActivityRelations\""));
        assertTrue(xml.contains("JOIN tb_activity a ON a.id = tv.activity_id AND a.row_valid = 1"));
        assertTrue(xml.contains("COUNT(tv.id)                          AS vehicle_task_count"));
        assertTrue(xml.contains("MAX(tv.create_time)                   AS latest_task_time"));
        assertTrue(xml.contains("GROUP BY tv.activity_id, a.name, a.state"));
        assertTrue(xml.contains("ORDER BY latest_task_time DESC"));
        assertFalse(xml.contains("LIMIT 1"), "禁止用 LIMIT 1 任取候选");
    }

    @Test
    @DisplayName("任务：一次 JOIN tb_task/tb_activity 取名称与状态，固定排序与过滤")
    void task_relation_is_joined_once() throws IOException {
        String xml = readMapper();
        assertTrue(xml.contains("id=\"selectTaskRelations\""));
        assertTrue(xml.contains("JOIN tb_task t ON t.id = tv.task_id AND t.row_valid = 1"));
        assertTrue(xml.contains("JOIN tb_activity a ON a.id = tv.activity_id AND a.row_valid = 1"));
        assertTrue(xml.contains("t.name                       AS task_name"));
        assertTrue(xml.contains("a.name                       AS activity_name"));
        assertTrue(xml.contains("tv.release_at                AS release_at"));
        assertTrue(xml.contains("tv.vt_start_time             AS start_time"));
        assertTrue(xml.contains("tv.vt_end_time               AS end_time"));
        assertTrue(xml.contains("AND tv.activity_id = #{activityId}"));
        assertTrue(xml.contains("AND t.state = #{taskState}"));
        assertTrue(xml.contains("AND tv.vehicle_task_status = #{vehicleTaskStatus}"));
        assertTrue(xml.contains("AND tv.create_time &gt;= #{beginTime}"));
        assertTrue(xml.contains("AND tv.create_time &lt;= #{endTime}"));
        assertTrue(xml.contains("ORDER BY tv.create_time DESC, tv.id DESC"));
        assertFalse(xml.contains("LIMIT 1"));
    }

    @Test
    @DisplayName("任务：vin 为可选过滤，缺失时返回全部车辆任务（原 taskVehicle/list 语义）")
    void task_relation_vin_is_optional() throws IOException {
        String xml = readMapper();
        int start = xml.indexOf("id=\"selectTaskRelations\"");
        assertTrue(start > 0, "必须存在 selectTaskRelations");
        String taskSql = xml.substring(start, xml.indexOf("</select>", start));
        assertFalse(taskSql.contains("WHERE tv.vin = #{vin}"), "vin 不得为硬等值，否则无 VIN 查询恒空");
        assertTrue(taskSql.contains("<if test=\"vin != null and vin != ''\">"), "vin 必须由 <if> 包裹为可选条件");
        assertTrue(taskSql.contains("AND tv.vin = #{vin}"));
        assertTrue(taskSql.contains("tv.vin                       AS vin,"), "全局列表需由 SQL 回传 vin");
        assertTrue(xml.contains("<result column=\"vin\" property=\"vin\" />"), "vin 需映射到结果对象");
    }

    @Test
    @DisplayName("分组分页总数：PageHelper count 包裹外层，total 为活动分组数而非任务行数")
    void group_by_count_sql_counts_activity_groups() {
        String groupBySql = "SELECT tv.activity_id AS activity_id, a.name AS activity_name, a.state AS activity_state, "
                + "COUNT(tv.id) AS vehicle_task_count, MAX(tv.create_time) AS latest_task_time "
                + "FROM tb_task_vehicle tv "
                + "JOIN tb_activity a ON a.id = tv.activity_id AND a.row_valid = 1 "
                + "WHERE tv.vin = ? AND tv.row_valid = 1 "
                + "GROUP BY tv.activity_id, a.name, a.state ORDER BY latest_task_time DESC";
        String countSql = new DefaultCountSqlParser().getSmartCountSql(groupBySql);

        assertTrue(countSql.startsWith("SELECT count(0) FROM ("), "分组查询必须包裹为子查询计数");
        assertTrue(countSql.contains("GROUP BY tv.activity_id, a.name, a.state"), "内层保留 GROUP BY");
        assertTrue(countSql.endsWith("table_count"), "外层计数 = 分组数，避免把任务行数当活动总数");
    }
}
