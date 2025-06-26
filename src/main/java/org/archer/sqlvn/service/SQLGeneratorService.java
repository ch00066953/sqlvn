package org.archer.sqlvn.service;

import java.sql.SQLException;

import lombok.AllArgsConstructor;
import lombok.Data;

public interface SQLGeneratorService {
    String generateDeleteStatement(String selectQuery) throws SQLException;
    String generateInsertStatement(String selectQuery) throws SQLException;
    SQLStatements generateBothStatements(String selectQuery) throws SQLException;
    
    @Data
    @AllArgsConstructor
    public class SQLStatements {
    	private String deleteStatement;
    	private String insertStatement;
    }
}
