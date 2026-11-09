package net.hwyz.iov.cloud.iov.ota.service.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 车辆零件投影 Mapper SQL 守卫测试
 *
 * <p>本仓库测试环境无数据库，无法通过执行 SQL 发现过滤条件缺失，故以 Mapper XML 内容断言固定。
 * 背景缺陷：{@code selectPoByMap} 由代码生成器生成后保留了「添加自定义条件」占位注释而未填条件，
 * 实际 SQL 为 {@code WHERE row_valid = 1} 全表返回。VMD 绑定消费者用它做「先查后插」判断，
 * 导致：① 表非空时永远命中 id 最大行改写，新零件永不插入；② UNBIND 会物理删除所有车辆的投影。</p>
 *
 * @author hwyz_leo
 */
@DisplayName("车辆零件投影 Mapper SQL 守卫")
class VehiclePartMapperSqlGuardTest {

    private static final String MAPPER = "mappers/VehiclePartMapper.xml";

    private String readMapper() throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(MAPPER)) {
            assertNotNull(in, "Mapper XML 必须存在于 classpath：" + MAPPER);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("业务身份键查询：selectPoListByVinAndPn 必须同时过滤 vin 与 pn")
    void business_key_query_filters_vin_and_pn() throws IOException {
        String xml = readMapper();
        String sql = statement(xml, "selectPoListByVinAndPn");

        assertTrue(sql.contains("AND t.vin = #{vin}"), "必须按 vin 过滤");
        assertTrue(sql.contains("AND t.pn = #{pn}"), "必须按 pn 过滤");
        assertFalse(sql.contains("添加自定义条件"), "不得残留未实现的占位条件注释");
    }

    @Test
    @DisplayName("通用 Map 查询：selectPoByMap / countPoByMap 不得退化为全表扫描")
    void generic_map_query_is_not_unfiltered() throws IOException {
        String xml = readMapper();

        for (String id : new String[]{"selectPoByMap", "countPoByMap"}) {
            String sql = statement(xml, id);
            assertFalse(sql.contains("添加自定义条件"), id + " 不得残留未实现的占位条件注释");
            assertTrue(sql.contains("AND t.vin = #{vin}"), id + " 必须支持 vin 过滤");
            assertTrue(sql.contains("AND t.pn = #{pn}"), id + " 必须支持 pn 过滤");
        }
    }

    /** 截取指定 id 的语句片段（到下一个 &lt;/select&gt; 或 &lt;/update&gt; ... 结束标签为止）。 */
    private String statement(String xml, String id) {
        int start = xml.indexOf("id=\"" + id + "\"");
        assertTrue(start > 0, "Mapper XML 必须包含语句：" + id);
        int end = xml.indexOf("</select>", start);
        if (end < 0) {
            end = xml.indexOf("</update>", start);
        }
        assertTrue(end > start, "语句未正确闭合：" + id);
        return xml.substring(start, end);
    }
}
