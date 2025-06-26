package org.archer.sqlvn.utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DQLtoDMLConverter {
    private static final Pattern SQL_STATEMENT_PATTERN = 
        Pattern.compile("(?i)SELECT\\s+.+?FROM\\s+\\S+.*?(?=(?:SELECT\\s|$))", Pattern.DOTALL);
    private static final Pattern TABLE_NAME_PATTERN = 
        Pattern.compile("(?i)FROM\\s+(\\w+(?:\\.\\w+)?)(?:\\s+\\w*)?(?:\\s+|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHERE_CLAUSE_PATTERN = 
        Pattern.compile("(?i)WHERE\\s+((?:(?!\\s+(?:ORDER\\s+BY|GROUP\\s+BY|HAVING|$)).)+", Pattern.DOTALL);

    
   
    private final Connection connection;

    public DQLtoDMLConverter(Connection connection) {
        this.connection = connection;
    }

    public static void main(String[] args) {

     // Oracle数据库连接信息
        String jdbcUrl = "jdbc:oracle:thin:@1.1.1.1:1521/oracle";
        String username = "dev";
        String password = "dev";
        
//        String jdbcUrl = args[0];
//        String username = args[1];
//        String password = args[2];
        String inputFile = "D:\\src\\main\\resources\\sqlvn\\query\\V2025.01\\dql_20250623_ceshi.sql";
        String outputFile = inputFile + "_dml.sql";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            DQLtoDMLConverter converter = new DQLtoDMLConverter(conn);
            converter.convertFile(inputFile, outputFile);
            log.info("Conversion completed successfully. Output saved to: " + outputFile);
        } catch (Exception e) {
            System.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void convertFile(String inputFile, String outputFile) throws IOException, SQLException {
        String sqlContent = readFile(inputFile);
        List<QueryWithComments> queries = extractQueriesWithComments(sqlContent);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("-- DML statements generated from: " + inputFile + "\n");
            writer.write("-- Generated on: " + new Date() + "\n\n");
            
            for (QueryWithComments query : queries) {
                try {
                	processQueryWithComments(query, writer);
                } catch (SQLException e) {
                    writer.write("-- Error processing query: " + e.getMessage() + "\n");
                    writer.write("-- Original query: " + query + "\n\n");
                }
            }
        }
    }
    
    private List<QueryWithComments> extractQueriesWithComments(String sqlContent) {
        List<QueryWithComments> queries = new ArrayList<>();
        Matcher matcher = SQL_STATEMENT_PATTERN.matcher(sqlContent);
        
        while (matcher.find()) {
            String comments = matcher.group(1);
            String query = matcher.group(2);
            queries.add(new QueryWithComments(query, comments));
        }
        
        return queries;
    }

    private void processQueryWithComments(QueryWithComments query, BufferedWriter writer) 
            throws SQLException, IOException {
        // 写入原始注释
        if (!query.getComments().trim().isEmpty()) {
            writer.write(query.getComments());
        }
        
        String tableName = extractTableName(query.getQuery());
        String whereClause = extractWhereClause(query.getQuery());
        
        // Generate DELETE statement with comment
        String deleteStmt = generateDeleteStatement(tableName, whereClause);
        writer.write("-- Generated DELETE statement for the above query\n");
        writer.write(deleteStmt + ";\n\n");
        
        // Generate INSERT statements with comment
        writer.write("-- Generated INSERT statements for the above query\n");
        List<String> insertStmts = generateInsertStatements(query.getQuery());
        for (String insertStmt : insertStmts) {
            writer.write(insertStmt + ";\n");
        }
        writer.write("\n");
    }
    
    private void processQuery(String query, BufferedWriter writer) throws SQLException, IOException {
        String tableName = extractTableName(query);
        String whereClause = extractWhereClause(query);
        
        // Generate DELETE statement
        String deleteStmt = generateDeleteStatement(tableName, whereClause);
        writer.write(deleteStmt + ";\n\n");
        
        // Generate INSERT statements
        List<String> insertStmts = generateInsertStatements(query);
        for (String insertStmt : insertStmts) {
            writer.write(insertStmt + ";\n");
        }
        writer.write("\n");
    }

    private String generateDeleteStatement(String tableName, String whereClause) {
        StringBuilder sb = new StringBuilder("DELETE FROM ").append(tableName);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sb.append(" WHERE ").append(whereClause.trim());
        }
        return sb.toString();
    }

    private List<String> generateInsertStatements(String selectQuery) throws SQLException {
        List<String> insertStatements = new ArrayList<>();
        List<String> columnNames = getColumnNames(selectQuery);
        List<Map<String, Object>> rows = executeQuery(selectQuery);
        String tableName = extractTableName(selectQuery);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Map<String, Object> row : rows) {
            StringBuilder insert = new StringBuilder("INSERT INTO ")
                .append(tableName)
                .append(" (")
                .append(String.join(", ", columnNames))
                .append(") VALUES (");
            
            for (int i = 0; i < columnNames.size(); i++) {
                if (i > 0) insert.append(", ");
                appendValue(insert, row.get(columnNames.get(i).toUpperCase()), dateFormat);
            }
            
            insert.append(")");
            insertStatements.add(insert.toString());
        }
        
        return insertStatements;
    }

    private void appendValue(StringBuilder sb, Object value, SimpleDateFormat dateFormat) {
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
            sb.append("'").append(value.toString().replace("'", "''")).append("'");
        }
    }

    private List<String> extractQueries(String sqlContent) {
        List<String> queries = new ArrayList<>();
        Matcher matcher = SQL_STATEMENT_PATTERN.matcher(sqlContent);
        
        while (matcher.find()) {
            queries.add(matcher.group().trim());
        }
        
        return queries;
    }

    private String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 移除单行注释
                if (!line.trim().startsWith("--")) {
                    content.append(line).append("\n");
                }
            }
        }
        return content.toString();
    }

    // 以下是辅助方法，与之前版本相同
    private String extractTableName(String selectQuery) {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(selectQuery);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        throw new IllegalArgumentException("Could not extract table name from: " + selectQuery);
    }

    private String extractWhereClause(String selectQuery) {
        Matcher matcher = WHERE_CLAUSE_PATTERN.matcher(selectQuery);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private List<String> getColumnNames(String selectQuery) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(selectQuery);
             ResultSet rs = pstmt.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columnNames.add(metaData.getColumnName(i));
            }
        }
        return columnNames;
    }

    private List<Map<String, Object>> executeQuery(String selectQuery) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(selectQuery);
             ResultSet rs = pstmt.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }
        }
        return rows;
    }
    
    class QueryWithComments {
        private final String query;
        private final String comments;

        public QueryWithComments(String query, String comments) {
            this.query = query;
            this.comments = comments;
        }

        public String getQuery() {
            return query;
        }

        public String getComments() {
            return comments;
        }
    }
}