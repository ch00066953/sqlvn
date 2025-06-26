package org.archer.sqlvn.runner;

import org.archer.sqlvn.config.SqlvnConfig.Action;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Order(9)
public class FlywayRunner implements ApplicationRunner {
    
    @Autowired
    private Flyway flyway;
    @Value("${spring.sqlvn.action:migration}")
    private Action action;
    @Value("${spring.sqlvn.migration-enabled:false}")
    private boolean migEnabled;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
    	
    	// 条件1: 检查命令行参数
        if (!action.equals(Action.migration)) {
        	log.debug("迁移功能未启用,如要启用，请在配置中设置spring.sqlvn.action:migration");
        	return;
        }
     // 条件1: 检查命令行参数
        if (!migEnabled) {
        	log.info("迁移功能未启用，请在配置中设置sqlvn.migration-enabled=true");
            return;
        }
        
        // 条件2: 检查命令行参数
        if (!isMigrateEnabledByArgs(args)) {
            return;
        }
        
        // 应用完全启动后执行迁移
        log.info("应用启动完成，开始执行Flyway数据库迁移...");
//        flyway.
        if(flyway.getConfiguration().isBaselineOnMigrate())
        	flyway.baseline();
        MigrateResult migrate = flyway.migrate();
        log.info("数据库迁移完成");
    }
    

    private boolean isMigrateEnabledByArgs(ApplicationArguments args) {
        // 从命令行参数读取
        return !args.containsOption("skipMigrate");
    }
}