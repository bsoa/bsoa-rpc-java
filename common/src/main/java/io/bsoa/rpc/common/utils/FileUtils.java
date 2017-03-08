/*
 * Copyright © 2016-2017 The BSOA Project
 *
 * The BSOA Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.bsoa.rpc.common.utils;

import io.bsoa.rpc.exception.BsoaRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Title: 文件操作工具类<br>
 * Created by zhangg on 2016/7/31 18:16. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class FileUtils {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    /**
     * 得到项目所在路径
     *
     * @return 项目所在路径
     */
    public static String getBaseDirName() {
        String fileName = null;
        // 先取classes
        java.net.URL url1 = FileUtils.class.getResource("/");
        if (url1 != null) {
            fileName = url1.getFile();
        } else {
            // 取不到再取lib
            String jarpath = ReflectUtils.getCodeBase(FileUtils.class);
            if (jarpath != null) {
                int jsfidx = jarpath.lastIndexOf("");
                if (jsfidx > -1) { // 如果有开头的jar包
                    fileName = jarpath.substring(0, jsfidx);
                } else {
                    int sepidx = jarpath.lastIndexOf(File.separator);
                    if (sepidx > -1) {
                        fileName = jarpath.substring(0, sepidx + 1);
                    }
                }
            }
        }
        // 将冒号去掉 “/”换成“-”
        if (fileName != null) {
            fileName = fileName.replace(":", "").replace(File.separator, "/")
                    .replace("/", "-");
            if (fileName.startsWith("-")) {
                fileName = fileName.substring(1);
            }
        } else {
            LOGGER.warn("can not parse webapp baseDir path");
            fileName = "UNKNOW_";
        }
        return fileName;
    }

    /**
     * 得到USER_HOME目录
     *
     * @param base 用户目录下文件夹
     * @return 得到用户目录
     */
    public static String getUserHomeDir(String base) {
        String userhome = System.getProperty("user.home");
        File file = new File(userhome, base);
        if (file.exists()) {
            if (!file.isDirectory()) {
                LOGGER.error("{} exists, but not directory", file.getAbsolutePath());
                throw new BsoaRuntimeException(21000, file.getAbsolutePath() + " exists, but not directory");
            }
        } else {
            file.mkdirs(); // 可能创建不成功
        }
        return file.getAbsolutePath();
    }

    /**
     * 读取文件内容
     *
     * @param file 文件
     * @return 文件内容
     */
    public static String file2String(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            return null;
        }
        FileReader reader = null;
        StringWriter writer = null;
        try {
            reader = new FileReader(file);
            writer = new StringWriter();
            char[] cbuf = new char[1024];
            int len = 0;
            while ((len = reader.read(cbuf)) != -1) {
                writer.write(cbuf, 0, len);
            }
            return writer.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            if (writer != null) {
                try {
                    writer.close();

                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 读取文件内容
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static String file2String(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    /**
     * 读取类相对路径内容
     *
     * @param clazz
     * @param relativePath
     * @return
     */
    public static String file2String(Class clazz, String relativePath, String encoding) throws IOException {
        try (InputStream is = clazz.getResourceAsStream(relativePath);
             InputStreamReader reader = new InputStreamReader(is, encoding);
             BufferedReader bufferedReader = new BufferedReader(reader);
        ) {
            StringBuilder context = new StringBuilder();
            String lineText = null;
            while ((lineText = bufferedReader.readLine()) != null) {
                context.append(lineText).append(System.lineSeparator());
            }
            return context.toString();
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * 字符流写文件 较快
     *
     * @param file 文件
     * @param data 数据
     */
    public static boolean string2File(File file, String data) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, false);
            writer.write(data);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return true;
    }
}