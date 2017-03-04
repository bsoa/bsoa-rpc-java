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

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.context.BsoaContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Created by zhangg on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class ParameterDefinitionParser implements BeanDefinitionParser {

    private Class beanClass;

    public ParameterDefinitionParser(Class beanClass) {
        this.beanClass = beanClass;
    }

    public BeanDefinition parse(Element element, ParserContext parserContext) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);

        String key = element.getAttribute("key");
        String value = element.getAttribute("value");
        String hide = element.getAttribute("hide");
        if (CommonUtils.isTrue(hide)) {
            BsoaContext.putGlobalVal(BsoaConstants.HIDE_KEY_PREFIX + key, value);
        } else {
            BsoaContext.putGlobalVal(key, value);
        }

        beanDefinition.getPropertyValues().addPropertyValue("key", key);
        beanDefinition.getPropertyValues().addPropertyValue("value", value);
        beanDefinition.getPropertyValues().addPropertyValue("hide", Boolean.valueOf(hide));

        return beanDefinition;
    }
}
