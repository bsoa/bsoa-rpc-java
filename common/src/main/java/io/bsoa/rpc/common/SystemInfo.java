/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.utils.NetUtils;

/**
 *
 *
 * Created by zhangg on 2016/7/14 20:48.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class SystemInfo {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(SystemInfo.class);
    
    public static final int CPU_CORES = getCpuCores();
    private static String LOCALHOST;


    static {
        String osName = System.getProperty("os.name").toLowerCase();
        LOGGER.debug("BSOA OS Name::{}", osName);
        if (osName.contains("windows")) {
            IS_WINDOWS = true;
        } else if (osName.contains("linux")) {
            IS_LINUX = true;
        }

        LOCALHOST = NetUtils.getLocalHost();
    }

    private static boolean IS_WINDOWS;
    private static boolean IS_LINUX;

    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    public static Boolean isLinux() {
        return IS_LINUX;
    }

    
    /**
     * 得到CPU核心数（dock特殊处理）
     *
     * @return 可用的cpu内核数
     */
    public static int getCpuCores() {
//        try { // 京东的docker机器特殊处理
//            //cat /etc/config_info
//            // {"Config": {"Cpuset": "1,2", "Memory": 4294967296}, "host_ip": "10.8.65.251"}
//            String s = FileUtils.file2String(new File("/etc/config_info"));
//            if (StringUtils.isNotEmpty(s)) {
//                Map all = JSON.parseObject(s, Map.class);
//                Map config = (Map) all.get("Config");
//                String cpuset = (String) config.get("Cpuset");
//                if (cpuset != null) {
//                    return cpuset.split(",").length;
//                }
//            }
//        } catch (Exception e) {
//        }
        // 找不到文件或者异常，则去物理机的核心数
        return Runtime.getRuntime().availableProcessors();
    }

    public static String getLocalHost() {
        return LOCALHOST;
    }
}
