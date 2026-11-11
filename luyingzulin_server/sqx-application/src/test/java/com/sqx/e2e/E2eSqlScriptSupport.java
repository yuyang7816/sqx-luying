package com.sqx.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * E2E/联调：幂等执行 db/migration 下 SQL 脚本（跳过注释与空行）。
 */
final class E2eSqlScriptSupport {

    private E2eSqlScriptSupport() {
    }

    static void ensureTableExists(String tableName, String relativeSqlPath) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MySQL 驱动不可用", ex);
        }
        try (Connection connection = openConnection()) {
            if (tableExists(connection, tableName)) {
                return;
            }
            executeSqlFile(connection, resolveSqlPath(relativeSqlPath));
            if (!tableExists(connection, tableName)) {
                throw new IllegalStateException("执行 " + relativeSqlPath + " 后仍缺少表 " + tableName);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("执行 SQL 脚本失败: " + relativeSqlPath, ex);
        }
    }

    static void executeSqlFile(Connection connection, Path sqlPath) throws IOException, SQLException {
        String raw = new String(Files.readAllBytes(sqlPath), StandardCharsets.UTF_8);
        List<String> statements = splitStatements(raw);
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    static Path resolveSqlPath(String relativeSqlPath) {
        Path cwd = Paths.get("").toAbsolutePath();
        Path[] candidates = new Path[] {
                cwd.resolve(relativeSqlPath),
                cwd.resolve("..").resolve(relativeSqlPath),
                cwd.resolve("..").resolve("..").resolve(relativeSqlPath),
                cwd.resolve("luyingzulin_server").resolve(relativeSqlPath)
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.normalize();
            }
        }
        throw new IllegalStateException("未找到 SQL 脚本: " + relativeSqlPath + "（cwd=" + cwd + "）");
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                E2eServerSupport.MYSQL_JDBC_URL,
                E2eServerSupport.MYSQL_USER,
                E2eServerSupport.MYSQL_PASSWORD);
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, new String[] { "TABLE" })) {
            return rs.next();
        }
    }

    static List<String> splitStatements(String raw) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\'') {
                inSingleQuote = !inSingleQuote;
                current.append(ch);
                continue;
            }
            if (!inSingleQuote && ch == ';') {
                String sql = stripLeadingComments(current.toString()).trim();
                if (!sql.isEmpty()) {
                    statements.add(sql);
                }
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        String tail = stripLeadingComments(current.toString()).trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }

    private static String stripLeadingComments(String block) {
        String[] lines = block.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString();
    }
}
