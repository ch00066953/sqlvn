package org.archer.sqlvn.utils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlPathGenerator {
	@Getter
    private final String version;
    private final Path queryRootPath;
    private final Path bakRootPath;
    private final Path migrationRootPath;
    private final String queryPrefix;
    private final String querySuffix;
    private final Pattern versionPattern;
    private final SimpleDateFormat timestampFormat;


    public SqlPathGenerator(String version, 
                          String queryLocations, 
                          String bakLocations,
                          String migrationLocations,
                          String queryPrefix,
                          String querySuffix) {
        this.version = version;
        // 去除路径前的"file:"前缀
        this.queryRootPath = Paths.get(queryLocations.replaceFirst("^file:", ""));
        this.bakRootPath = Paths.get(bakLocations.replaceFirst("^file:", ""));
        this.migrationRootPath = Paths.get(migrationLocations.replaceFirst("^file:", ""));
        this.versionPattern = Pattern.compile(".*V" + Pattern.quote(version) + ".*");
        this.queryPrefix = queryPrefix;
        this.querySuffix = querySuffix;
        this.timestampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    }
    
    /**
     * 验证DQL路径中是否包含正确的版本号
     */
    private void validateVersionInPath(Path dqlPath) throws IllegalArgumentException {
        String pathStr = dqlPath.toString();
        if (!versionPattern.matcher(pathStr).matches()) {
            throw new IllegalArgumentException(
                "DQL路径中不包含正确的版本号(V" + version + "): " + dqlPath);
        }
    }

	/**
	 * 根据DQL文件路径生成对应的DML备份文件路径
	 * 
	 * @param dqlPath 原始DQL文件路径（应包含版本号）
	 * @return 对应的DML备份文件路径（保持版本号结构）
	 */
	public Path generateDmlBackupPath(Path dqlPath) {

		// 获取当前时间戳
		String timestamp = timestampFormat.format(new Date());

		// 构建完整路径: bak根目录/版本文件夹/相对路径(父目录)/dml文件名
		return generateDmlBackupPathByTime(dqlPath, timestamp);
	}
	

	/**
	 * 根据DQL文件路径生成对应的DML备份文件路径
	 * 
	 * @param dqlPath 原始DQL文件路径（应包含版本号）
	 * @return 对应的DML备份文件路径（保持版本号结构）
	 */
	public Path generateDmlBackupPathByTime(Path dqlPath,String timestamp) {
		validateVersionInPath(dqlPath);

		// 获取相对于查询根目录的相对路径（包含版本文件夹）
		Path relativePath = queryRootPath.relativize(dqlPath);

		// 替换文件名前缀从dql到dml
		String fileName = relativePath.getFileName().toString();
		String dmlFileName = fileName.replaceFirst("^" + queryPrefix, "dml");

		// 构建完整路径: bak根目录/版本文件夹/相对路径(父目录)/dml文件名
		return bakRootPath.resolve(relativePath.getParent()).resolve(timestamp) // 添加时间戳文件夹
				.resolve(dmlFileName);
	}
    /**
     * 根据DQL文件路径生成对应的迁移文件路径
     * @param dqlPath 原始DQL文件路径（应包含版本号）
     * @return 对应的迁移文件路径（保持版本号结构）
     */
    public Path generateMigrationPath(Path dqlPath) {
        validateVersionInPath(dqlPath);
        
        Path relativePath = queryRootPath.relativize(dqlPath);
        
        String fileName = relativePath.getFileName().toString();
        String migrationFileName = fileName.replaceFirst("^" + queryPrefix, "dml");
        
        return migrationRootPath.resolve(relativePath.getParent())
                               .resolve(migrationFileName);
    }
    
    /**
     *  	获取迁移版本基本路径
     */
    public Path generateMigrationVersionPath() {
        
        return migrationRootPath.resolve(getVersionFolderPath());
    }
    
    /**
     *  	获取迁移版本基本路径,用于回滚代码
     */
    public Path getRollbackVersionPath(String versionTimestamp) {
        
        return bakRootPath.resolve(getVersionFolderPath()).resolve(versionTimestamp);
    }
    
    /**
     * 	获取版本文件夹路径(V + 版本号)
     */
    public Path getVersionFolderPath() {
        return Paths.get("V" + version);
    }

    // 示例使用
    public static void main(String[] args) {
        // 模拟YAML配置
        String version = "2025.02";
        String queryLocations = "file:D:\\workspace\\HYworkspace\\sqlvn\\src\\main\\resources\\sqlvn\\query";
        String bakLocations = "file:D:\\workspace\\HYworkspace\\sqlvn\\src\\main\\resources\\sqlvn\\bak";
        String migrationLocations = "file:D:\\workspace\\HYworkspace\\sqlvn\\src\\main\\resources\\sqlvn\\migration";
        String queryPrefix = "dql";
        String querySuffix = ".sql";

        SqlPathGenerator generator = new SqlPathGenerator(
            version, queryLocations, bakLocations, migrationLocations, queryPrefix, querySuffix
        );

        // 测试路径转换
        Path dqlPath = Paths.get("D:\\workspace\\HYworkspace\\sqlvn\\src\\main\\resources\\sqlvn\\query\\V2025.02\\dql_20250623_测试生成_lgwang.sql");
        Path dmlPath = generator.generateDmlBackupPath(dqlPath);
        Path migrationPath = generator.generateMigrationPath(dqlPath);

        log.info("DQL Path: " + dqlPath);
        log.info("DML Backup Path: " + dmlPath);
        log.info("Migration Path: " + migrationPath);
    }
}