package org.archer.sqlvn.utils;

import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLStatementGenerator {
    private static final Pattern TABLE_NAME_PATTERN = 
        Pattern.compile("FROM\\s+([\\w.]+)(?:\\s+|$)", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern WHERE_CLAUSE_PATTERN = 
    	    Pattern.compile("WHERE\\s+(.+?)(?:\\s+(?:ORDER\\s+BY|GROUP\\s+BY|HAVING|$)|$)", 
    	                   Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 根据SELECT语句生成DELETE语句
     */
    public static String generateDeleteStatement(Connection conn, String selectQuery) throws SQLException {
        String tableName = extractTableName(selectQuery);
        String whereClause = extractWhereClause(selectQuery);
        
        validateTableName(tableName);
        
        StringBuilder deleteStatement = new StringBuilder("DELETE FROM ").append(tableName);
        
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            deleteStatement.append(" WHERE ").append(whereClause.trim()).append(";");
        }
        
        return deleteStatement.toString();
    }

    /**
     * 根据SELECT语句生成包含实际值的INSERT语句
     */
    public static List<String> generateInsertStatements(Connection conn, String selectQuery) throws SQLException {
        String tableName = extractTableName(selectQuery);
        validateTableName(tableName);
        
        List<String> columnNames = getColumnNames(conn, selectQuery);
        List<Map<String, Object>> rows = executeQuery(conn, selectQuery);
        
        List<String> insertStatements = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        for (Map<String, Object> row : rows) {
            StringBuilder insert = new StringBuilder("INSERT INTO ")
                .append(tableName)
                .append(" (")
                .append(String.join(", ", columnNames))
                .append(") VALUES (");
            
            for (int i = 0; i < columnNames.size(); i++) {
                if (i > 0) {
                    insert.append(", ");
                }
                
                String columnName = columnNames.get(i);
                Object value = row.get(columnName.toUpperCase()); // Oracle列名通常大写
                
                appendValue(insert, value, dateFormat);
            }
            
            insert.append(");");
            insertStatements.add(insert.toString());
        }
        
        return insertStatements;
    }

    private static void appendValue(StringBuilder sb, Object value, SimpleDateFormat dateFormat) {
        if (value == null) {
            sb.append("NULL");
        } else if (value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Date) {
            sb.append("TO_DATE('")
              .append(dateFormat.format((Date) value))
              .append("', 'YYYY-MM-DD HH24:MI:SS')");
        } else if (value instanceof Boolean) {
            sb.append(((Boolean) value) ? "1" : "0");
        } else {
            // 处理字符串，转义单引号
            String strValue = value.toString().replace("'", "''");
            sb.append("'").append(strValue).append("'");
        }
    }

    /**
     * 从SELECT语句中提取表名
     */
    public static String extractTableName(String selectQuery) {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(selectQuery);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        throw new IllegalArgumentException("Could not extract table name from SQL: " + selectQuery);
    }

    /**
     * 从SELECT语句中提取WHERE子句
     */
    public static String extractWhereClause(String selectQuery) {
        Matcher matcher = WHERE_CLAUSE_PATTERN.matcher(selectQuery);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * 获取查询结果的列名
     */
    public static List<String> getColumnNames(Connection conn, String selectQuery) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(selectQuery);
             ResultSet rs = pstmt.executeQuery()) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
        }
        
        return columnNames;
    }

    /**
     * 执行查询并返回结果集
     */
    private static List<Map<String, Object>> executeQuery(Connection conn, String selectQuery) 
            throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(selectQuery);
             ResultSet rs = pstmt.executeQuery()) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }
        }
        
        return rows;
    }

    /**
     * 验证表名是否合法
     */
    private static void validateTableName(String tableName) {
        if (!tableName.matches("[\\w.]+")) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
    }

    /**
     * 生成MERGE语句（Oracle特有的UPSERT操作）
     */
    public static String generateMergeStatement(Connection conn, String selectQuery) throws SQLException {
        String tableName = extractTableName(selectQuery);
        validateTableName(tableName);
        
        List<String> columnNames = getColumnNames(conn, selectQuery);
        List<String> keyColumns = identifyPrimaryKeys(conn, tableName);
        
        if (keyColumns.isEmpty()) {
            throw new SQLException("Cannot generate MERGE statement - no primary key found for table: " + tableName);
        }
        
        StringBuilder merge = new StringBuilder("MERGE INTO ")
            .append(tableName)
            .append(" t USING (")
            .append(selectQuery)
            .append(") s ON (");
        
        // 构建ON条件（基于主键）
        for (int i = 0; i < keyColumns.size(); i++) {
            if (i > 0) {
                merge.append(" AND ");
            }
            merge.append("t.").append(keyColumns.get(i))
                 .append(" = s.").append(keyColumns.get(i));
        }
        
        merge.append(")\nWHEN MATCHED THEN UPDATE SET\n");
        
        // 构建UPDATE部分（非主键列）
        int updateCount = 0;
        for (String column : columnNames) {
            if (!keyColumns.contains(column)) {
                if (updateCount > 0) {
                    merge.append(",\n");
                }
                merge.append("    t.").append(column).append(" = s.").append(column);
                updateCount++;
            }
        }
        
        merge.append("\nWHEN NOT MATCHED THEN INSERT (")
             .append(String.join(", ", columnNames))
             .append(")\nVALUES (");
        
        // 构建INSERT部分
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) {
                merge.append(", ");
            }
            merge.append("s.").append(columnNames.get(i));
        }
        
        merge.append(")");
        
        return merge.toString();
    }

    /**
     * 识别表的主键列
     */
    private static List<String> identifyPrimaryKeys(Connection conn, String tableName) throws SQLException {
        List<String> keys = new ArrayList<>();
        
        // 解析可能的模式名和表名
        String schema = null;
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.");
            schema = parts[0];
            tableName = parts[1];
        }
        
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getPrimaryKeys(conn.getCatalog(), schema, tableName)) {
            while (rs.next()) {
                keys.add(rs.getString("COLUMN_NAME"));
            }
        }
        
        return keys;
    }
    
    public static void main(String[] args) {
        // Oracle数据库连接信息
        String jdbcUrl = "jdbc:oracle:thin:@1.1.1.1:1521/oracle";
        String username = "dev";
        String password = "dev";
        
        // 示例SELECT查询
        String selectQuery = "SELECT * " +
                            "FROM dev.b WHERE b.b = '2021061200000051'";
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            log.info("=== 数据库连接成功 ===");
            
            // 1. 生成DELETE语句
            String deleteStmt = generateDeleteStatement(conn, selectQuery);
            log.info("\n生成的DELETE语句:");
            log.info(deleteStmt);
            
            // 2. 生成INSERT语句(带实际值)
            log.info("\n生成的INSERT语句:");
            List<String> insertStmts = generateInsertStatements(conn, selectQuery);
            for (String stmt : insertStmts) {
                log.info(stmt);
            }
            
            // 3. 生成MERGE语句
            try {
                String mergeStmt = generateMergeStatement(conn, selectQuery);
                log.info("\n生成的MERGE语句:");
                log.info(mergeStmt);
            } catch (SQLException e) {
                log.info("\n生成MERGE语句失败: " + e.getMessage());
            }
            
            // 4. 测试表名提取
            log.info("\n测试表名提取:");
            String[] testQueries = {
                "SELECT * FROM user_info",
                "select e.userid, e.username from dev.user_info e where e.userid > '1'",
                "SELECT * FROM (SELECT * FROM user_info) WHERE userid = 'test'",
                "SELECT * FROM user_info WHERE userid in (SELECT userid FROM user_info where userid = 'test')"
            };
            
            for (String query : testQueries) {
                try {
                    log.info("'" + query + "' => " + extractTableName(query));
                } catch (IllegalArgumentException e) {
                    log.info("'" + query + "' => 错误: " + e.getMessage());
                }
            }
            
        } catch (SQLException e) {
            System.err.println("数据库连接或操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}