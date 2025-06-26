package org.archer.sqlvn.config;

import org.archer.sqlvn.utils.SqlPathGenerator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "spring.sqlvn")
@Data
public class SqlvnConfig {
	@Getter @Setter
    private String version;
	@Getter @Setter
    private String sqlQueryLocations;
	@Getter @Setter
    private String sqlBakLocations;
	@Getter @Setter
	private String sqlMigrationLocations;
	@Getter @Setter
	private String sqlQueryPrefix;
	@Getter @Setter
	private String sqlQuerySuffixes;
	@Getter @Setter
	private Action action = Action.migration;
	@Getter @Setter
	private String rollbackVersion;
	@Getter @Setter
	private String rollbackTablePrefix;
    
    
    @Bean
    public SqlPathGenerator sqlPathGenerator() {
        return new SqlPathGenerator(
            version,
            sqlQueryLocations,
            sqlBakLocations,
            sqlMigrationLocations,
            sqlQueryPrefix,
            sqlQuerySuffixes
        );
    }
    
    public enum Action {
        rollback, migration, check
    }
}