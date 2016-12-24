/*
 * Copyright 2016 The BSOA Project
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
package io.bsoa.rpc.config.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

import io.bsoa.rpc.config.ParameterConfig;
import io.bsoa.rpc.config.RegistryConfig;

/**
 * Created by zhanggeng on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class BsoaNamespaceHandler extends NamespaceHandlerSupport{

    public void init() {
        registerBeanDefinitionParser("provider", new BsoaBeanDefinitionParser(ProviderBean.class, true));
        registerBeanDefinitionParser("consumer", new BsoaBeanDefinitionParser(ConsumerBean.class, true));
        registerBeanDefinitionParser("server", new BsoaBeanDefinitionParser(ServerBean.class, true));
        registerBeanDefinitionParser("registry", new BsoaBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("annotation", new BsoaBeanDefinitionParser(AnnotationBean.class, true));
        registerBeanDefinitionParser("parameter", new ParameterDefinitionParser(ParameterConfig.class));
        registerBeanDefinitionParser("filter", new BsoaBeanDefinitionParser(FilterBean.class, true));
    }
}
