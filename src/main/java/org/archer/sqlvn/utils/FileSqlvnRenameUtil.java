package org.archer.sqlvn.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 合并sql使用，生成不部署该代码
 */
@Slf4j
public class FileSqlvnRenameUtil {
	
	public static final String sqlType_DDL = "ddl";
	public static final String sqlType_DML = "dml";
	public static final String sqlType_ALL = "all";
	public static final String sqlType_INV = "invalid";
	
	public static final String VersionType_DDL = "01";
	public static final String VersionType_DML = "02";
	public static final String VersionType_INV = "99";
	

	public static final String VERSION_V = "V";
	public static final String VERSION_R = "R";
	
	public static String sBaseVersionNo = "V2025.01";

	public static String sBaseVersionPath = "D:\\workspace\\HYworkspace\\sqlvn\\src\\main\\resources\\sqlvn\\migration\\V2025.01";

	public static int iMaxDdlCnt = 0;
	public static int iMaxDmlCnt = 0;
	

	public static String separator = "__";
	
	public static void main(String[] args) throws Exception {
		renameAllFileByPath(sBaseVersionPath);
	}

	/**
	 * 重命名
	 * @param oldName
	 * @param newName
	 */
	public static void rename(String oldName ,String newName) {
		Path source = Paths.get(oldName);
        Path target = Paths.get(newName);

        try {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING // 如果目标文件存在，则覆盖
            );
            log.info("文件重命名成功！"+target);
        } catch (Exception e) {
        	log.error("文件重命名失败：" + e.getMessage());
        }
	}
	
	/**
	 * 重命名
	 * @param oldName
	 */
	public String getNewNameByRule(String oldName) {
		String sNameType = checkName(oldName);
//		if(1==1)
		
		return oldName;
	}
	/**
	 * 获取文件
	 * @return
	 * @throws Exception 
	 */
	private static List<File> getFileList(String basePath) throws Exception {
		File f = new File(basePath);
        List<File> allFiles = FileSqlMergeUtil.getAllFiles(f);
		return allFiles;
	}
	
	/**
	 * 检查文件命是否需改更名正确
	 */
	private static String checkName(String sFileName) {
		if(sFileName.endsWith(".sql")) {
			if(sFileName.startsWith(sqlType_DDL))
				return VersionType_DDL;
			else if (sFileName.startsWith(sqlType_DML))
				return VersionType_DML;
			else
				return VersionType_INV;
		} else
			return VersionType_INV;
	}
	
	/**
	 * 重命名所有非版本管理规范文件
	 * @param oldName
	 * @param newName
	 * @throws Exception 
	 */
	public static void renameAllFileByPath(String basePath) throws Exception {
		
		List<File> allFiles = getFileList(basePath);
		
		renameAllFiles(allFiles);
	}

	protected static void renameAllFiles(List<File> allFiles) {
		//初始化
		List<String> allFileNames = new ArrayList<>();
		for(File file : allFiles) {
			allFileNames.add(file.getName());
		}
		initMxSubVersion(allFileNames);
		int iDdlCnt = iMaxDdlCnt;
		int iDmlCnt = iMaxDmlCnt;
		
		DecimalFormat df = new DecimalFormat("000");
		
		String newFileName = "";
		for(File file : allFiles) {
        	log.debug(file.getName()+",size:"+file.length());
        	String checkName = checkName(file.getName());
        	String basePath = file.getParent();
        	
        	if(checkName.equals(VersionType_DDL)) {
        		
        		iDdlCnt ++;
        		String sVersionCnt = df.format(iDdlCnt);
        		newFileName = sBaseVersionNo + "." + checkName + "." + sVersionCnt + separator + file.getName();
        		rename(file.getPath(),basePath+"/"+newFileName);
        	}
        	
        	if(checkName.equals(VersionType_DML)) {
        		
        		iDmlCnt ++;
        		String sVersionCnt = df.format(iDmlCnt);
        		newFileName = sBaseVersionNo + "." + checkName + "." + sVersionCnt + separator + file.getName();
        		rename(file.getPath(),basePath+"/"+newFileName);
        	}
        	if(checkName.equals(VersionType_INV)) {
        		continue;
        	}
        }
	}
	
	/**
	 * 获取最大版本号
	 * @param sVersionFileNames
	 * @return
	 */
	public static int getMaxSubVersion(List<String> sVersionFileNames) {
		Optional<String> max = sVersionFileNames.stream().max(Comparator.naturalOrder());
		String maxName = "";
		String maxSubVersion = "000";
		
		// 安全处理
		if (max.isPresent()) {
			maxName = max.get();
			if(maxName.contains(separator)) {
				maxName = maxName.substring(0, maxName.indexOf(separator));
			}
			if(StringUtils.hasText(maxName)) {
				String[] split = maxName.split("\\.");
				if( split.length > 0 )
					maxSubVersion = split[split.length-1];
			}
		} else {
		    log.info("sVersionFileNames列表为空");
		}

		return Integer.valueOf(maxSubVersion);
	}
	

	/**
	 * 获取最大版本号
	 * @param sVersionFileNames
	 * @return
	 */
	public static void initMxSubVersion(List<String> sVersionFileNames) {
		List<String> ddlVersionFileNames = new ArrayList<String>();
		List<String> dmlVersionFileNames = new ArrayList<String>();
		for(String sVersionFileName :sVersionFileNames) {
			if(sVersionFileName.startsWith(sBaseVersionNo+"."+VersionType_DDL)) {
				ddlVersionFileNames.add(sVersionFileName);
			}
			if(sVersionFileName.startsWith(sBaseVersionNo+"."+VersionType_DML)) {
				dmlVersionFileNames.add(sVersionFileName);
			}
		}
		iMaxDdlCnt = getMaxSubVersion(ddlVersionFileNames);
		log.info("iMaxDdlCnt:"+iMaxDdlCnt);
		iMaxDmlCnt = getMaxSubVersion(dmlVersionFileNames);
		log.info("iMaxDmlCnt:"+iMaxDmlCnt);
		
	}
	
	/**
	 * 恢复路径下所有名称
	 * @param basePath
	 * @throws Exception 
	 */
	public static void revertAllFileByPath(String basePath) throws Exception {
		log.debug("恢复路径【{}】下所有名称，去除版本编号",basePath);
		List<File> allFiles = getFileList(basePath);
		
		for(File file : allFiles) {
        	log.debug(file.getName()+",size:"+file.length());
        	
        	String revertFileName = revertFileName(file.getName());
        	rename(file.getPath(),basePath+"/"+revertFileName);
        }
	}
	
	/**
	 * 恢复名称
	 * 如：将V2025.01.02.001__dml_01_20250617_zhren_银团还款信息调整.sql 的V2025.01.02.001__去掉，规则是将V和__之间的内容去掉，剩下dml_01_20250617_zhren_银团还款信息调整.sql
	 * @param fileName
	 * @return
	 */
	public static String revertFileName(String fileName) {
		// 使用正则表达式匹配 V...__ 的模式并替换为空
		Pattern pattern = Pattern.compile(VERSION_V + ".*?"+separator);
		Matcher matcher = pattern.matcher(fileName);

		// 如果找到匹配项，则替换为空字符串
		if (matcher.find()) {
			return matcher.replaceFirst("");
		}

		// 如果没有匹配项，返回原文件名
		return fileName;
	}
}
