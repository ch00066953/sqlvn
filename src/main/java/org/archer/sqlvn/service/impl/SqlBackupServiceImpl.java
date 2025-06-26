package org.archer.sqlvn.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.archer.sqlvn.utils.DQLtoDMLConverterWithComments;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqlBackupServiceImpl {
    private final SqlPathServiceImpl pathService;
    private final DataSource dataSource;
    
    /**
     * 	执行单个SQL文件备份
     */
    public void backupSqlFile(Path dqlPath,String timestamp) throws IOException, SQLException {
        Path backupPath = pathService.generateBackupPath(dqlPath);
        Files.createDirectories(backupPath.getParent());
        
        log.info("DML Backup Path: " + backupPath);
        
        try (Connection conn = dataSource.getConnection()) {
            DQLtoDMLConverterWithComments converter = new DQLtoDMLConverterWithComments(conn);
            converter.convertFile(dqlPath.toString(), backupPath.toString());
            log.info("Conversion completed successfully. Output saved to: " + backupPath.toString());
        } catch (Exception e) {
        	log.error("Error during conversion: " + e.getMessage(),e);
        	throw e;
        }
    }
    
    /**
     * 	执行单个SQL文件备份
     */
    public void backupSqlFile(Path dqlPath) throws IOException, SQLException {
        Path backupPath = pathService.generateBackupPath(dqlPath);
        Files.createDirectories(backupPath.getParent());
        
        log.info("DML Backup Path: " + backupPath);
        
        try (Connection conn = dataSource.getConnection()) {
            DQLtoDMLConverterWithComments converter = new DQLtoDMLConverterWithComments(conn);
            converter.convertFile(dqlPath.toString(), backupPath.toString());
            log.info("Conversion completed successfully. Output saved to: " + backupPath.toString());
        } catch (Exception e) {
        	log.error("Error during conversion: " + e.getMessage(),e);
        	throw e;
        }
    }
    
    /**
     * 	执行单个SQL文件备份
     */
    public void backupSqlFiles() throws IOException, SQLException {
    	List<Path> dqlFiles = pathService.getAllDqlFiles();
        if (dqlFiles.isEmpty()) {
        	log.info("No DQL files found for backup");
            return;
        }
        
        log.info("Found " + dqlFiles.size() + " DQL files to backup");
        
        for (Path dqlFile : dqlFiles) {
            try {
                log.info("Backing up: " + dqlFile);
                backupSqlFile(dqlFile);
                log.info("Backup created for: " + dqlFile);
            } catch (Exception e) {
            	log.error("Failed to backup: " + dqlFile,e);
            }
        }
    }

    /**
     * 	执行单个SQL文件备份
     */
    public void genMigSqlFile(Path dqlPath) throws IOException, SQLException {
        Path migPath = pathService.generateMigPath(dqlPath);
        Files.createDirectories(migPath.getParent());
        
        log.info("DML GeneratorMig Path: " + migPath);
        
        try (Connection conn = dataSource.getConnection()) {
            DQLtoDMLConverterWithComments converter = new DQLtoDMLConverterWithComments(conn);
            converter.convertFile(dqlPath.toString(), migPath.toString());
            log.info("Conversion completed successfully. Output saved to: " + migPath.toString());
        } catch (Exception e) {
        	log.error("Error during conversion: " + e.getMessage(),e);
        	throw e;
        }
    }
}