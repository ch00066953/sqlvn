package org.archer.sqlvn.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 合并sql使用，生成不部署该代码
 */
@Slf4j
public class FileSqlMergeUtil {
	
	public static final String sqlType_DDL = "ddl";
	public static final String sqlType_DML = "dml";
	public static final String sqlType_ALL = "all";

	public static void main(String[] args) throws Exception {
		String basePath = "D:\\项目\\项目\\海翼\\doc\\增量版本\\4.票据四期\\sql\\待上线\\dml";
		String resultPath = basePath + "\\result";
//        List<String> list = new ArrayList<>();
//        list.add("D:\\项目\\项目\\海翼\\doc\\增量版本\\5.脱敏五期\\sql\\ce\\ddl_20230822_lgwang_脱敏字段加密长度增加.sql");
//        list.add("D:\\项目\\项目\\海翼\\doc\\增量版本\\5.脱敏五期\\sql\\ce\\ddl_20231013_zhren_数据脱敏.sql");
        String targetName = "dml_20231109_bill_fileNum.sql";
        File f = new File(basePath);
        File resultf = new File(resultPath);
        if(!resultf.exists()) {
        	resultf.mkdir();
        }
        List<File> allFiles = getAllFiles(f);

        String targetBasePath = resultPath + "\\" +targetName;
        String targetPath = targetBasePath;
        log.debug(targetPath);
        int iFileNum = 1; 
        for(File file : allFiles) {
        	log.debug(file.getName()+",size:"+file.length());
        	if(file.length() > 100000) {
        		log.debug(file.getName()+",size:"+file.length());
        		iFileNum ++;
        	}
        	targetPath = targetBasePath.replaceFirst("fileNum", String.valueOf(iFileNum));
        	appendFile(file.getPath(), targetPath);
        	File targetFile = new File(targetPath);
        	log.debug("输出文件：" + targetFile.getName()+",size:"+targetFile.length());
        	if(targetFile.length() > 100000) {
        		iFileNum ++;
        	}
        }
        
	}
	/**
	 * 单个文件追加
	 * @param inputFile1 	输入文件
	 * @param outputFile	输出文件
	 */
	public static void appendFile(String inputFile1 ,String outputFile) {
		try {
			File iFile = new File(inputFile1);
			File oFile = new File(outputFile);
            // 创建输入流读取文件1
            BufferedReader reader1 = new BufferedReader(new FileReader(inputFile1));
            // 创建输出流写入目标文件
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
            String line;
            // 逐行读取文件1的内容，并写入目标文件
            writer.write("-----"+ iFile.getName() + " begin -----");
            writer.newLine();
            while ((line = reader1.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            writer.write("-----"+ iFile.getName() + " end -----");
            writer.newLine();
            writer.newLine();
            // 关闭输入流和输出流
            reader1.close();
            writer.close();
            log.debug(iFile.getName() + "-->" + oFile.getName() + "文件合并成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * 两个文件追加
	 * @param inputFile1	输入文件1
	 * @param inputFile2	输入文件2
	 * @param outputFile	输出文件
	 */
	public static void mergeFile(String inputFile1 ,String inputFile2 ,String outputFile) {
//        String inputFile1 = "file1.txt";
//        String inputFile2 = "file2.txt";
//        String outputFile = "output.txt";
        try {
            // 创建输入流读取文件1
            BufferedReader reader1 = new BufferedReader(new FileReader(inputFile1));
            // 创建输入流读取文件2
            BufferedReader reader2 = new BufferedReader(new FileReader(inputFile2));
            // 创建输出流写入目标文件
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
            String line;
            // 逐行读取文件1的内容，并写入目标文件
            while ((line = reader1.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            // 逐行读取文件2的内容，并写入目标文件
            while ((line = reader2.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            // 关闭输入流和输出流
            reader1.close();
            reader2.close();
            writer.close();
            log.info("文件合并成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	/**
     * 获取指定文件夹下的所有文件
     *
     * @param dirPath 文件夹路径
     * @return 所有文件
     */
    public static List<File> getAllFiles(File dirPath) throws Exception {
        if (!dirPath.isFile() && dirPath.exists()) {
            List<File> fileList = new ArrayList<>();
            return recursion(dirPath, fileList,false);
        } else {
            throw new Exception("请输入正确的文件夹路径");
        }
    }

    /**
     * 递归文件
     * @param file 路径
     * @param fileList	文件列表
     * @param isRecursion	是否使用递归文件夹
     * @return
     */
    private static List<File> recursion(File file, List<File> fileList,boolean isRecursion) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory() ) {
                	if(isRecursion)//递归
                		recursion(f, fileList,isRecursion);
                } else {
                    fileList.add(f);
                }

            }
        }
        return fileList;
    }
    
    /**
     * 打包所有项目
     * @param projectName
     * @param basePath
     * @throws Exception
     */
    public static List<File> AppendAllSQLFile(String projectName,String basePath) throws Exception {
    	// 获取当前日期（格式：yyyy-MM-dd）
        LocalDate currentDate = LocalDate.now();
        String dateString = currentDate.toString(); // 默认格式：2023-10-05
    	
    	String date = dateString.replace("-", "");
    	if(!StringUtils.hasText(projectName))
    		projectName = "als";
    	List<File> allFiles = new ArrayList<>();
    	if(!checkSqlFile(basePath)) {
    		log.error("检查文件不通过！");
    		return allFiles;
    	}else
    		log.info("检查文件通过。");
    	List<File> ddlFiles = AppendSQLFile(sqlType_DDL, projectName, basePath, date);
    	List<File> dmlFiles = AppendSQLFile(sqlType_DML, projectName, basePath, date);
    	allFiles.addAll(ddlFiles);
    	allFiles.addAll(dmlFiles);
    	log.debug("处理ddl文件数：{}",ddlFiles.size());
    	log.debug("处理dml文件数：{}",dmlFiles.size());
    	log.debug("总共处理文件数：{}",allFiles.size());
		return allFiles;
    }
    
    public static List<File> AppendSQLFile(String sqlType,String projectName,String basePath,String date) throws Exception {
    	if(!sqlType.equals(sqlType_DDL) && !sqlType.equals(sqlType_DML)) {
    		throw new Exception("无效sqlType："+sqlType);
    	}
//		String sqlType = "ddl";
//		String projectName = "desen";
//		String basePath = "D:\\项目\\项目\\海翼\\doc\\增量版本\\5.脱敏五期\\sql\\脱敏";
		String resultPath = basePath + "\\" + sqlType;
        String targetName = sqlType + "_20231109_" + projectName + "_fileNum.sql";
        File f = new File(basePath);
        File resultf = new File(resultPath);
        if(!resultf.exists()) {
        	resultf.mkdir();
        }
        List<File> allFiles = FileSqlMergeUtil.getAllFiles(f);
        List<File> appendFiles = new ArrayList<>();
        

        String targetBasePath = resultPath + "\\" +targetName;
        String targetPath = targetBasePath;
        log.debug(targetPath);
        int iFileNum = 1; //文件号数
        for(File file : allFiles) {
        	log.debug(file.getName()+",size:"+file.length());
        	if(!file.getName().startsWith(sqlType)) {
        		log.debug(file.getName()+"非"+sqlType +"文件");
        		continue;
        	}
        	if(file.length() > 100000 && iFileNum != 1) {
        		log.debug(file.getName()+",size:"+file.length());
        		iFileNum ++;
        	}
        	targetPath = targetBasePath.replaceFirst("fileNum", String.valueOf(iFileNum));
        	FileSqlMergeUtil.appendFile(file.getPath(), targetPath);
        	File targetFile = new File(targetPath);

        	//文件统计
        	appendFiles.add(file);
        	log.debug("输出文件：" + targetFile.getName()+",size:"+targetFile.length());
        	if(targetFile.length() > 100000) {
        		iFileNum ++;
        	}
        }
		return appendFiles;
	}
    
    private static boolean checkSqlFile(String basePath) throws Exception {
    	boolean result = true;
    	File f = new File(basePath);
    	List<File> allFiles = FileSqlMergeUtil.getAllFiles(f);
    	for(File file : allFiles) {
    		String codeString = codeString(file);
    		if(!codeString.startsWith("UTF-8")){
    			log.debug("输入文件：" + file.getName() + "是" + codeString + "不是UTF-8格式");
    			result = false;
    		}
//			log.debug("输入文件：" + file.getName() + "是" + codeString );

    		if(!file.getPath().endsWith("sql")){
    			log.debug("输入文件：" + file.getName()+"不是sql文件");
    			result = false;
    		}
    	}
		return result;

	}
    

	public static String detectCharset(String filePath) throws IOException {
		try(InputStream inputStream = new FileInputStream(filePath)) {
			byte[] bytes = new byte[3];
			inputStream.read(bytes) ;
		
			if (bytes[0] == -17 && bytes[1] == -69 && bytes[2] == -65) {
				return "UTF-8";
			} else {
				return "其他编码格式";
			}
		}
	}
	
	/**
	 * 判断文件的编码格式
	 * @param fileName :file
	 * @return 文件编码格式
	 * @throws Exception
	 */
	public static String codeString(File file) throws Exception{
		BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file));
		int p = (bin.read() << 8) + bin.read();
		String code = null;
		switch (p) {
			case 0xefbb:
				code = "UTF-8";
				break;
			case 0xfffe:
				code = "Unicode";
				break;
			case 0xfeff:
				code = "UTF-16BE";
				break;
			default:
				code = "other";
		}
		if(code.equals("other")) {
			if(CharsetUtil.isGbk(file)) {
				code = "GBK";
			}else if(CharsetUtil.isUtf8(file)) {
				code = "UTF-8 NoBom";
			}
		}
		bin.close();
		return code;
	}
	
}
