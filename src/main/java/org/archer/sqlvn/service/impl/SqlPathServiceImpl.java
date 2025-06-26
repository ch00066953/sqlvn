package org.archer.sqlvn.service.impl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.archer.sqlvn.config.SqlvnConfig;
import org.archer.sqlvn.utils.SqlPathGenerator;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqlPathServiceImpl {
    private final SqlvnConfig sqlConfig;
    @Autowired
    private Flyway flyway;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private final SqlPathGenerator sqlPathGenerator;
    
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    
    /**
     * 	获取指定版本的所有DQL文件路径
     */
    public List<Path> getAllDqlFiles() throws IOException {
        Path versionPath = getVersionQueryPath();
        try (Stream<Path> paths = Files.walk(versionPath)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().startsWith(sqlConfig.getSqlQueryPrefix()))
                .filter(p -> p.getFileName().toString().endsWith(sqlConfig.getSqlQuerySuffixes()))
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 	生成备份路径
     */
    public Path generateBackupPath(Path dqlPath) {
    	long startupTime = applicationContext.getStartupDate();
    	Date startupDate = new Date(startupTime);
    	// 获取当前时间戳
		String timestamp = timestampFormat.format(startupDate);
        log.info("备份用应用启动时间: " + timestamp);
        
        SqlPathGenerator generator = new SqlPathGenerator(
    		sqlConfig.getVersion(), sqlConfig.getSqlQueryLocations(),
    		sqlConfig.getSqlBakLocations(), sqlConfig.getSqlMigrationLocations(),
    		sqlConfig.getSqlQueryPrefix(), sqlConfig.getSqlQuerySuffixes()
        );

        // 测试路径转换
        Path dmlPath = generator.generateDmlBackupPathByTime(dqlPath,timestamp);

        log.info("DQL Path: " + dqlPath);
        log.info("DML Backup Path: " + dmlPath);
        
        return dmlPath;
    }
    
    /**
     * 	生成备份路径
     */
    public Path generateMigPath(Path dqlPath) {

        // 迁移路径转换
        Path dmlPath = sqlPathGenerator.generateMigrationPath(dqlPath);

        log.info("DQL Path: " + dqlPath);
        log.info("DML Backup Path: " + dmlPath);
        
        return dmlPath;
    }
    
    private Path getVersionQueryPath() {
        return Paths.get(sqlConfig.getSqlQueryLocations().replaceFirst("^file:", ""))
            .resolve(flyway.getConfiguration().getSqlMigrationPrefix() + sqlConfig.getVersion());
    }
}