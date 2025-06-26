package org.archer.sqlvn.service.impl;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.archer.sqlvn.service.SQLGeneratorService;
import org.archer.sqlvn.utils.SQLStatementGenerator;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SQLGeneratorServiceImpl implements SQLGeneratorService {

    private final DataSource dataSource;

    @Override
    public String generateDeleteStatement(String selectQuery) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return SQLStatementGenerator.generateDeleteStatement(conn, selectQuery);
        }
    }

    @Override
    public String generateInsertStatement(String selectQuery) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return SQLStatementGenerator.generateInsertStatements(conn, selectQuery).toString();
        }
    }

    @Override
    public SQLStatements generateBothStatements(String selectQuery) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String delete = SQLStatementGenerator.generateDeleteStatement(conn, selectQuery);
            String insert = SQLStatementGenerator.generateInsertStatements(conn, selectQuery).toString();
            return new SQLStatements(delete, insert);
        }
    }
}