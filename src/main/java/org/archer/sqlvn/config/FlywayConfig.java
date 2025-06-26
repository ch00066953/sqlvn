package org.archer.sqlvn.config;

import javax.sql.DataSource;

import org.archer.sqlvn.component.FlywayProperties;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FlywayConfig {

	@Bean
    public Flyway flyway(DataSource dataSource, FlywayProperties properties) {
		FluentConfiguration configuration = Flyway.configure()
	            .dataSource(dataSource);
	        
	        // 使用Spring Boot的PropertyMapper实现与自动配置完全一致的属性绑定
	        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
	        
	        // 基础配置
	        map.from(properties::getBaselineDescription).to(configuration::baselineDescription);
	        map.from(properties::getBaselineVersion).to(configuration::baselineVersion);
	        map.from(properties::isBaselineOnMigrate).to(configuration::baselineOnMigrate);
	        map.from(properties::isValidateOnMigrate).to(configuration::validateOnMigrate);
	        map.from(properties::isCleanDisabled).to(configuration::cleanDisabled);
	        map.from(properties::isCleanOnValidationError).to(configuration::cleanOnValidationError);
	        map.from(properties::getEncoding).to(configuration::encoding);
	        map.from(properties::isGroup).to(configuration::group);
	        map.from(properties::getInstalledBy).to(configuration::installedBy);
	        map.from(properties::getTable).to(configuration::table);
	        
	        // 占位符配置（完全兼容Spring Boot处理方式）
	        map.from(properties::isPlaceholderReplacement).to(configuration::placeholderReplacement);
	        map.from(properties::getPlaceholderPrefix).to(configuration::placeholderPrefix);
	        map.from(properties::getPlaceholderSuffix).to(configuration::placeholderSuffix);
	        map.from(properties::getPlaceholders).to(configuration::placeholders);
	        
	        // 迁移脚本配置
	        if (properties.getLocations() != null) {
	            configuration.locations(properties.getLocations());
	        }
	        
	        // 回调配置（与Spring Boot一致的处理）
	        if (properties.getCallbacks() != null) {
	            configuration.callbacks(properties.getCallbacks().toArray(new Callback[0]));
	        }
	        
	        // 模式配置
	        if (properties.getSchemas() != null) {
	            configuration.schemas(properties.getSchemas());
	        }
	        
	        return configuration.load();
    }

    @Bean
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway, f -> {
            // 空实现，不自动执行迁移
            log.debug("Flyway 8.5+ 已初始化，但跳过自动迁移");
        });
    }
}