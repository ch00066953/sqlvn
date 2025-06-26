package org.archer.sqlvn.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CharsetUtil {
 
	/**
	 * 如果文件是utf8编码返回true,反之false
	 * @param file
	 * @return
	 */
	public static Boolean isUtf8(File file) {
		boolean isUtf8 = true;
		byte[] buffer = readByteArrayData(file);
		int end = buffer.length;
		for (int i = 0; i < end; i++) {
			byte temp = buffer[i];
			if ((temp & 0x80) == 0) {// 0xxxxxxx
				continue;
			} else if ((temp & 0xC0) == 0xC0 && (temp & 0x20) == 0) {// 110xxxxx 10xxxxxx
				if (i + 1 < end && (buffer[i + 1] & 0x80) == 0x80 && (buffer[i + 1] & 0x40) == 0) {
					i = i + 1;
					continue;
				}
			} else if ((temp & 0xE0) == 0xE0 && (temp & 0x10) == 0) {// 1110xxxx 10xxxxxx 10xxxxxx
				if (i + 2 < end && (buffer[i + 1] & 0x80) == 0x80 && (buffer[i + 1] & 0x40) == 0
						&& (buffer[i + 2] & 0x80) == 0x80 && (buffer[i + 2] & 0x40) == 0) {
					i = i + 2;
					continue;
				}
			} else if ((temp & 0xF0) == 0xF0 && (temp & 0x08) == 0) {// 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
				if (i + 3 < end && (buffer[i + 1] & 0x80) == 0x80 && (buffer[i + 1] & 0x40) == 0
						&& (buffer[i + 2] & 0x80) == 0x80 && (buffer[i + 2] & 0x40) == 0
						&& (buffer[i + 3] & 0x80) == 0x80 && (buffer[i + 3] & 0x40) == 0) {
					i = i + 3;
					continue;
				}
			}
			isUtf8 = false;
			break;
		}
		return isUtf8;
	}
 
	/**
	 * 如果文件是gbk编码或者gb2312返回true,反之false
	 * @param file
	 * @return
	 */
	public static Boolean isGbk(File file) {
		boolean isGbk = true;
		byte[] buffer = readByteArrayData(file);
		int end = buffer.length;
		for (int i = 0; i < end; i++) {
			byte temp = buffer[i];
			if ((temp & 0x80) == 0) {
				continue;// B0A1-F7FE//A1A1-A9FE
			} else if ((Byte.toUnsignedInt(temp) < 0xAA && Byte.toUnsignedInt(temp) > 0xA0)
					|| (Byte.toUnsignedInt(temp) < 0xF8 && Byte.toUnsignedInt(temp) > 0xAF)) {
				if (i + 1 < end) {
					if (Byte.toUnsignedInt(buffer[i + 1]) < 0xFF && Byte.toUnsignedInt(buffer[i + 1]) > 0xA0
							&& Byte.toUnsignedInt(buffer[i + 1]) != 0x7F) {
						i = i + 1;
						continue;
					}
				} // 8140-A0FE
			} else if (Byte.toUnsignedInt(temp) < 0xA1 && Byte.toUnsignedInt(temp) > 0x80) {
				if (i + 1 < end) {
					if (Byte.toUnsignedInt(buffer[i + 1]) < 0xFF && Byte.toUnsignedInt(buffer[i + 1]) > 0x3F
							&& Byte.toUnsignedInt(buffer[i + 1]) != 0x7F) {
						i = i + 1;
						continue;
					}
				} // AA40-FEA0//A840-A9A0
			} else if ((Byte.toUnsignedInt(temp) < 0xFF && Byte.toUnsignedInt(temp) > 0xA9)
					|| (Byte.toUnsignedInt(temp) < 0xAA && Byte.toUnsignedInt(temp) > 0xA7)) {
				if (i + 1 < end) {
					if (Byte.toUnsignedInt(buffer[i + 1]) < 0xA1 && Byte.toUnsignedInt(buffer[i + 1]) > 0x3F
							&& Byte.toUnsignedInt(buffer[i + 1]) != 0x7F) {
						i = i + 1;
						continue;
					}
				}
			}
			isGbk = false;
			break;
		}
		return isGbk;
	}
	
	/**
	 * 从文件中直接读取字节
	 * @param file
	 * @return
	 */
	public static byte[] readByteArrayData(File file) {
		byte[] rebyte = null;
		BufferedInputStream bis;
		ByteArrayOutputStream output;
		try {
			bis = new BufferedInputStream(new FileInputStream(file));
			output = new ByteArrayOutputStream();
			byte[] byt = new byte[1024 * 4];
			int len;
			try {
				while ((len = bis.read(byt)) != -1) {
					if (len < 1024 * 4) {
						output.write(byt, 0, len);
					} else
						output.write(byt);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			rebyte = output.toByteArray();
			if (bis != null) {
				bis.close();
			}
			if (output != null) {
				output.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
 
		return rebyte;
	}
 
}