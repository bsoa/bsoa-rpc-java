/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.client.jsf;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.client.AllConnectConnectionHolder;
import io.bsoa.rpc.client.ProviderInfo;
import io.bsoa.rpc.client.TelnetClient;
import io.bsoa.rpc.common.json.JSON;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.ext.Extension;
import io.bsoa.rpc.transport.ClientTransport;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/6 23:29. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@Extension("jsf")
public class JSFConnectionHolder extends AllConnectConnectionHolder{

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(JSFConnectionHolder.class);

    public boolean checkState(ProviderInfo providerInfo, ClientTransport transport) {
        ProviderCheckedInfo checkedInfo = checkProvider(providerInfo);
        return !reliveToRetry(checkedInfo.isProviderExportedFully(), providerInfo, transport);
    }
    
    /**
     * 通过telnet命令检查Provider是否支持调用优化 1.5.0+支持</br>
     *
     * 检查服务是否存在此节点上
     *
     * @param providerInfo
     *         服务端
     */
    public ProviderCheckedInfo checkProvider(ProviderInfo providerInfo) {
        ProviderCheckedInfo checkedInfo = new ProviderCheckedInfo();
        if (providerInfo.getProtocolType().equals("jsf")) {
            for (int i = 0; i < 2; i++) { // 试2次
                TelnetClient client = new TelnetClient(providerInfo.getIp(), providerInfo.getPort(),
                        2000, 2000);
                try {
                    // 发送握手检查服务端版本
                    String versionStr = client.telnetJSF("version");
                    try {
                        Map map = JSON.parseObject(versionStr, Map.class);
                        int realVersion = CommonUtils.parseInt(StringUtils.toString(map.get("jsfVersion")),
                                providerInfo.getBsoaVersion());
                        if (realVersion != providerInfo.getBsoaVersion()) {
                            providerInfo.setBsoaVersion(realVersion);
                        }
                    } catch (Exception e) {
                    }
                    // 检查服务端是否支持invocation简化
                    String ifaceId = consumerConfig.getInterfaceId();
                    if (StringUtils.isNotEmpty(ifaceId)) {
                        if (providerInfo.getBsoaVersion() >= 1500) {
                            String result = client.telnetJSF("check iface " + consumerConfig.getInterfaceId()
                                    + " " + ifaceId);
                            if (result != null) {
                                providerInfo.setInvocationOptimizing("1".equals(result));
                            }
                        } else {
                            providerInfo.setInvocationOptimizing(false);
                        }
                    }
                    //检查指定服务是否已经存在
                    checkedInfo.setProviderExportedFully(checkProviderExportedFully(client, providerInfo));

                    return checkedInfo; // 正常情况直接返回
                } catch (Exception e) {
                    LOGGER.warn(e.getMessage());
                } finally {
                    client.close();
                }
            }
        }
        return checkedInfo;
    }

    /**
     * 通过执行telnet ls -l 命令，查询指定节点上是否已经发布了相关条件的服务
     *
     * @param providerInfo
     *
     * @return
     * @since 1.6.1
     */
    private boolean checkProviderExportedFully(TelnetClient client, ProviderInfo providerInfo) throws IOException {
        String interfaceId = StringUtils.isEmpty(providerInfo.getInterfaceId()) ?
                consumerConfig.getInterfaceId() : providerInfo.getInterfaceId();
        String alias = StringUtils.isEmpty(providerInfo.getTags()) ?
                consumerConfig.getTags() : providerInfo.getTags();
        String serviceStr = String.format("%s?alias=%s&",interfaceId,alias);
        // telnet 检查该节点上是否已经发布此服务
        String exportedService = client.telnetJSF("ls -l");
        if (exportedService != null && exportedService.replace("tags=","alias=").indexOf(serviceStr) > -1){
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * 存活节（通过重试检查后）点如果不包含指定的服务，则加入重试列表中
     *
     * @param providerInfo
     * @param transport
     * @return true 存活节点如果不包含指定的服务
     */
    private boolean reliveToRetry(boolean isProviderExportedFully, ProviderInfo providerInfo, ClientTransport transport) {
        if (!isProviderExportedFully){
            providerInfo.setReconnectPeriodCoefficient(5);
            addRetry(providerInfo,transport);
            LOGGER.warn("No {}/{} service in {}:{} at the moment.add this node to retry connection list.",new Object[]{
                            providerInfo.getInterfaceId(),
                            providerInfo.getTags(),
                            providerInfo.getIp(),
                            providerInfo.getPort()
                    }
            );
            return true;
        }
        return false;
    }


    /**
     * telnet check provider节点信息
     */
    private class ProviderCheckedInfo{

        private boolean providerExportedFully;

        //telnet是否成功
        private boolean telnetOk;

        public ProviderCheckedInfo() {
        }

        public boolean isProviderExportedFully() {
            return providerExportedFully;
        }

        public void setProviderExportedFully(boolean providerExportedFully) {
            this.providerExportedFully = providerExportedFully;
        }

        public boolean isTelnetOk() {
            return telnetOk;
        }

        public void setTelnetOk(boolean telnetOk) {
            this.telnetOk = telnetOk;
        }
    }
}
