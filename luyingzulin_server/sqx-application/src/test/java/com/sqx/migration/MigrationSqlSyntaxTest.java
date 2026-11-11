package com.sqx.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M00：db/migration 脚本语法静态校验（不依赖 MySQL 连接）
 */
class MigrationSqlSyntaxTest {

    private static final Pattern ALLOWED_PREFIX = Pattern.compile(
            "^(CREATE|ALTER|DROP|INSERT|UPDATE|DELETE|SET|USE|TRUNCATE)\\b",
            Pattern.CASE_INSENSITIVE
    );

    @ParameterizedTest
    @ValueSource(strings = {
            "rental_v1.sql",
            "rollback-rental_v1.sql",
            "m01_tabbar_seed.sql",
            "rollback-m01_tabbar_seed.sql",
            "m02_goods_browse_seed.sql",
            "rollback-m02_goods_browse_seed.sql",
            "m03_cart_order_seed.sql",
            "rollback-m03_cart_order_seed.sql",
            "m04_order_lifecycle.sql",
            "rollback-m04_order_lifecycle.sql",
            "m04_order_lifecycle_seed.sql",
            "rollback-m04_order_lifecycle_seed.sql"
    })
    void migrationScriptShouldParseWithoutSyntaxIssues(String fileName) throws IOException {
        Path sqlFile = resolveMigrationFile(fileName);
        assertTrue(Files.exists(sqlFile), "迁移脚本不存在: " + sqlFile);

        String rawSql = new String(Files.readAllBytes(sqlFile), StandardCharsets.UTF_8);
        assertFalse(rawSql.trim().isEmpty(), fileName + " 不能为空");

        String cleaned = stripSqlComments(rawSql);
        List<String> statements = splitSqlStatements(cleaned);
        assertFalse(statements.isEmpty(), fileName + " 应至少包含一条 SQL 语句");

        for (String statement : statements) {
            assertBalancedDelimiters(statement, fileName);
            assertTrue(
                    ALLOWED_PREFIX.matcher(statement.trim()).find(),
                    fileName + " 含不支持的语句前缀: " + abbreviate(statement)
            );
        }
    }

    @Test
    void m04ScriptShouldDefineLifecycleTables() throws IOException {
        String sql = readMigration("m04_order_lifecycle.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `rent_renew_record`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `rent_return_apply`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `rent_inspection`"));
    }

    @Test
    void rentalScriptShouldDefineCoreTables() throws IOException {
        String sql = readMigration("rental_v1.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `rent_category`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `rent_goods`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `rent_order`"));
    }

    @Test
    void rollbackScriptShouldDropCoreTables() throws IOException {
        String sql = readMigration("rollback-rental_v1.sql");
        assertTrue(sql.toUpperCase(Locale.ROOT).contains("DROP TABLE"));
        assertTrue(sql.contains("rent_category"));
        assertTrue(sql.contains("rent_goods"));
    }

    private static Path resolveMigrationFile(String fileName) {
        Path fromModule = Paths.get("..", "db", "migration", fileName).normalize();
        if (Files.exists(fromModule)) {
            return fromModule;
        }
        Path fromServerRoot = Paths.get("db", "migration", fileName).normalize();
        if (Files.exists(fromServerRoot)) {
            return fromServerRoot;
        }
        return Paths.get(System.getProperty("user.dir")).resolve("db/migration").resolve(fileName);
    }

    private static String readMigration(String fileName) throws IOException {
        return new String(Files.readAllBytes(resolveMigrationFile(fileName)), StandardCharsets.UTF_8);
    }

    private static String stripSqlComments(String sql) {
        String noBlock = sql.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
        StringBuilder builder = new StringBuilder();
        for (String line : noBlock.split("\\R")) {
            int commentIndex = line.indexOf("--");
            builder.append(commentIndex >= 0 ? line.substring(0, commentIndex) : line).append('\n');
        }
        return builder.toString();
    }

    private static List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'' && !inDoubleQuote && !inBacktick) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (ch == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            }

            if (ch == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                String statement = current.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }

        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }

    private static void assertBalancedDelimiters(String statement, String fileName) {
        assertTrue(isBalanced(statement, '(', ')'), fileName + " 括号未闭合: " + abbreviate(statement));
        assertTrue(isBacktickBalanced(statement), fileName + " 反引号未闭合: " + abbreviate(statement));
    }

    private static boolean isBacktickBalanced(String text) {
        boolean inSingleQuote = false;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\'' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (!inSingleQuote && ch == '`') {
                count++;
            }
        }
        return count % 2 == 0;
    }

    private static boolean isBalanced(String text, char open, char close) {
        int depth = 0;
        boolean inSingleQuote = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\'' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (inSingleQuote) {
                continue;
            }
            if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth < 0) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    private static String abbreviate(String statement) {
        String normalized = statement.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 77) + "...";
    }
}
