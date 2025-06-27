package org.archer.sqlvn.runner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.archer.sqlvn.service.impl.SqlBackupServiceImpl;
import org.archer.sqlvn.service.impl.SqlPathServiceImpl;
import org.archer.sqlvn.utils.FileSqlvnRenameUtil;
import org.archer.sqlvn.utils.SqlPathGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Order(1)
@RequiredArgsConstructor
public class SqlvnMigFileGenerateRunner implements ApplicationRunner {
	
	@Value("${spring.sqlvn.genmigsql-enabled:false}")
    private boolean genEnabled;
	
    private final SqlBackupServiceImpl backupService;
    private final SqlPathServiceImpl pathService;
    private final SqlPathGenerator sqlPathGenerator;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
    	
        log.info("Starting SQL backup process...");
        
     // 条件1: 检查命令行参数
        if (!genEnabled) {
        	log.info("迁移sql功能未启用，请在配置中设置sqlvn.genmigsql-enabled=true");
            return;
        }
        
     // 条件2: 检查命令行参数
        if (!isGenerateMigsqlEnabledByArgs(args)) {
        	log.info("备份功能跳过，GenMigSQL");
            return;
        }
        
        List<Path> dqlFiles = pathService.getAllDqlFiles();
        if (dqlFiles.isEmpty()) {
        	log.info("No DQL files found for GenMigSQL");
            return;
        }
        
        log.info("Found " + dqlFiles.size() + " DQL files to GenMigSQL");
        
        FileSqlvnRenameUtil.sBaseVersionNo = FileSqlvnRenameUtil.VERSION_V + sqlPathGenerator.getVersion();
        
        if (Files.exists(sqlPathGenerator.generateMigrationVersionPath())) 
        	FileSqlvnRenameUtil.revertAllFileByPath(sqlPathGenerator.generateMigrationVersionPath().toString());
        
        for (Path dqlFile : dqlFiles) {
            try {
            	log.info("GenMigSQL up: " + dqlFile);
                backupService.genMigSqlFile(dqlFile);
                log.info("GenMigSQL created for: " + dqlFile);
            } catch (Exception e) {
            	log.error("Failed to GenMigSQL: " + dqlFile,e);
            }
        }
        FileSqlvnRenameUtil.renameAllFileByPath(sqlPathGenerator.generateMigrationVersionPath().toString());

        
        log.info("SQL backup process completed");
    }
    
    private boolean isGenerateMigsqlEnabledByArgs(ApplicationArguments args) {
    	// 从命令行参数读取
    	return !args.containsOption("skipGenMigsql");
    }
}