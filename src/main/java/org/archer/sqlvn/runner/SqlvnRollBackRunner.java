package org.archer.sqlvn.runner;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.archer.sqlvn.config.SqlvnConfig;
import org.archer.sqlvn.config.SqlvnConfig.Action;
import org.archer.sqlvn.service.impl.SqlBackupServiceImpl;
import org.archer.sqlvn.service.impl.SqlPathServiceImpl;
import org.archer.sqlvn.utils.FileSqlvnRenameUtil;
import org.archer.sqlvn.utils.SqlPathGenerator;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class SqlvnRollBackRunner implements ApplicationRunner {
	
	private final SqlvnConfig SqlvnConfig;
	
    private final SqlPathGenerator sqlPathGenerator;
    
    private final DataSource dataSource;
    @Override
    public void run(ApplicationArguments args) throws Exception {
    	
        
     // 条件1: 检查命令行参数
        if (SqlvnConfig.getAction().equals(Action.rollback)) {
        	log.info("Starting SQLVN rollback process...");
        }else {
        	log.debug("回滚功能未启用,如要启用，请在配置中设置spring.sqlvn.action:rollback");
        	return;
        }
        
        String rollbackVersion = SqlvnConfig.getRollbackVersion(); 
     // 条件2: 检查命令行参数
        if (!StringUtils.hasText(rollbackVersion)) {
        	log.error("未配置子版本号,请在配置中设置spring.sqlvn.rollback-version");
            return;
        }
        String versionTimestamp = SqlvnConfig.getVersion()+"_"+rollbackVersion;
        Path rollbackVersionPath = sqlPathGenerator.getRollbackVersionPath(rollbackVersion);
        //回滚表名 不与迁移表名一致
        String sTablePrefix = SqlvnConfig.getRollbackTablePrefix();
        if(!StringUtils.hasText(sTablePrefix))
        	sTablePrefix = "SQLVN_ROLLBACK_";
        
        if (Files.isDirectory(rollbackVersionPath)) {
        	log.info("回滚版本路径为{},有效路径",rollbackVersionPath);
        } else {
        	log.error("回滚版本路径为{},无效路径,停止回滚。",rollbackVersionPath);
        	return ;
        }
        //将ddl文件版本化
        FileSqlvnRenameUtil.renameAllFileByPath(rollbackVersionPath.toString());

        // 2. 创建 Flyway 配置
		FluentConfiguration config = Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:"+rollbackVersionPath) // SQL 文件路径
                .baselineOnMigrate(true)     // 如果没有基线表，自动创建
                .table(sTablePrefix+versionTimestamp)     // 迁移记录表名
                .sqlMigrationPrefix("V")     // SQL 文件前缀
                .sqlMigrationSeparator("__") // 分隔符
                .sqlMigrationSuffixes(".sql"); // 文件后缀

        // 3. 执行迁移
        Flyway flyway = config.load();
        flyway.migrate(); // 执行 SQL 文件

        log.info("rollback 完成！");
    }
    
    private boolean isBackupEnabledByArgs(ApplicationArguments args) {
        // 从命令行参数读取
        return !args.containsOption("skipBackup");
    }
}