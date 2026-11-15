package net.hwyz.iov.cloud.iov.ota.service.integration;

import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BasePo 表结构一致性守卫。
 *
 * <p>本仓库测试环境无数据库，无法直接比对真实表结构，因此以「按版本顺序回放
 * Flyway 脚本（CREATE TABLE / ALTER TABLE ... ADD|DROP COLUMN / DROP TABLE）」
 * 的方式推算出最终表结构，再断言所有继承
 * {@link BasePo} 的 PO 所映射的表都包含基类约定的列。</p>
 *
 * <p>背景：BasePo 含 description/createBy/createTime/modifyBy/modifyTime/
 * rowVersion/rowValid，MyBatis-Plus 的 {@code BaseMapper.selectList/selectById}
 * 会生成全字段 SELECT；建表时漏列会直接抛
 * {@code Unknown column 'xxx' in 'field list'}（V12 / V32 / V41 均为同类修复）。</p>
 *
 * @author hwyz_leo
 */
@DisplayName("BasePo 表结构一致性守卫")
class BasePoSchemaConsistencyTest {

    /** BasePo 约定列（缺任一列都会导致 MyBatis-Plus 全字段 SQL 失败） */
    private static final Set<String> BASE_PO_COLUMNS = new LinkedHashSet<>(Arrays.asList(
            "description", "create_by", "create_time", "modify_by", "modify_time", "row_version", "row_valid"));

    private static final String MIGRATION_DIR = "db/migration";
    private static final String DESCRIPTION_FIX_MIGRATION =
            "db/migration/V41__fix_missing_base_po_description_columns.sql";
    private static final String PO_PACKAGE = "net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po";

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "^CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+`?(\\w+)`?\\s*\\((.*)\\)\\s*ENGINE\\s*=",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ALTER_TABLE = Pattern.compile(
            "^ALTER\\s+TABLE\\s+`?(\\w+)`?\\s+(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DROP_TABLE = Pattern.compile(
            "^DROP\\s+TABLE(?:\\s+IF\\s+EXISTS)?\\s+`?(\\w+)`?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ADD_COLUMN = Pattern.compile("ADD\\s+COLUMN\\s+`?(\\w+)`?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_COLUMN = Pattern.compile("DROP\\s+COLUMN\\s+`?(\\w+)`?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COLUMN_DEF = Pattern.compile("^\\s*`(\\w+)`\\s", Pattern.MULTILINE);
    private static final Pattern VERSION = Pattern.compile("^V(\\d+)__.*\\.sql$");

    @Test
    @DisplayName("所有继承 BasePo 的 PO 映射表都包含 BasePo 约定列")
    void everyBasePoTableHasBasePoColumns() throws Exception {
        Map<String, Set<String>> schema = replayMigrations();
        Map<String, String> poByTable = scanBasePoTables();

        assertTrue(!poByTable.isEmpty(), "必须至少扫描到一个继承 BasePo 的 PO");

        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, String> entry : poByTable.entrySet()) {
            String table = entry.getKey();
            String po = entry.getValue();
            Set<String> columns = schema.get(table);
            if (columns == null) {
                violations.add(table + "（" + po + "）：迁移脚本中未找到该表的建表语句");
                continue;
            }
            Set<String> missing = new LinkedHashSet<>(BASE_PO_COLUMNS);
            missing.removeAll(columns);
            if (!missing.isEmpty()) {
                violations.add(table + "（" + po + "）：缺少列 " + missing);
            }
        }

        assertEquals(List.of(), violations,
                "以下表结构缺少 BasePo 约定列，MyBatis-Plus 全字段 SELECT 会抛 Unknown column：\n - "
                        + String.join("\n - ", violations)
                        + "\n修复方式：新增前向迁移脚本 ADD COLUMN（不得改写已执行的历史迁移）。");
    }

    @Test
    @DisplayName("V41 修复迁移：仅静态 DDL 追加 description，不改写历史数据")
    void v41AddsDescriptionWithStaticDdlOnly() throws IOException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(DESCRIPTION_FIX_MIGRATION);
        assertNotNull(url, "修复迁移必须存在于 classpath：" + DESCRIPTION_FIX_MIGRATION);
        String sql = Files.readString(Paths.get(url.toURI()), StandardCharsets.UTF_8);
        String executable = stripLineComments(sql).toUpperCase();

        for (String table : new String[]{"tb_task_vehicle_reset_log", "tb_vehicle_inventory_software_unit",
                "tb_task_vehicle_execution_ecu_software_unit", "tb_activity_approval"}) {
            assertTrue(sql.contains("ALTER TABLE `" + table + "`"), "必须补充 " + table + " 的 description 列");
        }
        assertEquals(4, sql.split("ADD COLUMN `description`", -1).length - 1,
                "必须恰好补充 4 张表的 description 列");

        assertTrue(!executable.contains("PREPARE ") && !executable.contains("SIGNAL SQLSTATE"),
                "Druid WallFilter 拦截 PREPARE/SIGNAL，只允许静态 DDL");
        assertTrue(!executable.contains("INFORMATION_SCHEMA"), "不做动态存在性判断");
        assertTrue(!executable.contains("DELETE FROM") && !executable.contains("INSERT INTO")
                && !executable.contains("UPDATE "), "不得清洗或回填数据");
        assertTrue(!executable.contains("DROP COLUMN") && !executable.contains("MODIFY COLUMN"),
                "修复迁移只允许 ADD COLUMN");
    }

    /**
     * 按版本顺序回放迁移脚本，推算各表最终列集合。
     *
     * <p>注释先行剔除：整行注释里的 {@code -- DROP TABLE ...} 等示例不能被当成可执行语句。</p>
     */
    private Map<String, Set<String>> replayMigrations() throws IOException, URISyntaxException {
        List<Path> files;
        try (Stream<Path> stream = Files.list(classpathDirectory(MIGRATION_DIR))) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> VERSION.matcher(p.getFileName().toString()).matches())
                    .sorted(Comparator.comparingInt(p -> version(p.getFileName().toString())))
                    .collect(Collectors.toList());
        }
        assertTrue(files.size() > 40, "迁移脚本数量异常，仅发现：" + files.size());

        Map<String, Set<String>> schema = new LinkedHashMap<>();
        for (Path file : files) {
            String sql = stripLineComments(Files.readString(file, StandardCharsets.UTF_8));
            for (String statement : sql.split(";")) {
                applyStatement(statement.trim(), schema);
            }
        }
        return schema;
    }

    private void applyStatement(String statement, Map<String, Set<String>> schema) {
        if (statement.isEmpty()) {
            return;
        }
        Matcher drop = DROP_TABLE.matcher(statement);
        if (drop.find()) {
            schema.remove(drop.group(1));
            return;
        }
        Matcher create = CREATE_TABLE.matcher(statement);
        if (create.find()) {
            Set<String> columns = new LinkedHashSet<>();
            Matcher column = COLUMN_DEF.matcher(create.group(2));
            while (column.find()) {
                columns.add(column.group(1));
            }
            schema.put(create.group(1), columns);
            return;
        }
        Matcher alter = ALTER_TABLE.matcher(statement);
        if (alter.find()) {
            String table = alter.group(1);
            String body = alter.group(2);
            Set<String> columns = schema.computeIfAbsent(table, k -> new LinkedHashSet<>());
            Matcher add = ADD_COLUMN.matcher(body);
            while (add.find()) {
                columns.add(add.group(1));
            }
            Matcher remove = DROP_COLUMN.matcher(body);
            while (remove.find()) {
                columns.remove(remove.group(1));
            }
        }
    }

    /**
     * 扫描所有编译产物 PO，返回 表名 -> PO 简单类名（继承 BasePo 且声明 @TableName 的类）。
     */
    private Map<String, String> scanBasePoTables() throws IOException, URISyntaxException, ClassNotFoundException {
        Map<String, String> result = new LinkedHashMap<>();
        List<Path> classes;
        try (Stream<Path> stream = Files.list(classpathDirectory(PO_PACKAGE))) {
            classes = stream
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .filter(p -> !p.getFileName().toString().contains("$"))
                    .collect(Collectors.toList());
        }
        for (Path clazz : classes) {
            String simpleName = clazz.getFileName().toString().replace(".class", "");
            Class<?> type = Class.forName(PO_PACKAGE.replace('/', '.') + "." + simpleName);
            if (!BasePo.class.isAssignableFrom(type) || type.equals(BasePo.class)) {
                continue;
            }
            TableName tableName = type.getAnnotation(TableName.class);
            assertNotNull(tableName, "继承 BasePo 的 PO 必须声明 @TableName：" + simpleName);
            result.put(tableName.value(), simpleName);
        }
        return result;
    }

    private Path classpathDirectory(String resource) throws IOException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        assertNotNull(url, "classpath 资源缺失：" + resource);
        return Paths.get(url.toURI());
    }

    private String stripLineComments(String sql) {
        return Arrays.stream(sql.split("\n"))
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
    }

    private int version(String fileName) {
        Matcher matcher = VERSION.matcher(fileName);
        assertTrue(matcher.matches(), "迁移脚本命名必须符合 V<版本号>__<描述>.sql：" + fileName);
        return Integer.parseInt(matcher.group(1));
    }
}
