package org.archer.sqlvn.controller;

import java.sql.SQLException;

import org.archer.sqlvn.service.SQLGeneratorService;
import org.archer.sqlvn.service.SQLGeneratorService.SQLStatements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-generator")
public class SQLGeneratorController {

	@Autowired
    private SQLGeneratorService sqlGeneratorService;

    @PostMapping("/delete")
    public ResponseEntity<String> generateDeleteStatement(@RequestBody String selectQuery) {
        try {
            String deleteStatement = sqlGeneratorService.generateDeleteStatement(selectQuery);
            return ResponseEntity.ok(deleteStatement);
        } catch (SQLException e) {
            return ResponseEntity.badRequest().body("Error generating DELETE statement: " + e.getMessage());
        }
    }

    @PostMapping("/insert")
    public ResponseEntity<String> generateInsertStatement(@RequestBody String selectQuery) {
        try {
            String insertStatement = sqlGeneratorService.generateInsertStatement(selectQuery);
            return ResponseEntity.ok(insertStatement);
        } catch (SQLException e) {
            return ResponseEntity.badRequest().body("Error generating INSERT statement: " + e.getMessage());
        }
    }

    @PostMapping("/both")
    public ResponseEntity<SQLStatements> generateBothStatements(@RequestBody String selectQuery) {
        try {
            SQLStatements statements = sqlGeneratorService.generateBothStatements(selectQuery);
            return ResponseEntity.ok(statements);
        } catch (SQLException e) {
            return ResponseEntity.badRequest().body(new SQLStatements("Error", "Error: " + e.getMessage()));
        }
    }
}