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
package io.bsoa.rpc.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.RegistryConfig;
import io.bsoa.rpc.config.ServerConfig;
import io.bsoa.rpc.config.spring.AnnotationBean;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/1/23 20:59. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class AnnotationStartService {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(AnnotationStartService.class);
	
	private static List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
	
	/*
	 * 协议配置
	 */
	private List<ServerConfig> serverConfigs;
	
	/*
	 * annotation扫描包路径
	 */
	private String scanPackage;
	
	/*
	 * 接口代理类引用
	 */
	private transient Object ref;
	
	private AnnotationBean annotationBean;
	
	private Class<?> interfaceClass;
	
	private String interfaceId;
	
	private String alias;
	
	private Integer timeout;
	
	private String url;
	
	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public AnnotationStartService(){}
	
	public AnnotationStartService(List<ServerConfig> protocolConfigs,
                                  String scanPackage){
		
		this.serverConfigs = protocolConfigs;
		this.scanPackage = scanPackage;
	}
	
	public AnnotationStartService(String scanPackage){
		this.scanPackage = scanPackage;
	}
	
	public AnnotationStartService addRegistry(String address){
		if(StringUtils.isNotBlank(address)){
			RegistryConfig config = new RegistryConfig();
			config.setAddress(address);
			config.setProtocol(BsoaConstants.REGISTRY_PROTOCOL_JSF);
			
			registryConfigs.add(config);
		}
		
		return this;
	}
	
	
	public void export(){
		// 简化配置只针对reference, service通过注解方式提供group和version
		annotationBean = new AnnotationBean(registryConfigs, serverConfigs, scanPackage);
	}

	
	public synchronized Object get() {
		if (interfaceClass == null) {
			throw new RuntimeException("interfaceClass should not be null!");
		}
		if (!StringUtils.isNotBlank(interfaceId)) {
			throw new RuntimeException("interfaceId should not be null!");
		}

		if (StringUtils.isNotBlank(scanPackage)) {
			if (annotationBean == null) {
				annotationBean = getAnnotationBean();
			}
	    	ref = annotationBean != null ? annotationBean.getReferenceObject(interfaceClass) : null;
		} else {
			// 简化配置只针对reference, service通过注解方式提供group和version
			ConsumerConfig<Object> consumer = new ConsumerConfig<Object>();
			consumer.setInterfaceId(interfaceId);
//			consumer.setInterfaceClass(interfaceClass);
			consumer.setRegistry(registryConfigs);
			if(StringUtils.isNotBlank(alias)) {
				consumer.setTags(alias);
			}
			consumer.setTimeout(timeout);
			if(StringUtils.isNotBlank(url)){
				consumer.setUrl(url);
			}
			ref = consumer.refer(); // 引用服务;
		}
    	return ref;
    }
	
	
	public void destroy(){
		if (annotationBean == null) {
    		annotationBean = getAnnotationBean();
    	}
		if(annotationBean != null){
			try {
				annotationBean.destroy();
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}
	
	private AnnotationBean getAnnotationBean(){
		return new AnnotationBean(registryConfigs, scanPackage);
	}
	
	
	public List<ServerConfig> getProtocolConfigs() {
		return serverConfigs;
	}

	public void setProtocolConfigs(List<ServerConfig> protocolConfigs) {
		this.serverConfigs = protocolConfigs;
	}

	public String getScanPackage() {
		return scanPackage;
	}

	public void setScanPackage(String scanPackage) {
		this.scanPackage = scanPackage;
	}

	public Class<?> getInterfaceClass() {
		return interfaceClass;
	}

	public void setInterfaceClass(Class<?> interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	 
	public String getInterfacId() {
		return interfaceId;
	}

	public void setInterfacId(String interfaceId) {
		this.interfaceId = interfaceId;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
}
