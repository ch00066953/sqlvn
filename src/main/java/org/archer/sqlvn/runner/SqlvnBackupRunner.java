package org.archer.sqlvn.runner;

import java.nio.file.Path;
import java.util.List;

import org.archer.sqlvn.service.impl.SqlBackupServiceImpl;
import org.archer.sqlvn.service.impl.SqlPathServiceImpl;
import org.archer.sqlvn.utils.SqlPathGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(0)
public class SqlvnBackupRunner implements ApplicationRunner {
	
	@Value("${spring.sqlvn.backup-enabled:false}")
    private boolean backupEnabled;
	
    private final SqlBackupServiceImpl backupService;
    private final SqlPathServiceImpl pathService;
    private final SqlPathGenerator sqlPathGenerator;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
    	
        log.info("Starting SQL backup process...");
        
     // 条件1: 检查命令行参数
        if (!backupEnabled) {
        	log.info("备份功能未启用，请在配置中设置sqlvn.backup-enabled=true");
            return;
        }
        
     // 条件2: 检查命令行参数
        if (!isBackupEnabledByArgs(args)) {
        	log.info("备份功能跳过，skipBackup");
            return;
        }
        
        List<Path> dqlFiles = pathService.getAllDqlFiles();
        if (dqlFiles.isEmpty()) {
        	log.info("No DQL files found for backup");
            return;
        }
        
        log.info("Found " + dqlFiles.size() + " DQL files to backup");
        
        for (Path dqlFile : dqlFiles) {
            try {
                log.info("Backing up: " + dqlFile);
                backupService.backupSqlFile(dqlFile);
                log.info("Backup created for: " + dqlFile);
            } catch (Exception e) {
            	log.error("Failed to backup: " + dqlFile,e);
            }
        }
        
        log.info("SQL backup process completed");
    }
    
    private boolean isBackupEnabledByArgs(ApplicationArguments args) {
        // 从命令行参数读取
        return !args.containsOption("skipBackup");
    }
}