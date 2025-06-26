package org.archer.sqlvn.component;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MigrationFilePaths {

	
	@Value("${spring.flyway.locations}")
    private String locations;
	
    private final ResourcePatternResolver resourcePatternResolver;

    public MigrationFilePaths(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<File> printAllMigrationFilePaths() throws IOException {
    	List<File>  fileList = new ArrayList<>();
        // 获取 classpath:sqlvn/migration/ 下的所有文件
        Resource[] resources = resourcePatternResolver.getResources(locations+"/**");
        
        for (Resource resource : resources) {
            // 获取文件的URL（全路径）
            URL fileUrl = resource.getURL();
            fileList.add(resource.getFile());
            log.info("File path: " + fileUrl.toString());
        }
		return fileList;
    }
}