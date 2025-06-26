package org.archer.sqlvn.component;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "spring.flyway")
@Data
public class FlywayProperties {
    private boolean enabled = true;
    private boolean baselineOnMigrate = false;
    private String[] locations = new String[]{"classpath:db/migration"};
    private String table = "flyway_schema_history";
    private String baselineVersion = "1";
    private String baselineDescription = "Initial baseline";
    private String encoding = "UTF-8";
    private boolean cleanDisabled = true;
    private boolean cleanOnValidationError = false;
    private boolean group = false;
    private String installedBy;
    private boolean mixed = false;
    private boolean outOfOrder = false;
    private boolean skipDefaultCallbacks = false;
    private boolean skipDefaultResolvers = false;
    private String sqlMigrationPrefix = "V";
    private String sqlMigrationSeparator = "__";
    private String[] sqlMigrationSuffixes = new String[]{".sql"};
    private String target;
    private String url;
    private String user;
    private String password;
    private Map<String, String> placeholders = new HashMap<>();
    private String placeholderPrefix = "${";
    private String placeholderSuffix = "}";
    private boolean placeholderReplacement = true;
    private Resource[] jars;
    private String[] schemas;
    private String defaultSchema;
    private boolean createSchemas = true;
    private boolean validateOnMigrate = true;
    private boolean ignoreMissingMigrations = false;
    private boolean ignoreIgnoredMigrations = false;
    private boolean ignorePendingMigrations = false;
    private boolean ignoreFutureMigrations = false;
    private boolean validateMigrationNaming = false;
    private boolean failOnMissingLocations = false;
    private List<String> callbacks;

}