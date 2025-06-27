package org.archer.sqlvn.utils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DQLtoDMLConverterWithComments {
	// 增强版正则表达式，明确匹配以分号结尾的SQL查询
    private static final Pattern SQL_QUERY_PATTERN = Pattern.compile(
        "(?s)" +                                  // DOTALL模式
        "(?:" +
        "(/\\*.*?\\*/\\s*)" +                    // 多行注释 /* ... */
        "|" +
        "(--[^\\n]*\\n\\s*)" +                   // 单行注释 --
        ")*" +                                    // 注释是可选的
        "(SELECT\\b.+?)(;?)" +                    // 分号作为可选捕获组
        "(?=\\s*(?:SELECT\\b|/\\*|--|$))",       // 前瞻断言
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern TABLE_NAME_PATTERN = 
        Pattern.compile("(?i)FROM\\s+(\\w+(?:\\.\\w+)?)(?:\\s+\\w*)?(?:\\s+|$)", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern WHERE_CLAUSE_PATTERN = 
    	    Pattern.compile("(?i)WHERE\\s+((?:(?!\\s+(?:ORDER\\s+BY|GROUP\\s+BY|HAVING|FOR|$)).)*)", 
    	                  Pattern.DOTALL);
    
    private final Connection connection;

    public DQLtoDMLConverterWithComments(Connection connection) {
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
        String inputFile = "D:\\src\\main\\resources\\sqlvn\\query\\V2025.01\\dql_20250623_llm.sql";
        String outputFile = inputFile + "_dml.sql";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            DQLtoDMLConverterWithComments converter = new DQLtoDMLConverterWithComments(conn);
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
                	log.error("-- Error processing query: " + query.getQuery() + "|" + query.getComments() + "\n");
                	log.error("-- Error processing query: " + e.getMessage() + "\n");
                    writer.write("-- Error processing query: " + e.getMessage() + "\n");
                    writer.write("-- Original query with comments:\n");
                    writer.write(query.getComments());
                    writer.write(query.getQuery() + "\n\n");
                    throw e;
                }
            }
        }
    }

    private void processQueryWithComments(QueryWithComments query, BufferedWriter writer) 
            throws SQLException, IOException {
        // 写入原始注释
        if (!query.getComments().trim().isEmpty()) {
            writer.write(query.getComments());
        }
        
        writer.write("-- OriginalQuery: " +query.getOriginalQuery() + "\n");
        
        String tableName = extractTableName(query.getQuery());
        String whereClause = extractWhereClause(query.getQuery());
        
        // Generate DELETE statement with comment
        String deleteStmt = generateDeleteStatement(tableName, whereClause);
//        writer.write("-- Generated DELETE statement for the above query\n");
        writer.write(deleteStmt + "\n\n");
        
        // Generate INSERT statements with comment
//        writer.write("-- Generated INSERT statements for the above query\n");
        List<String> insertStmts = generateInsertStatements(query.getQuery());
        for (String insertStmt : insertStmts) {
            writer.write(insertStmt + "\n");
        }
        writer.write("\n");
    }

    /**
     * 从SQL文本中提取查询及其相关注释
     */
    public static List<QueryWithComments> extractQueriesWithComments(String sqlText) {
        List<QueryWithComments> queries = new ArrayList<>();
        if (sqlText == null || sqlText.trim().isEmpty()) {
            return queries;
        }
        
        Matcher matcher = SQL_QUERY_PATTERN.matcher(sqlText);
        while (matcher.find()) {
            String multiLineComment = matcher.group(1);   // /* ... */ 注释
            String singleLineComment = matcher.group(2);  // -- 注释
            String query = matcher.group(3);              // SELECT语句主体
            String semicolon = matcher.group(4);          // 可能的分号
            
            // 合并所有注释
            StringBuilder comments = new StringBuilder();
            if (multiLineComment != null) {
                comments.append(multiLineComment);
            }
            if (singleLineComment != null) {
                comments.append(singleLineComment);
            }
            
            if (query != null) {
                // 去除查询末尾可能存在的分号
                query = query.trim();
                if (query.endsWith(";")) {
                    query = query.substring(0, query.length() - 1);
                }
                queries.add(new QueryWithComments(query, comments.toString()));
            }
        }
        
        return queries;
    }

    // 其他辅助方法与之前版本相同...
    // generateDeleteStatement, generateInsertStatements, appendValue
    // extractTableName, extractWhereClause, getColumnNames, executeQuery
    
    private String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

  //以下是辅助方法，与之前版本相同
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

  private String generateDeleteStatement(String tableName, String whereClause) {
      StringBuilder sb = new StringBuilder("DELETE FROM ").append(tableName);
      if (whereClause != null && !whereClause.trim().isEmpty()) {
          sb.append(" WHERE ").append(whereClause.trim());
      }
	  sb.append(";");
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
          
          insert.append(");");
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
  
  
  public static class QueryWithComments {
      private final String query;  // 不包含分号的查询
      private final String comments;
      
      public QueryWithComments(String query, String comments) {
          this.query = query;
          this.comments = comments;
      }
      
      public String getQuery() {
          return query;
      }
      
      public String getExecutableQuery() {
          // 返回可直接执行的SQL（不带分号）
          return query;
      }
      
      public String getOriginalQuery() {
          // 返回原始格式（带分号）
          return query + ";";
      }
      
      public String getComments() {
          return comments;
      }
  }
}