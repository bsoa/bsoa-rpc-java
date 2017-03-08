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
package io.bsoa.rpc.config.spring;

import io.bsoa.rpc.common.BsoaConstants;
import io.bsoa.rpc.common.utils.ClassLoaderUtils;
import io.bsoa.rpc.common.utils.CommonUtils;
import io.bsoa.rpc.common.utils.ExceptionUtils;
import io.bsoa.rpc.common.utils.StringUtils;
import io.bsoa.rpc.config.ConsumerConfig;
import io.bsoa.rpc.config.MethodConfig;
import io.bsoa.rpc.config.ParameterConfig;
import io.bsoa.rpc.filter.ExcludeFilter;
import io.bsoa.rpc.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by zhangg on 16-7-7.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>Geng Zhang</a>
 */
public class BsoaBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(BsoaBeanDefinitionParser.class);

    private final Class<?> beanClass;

    private final boolean required;

    public BsoaBeanDefinitionParser(Class<?> beanClass, boolean required) {
        this.beanClass = beanClass;
        this.required = required;
    }

    /**
     * Method Name parse
     *
     * @param element
     * @param parserContext
     * @param beanClass
     * @param requireId
     * @return Return Type BeanDefinition
     */
    private BeanDefinition parse(Element element, ParserContext parserContext,
                                 Class<?> beanClass, boolean requireId) {

        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        String id = element.getAttribute("id");

        if (StringUtils.isBlank(id) && requireId) {
            throw new IllegalStateException("[21000]This bean do not set spring bean id " + id);
        }
        //id 肯定是必须的所以此处去掉对id是否为空的判断
        if (requireId) {
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("[21001]Duplicate spring bean id " + id);
            }
            parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
        }
        //set各个属性值
        for (Method setter : beanClass.getMethods()) {
            if (!isProperty(setter, beanClass)) continue; //略过不是property的方法
            String name = setter.getName();
            String property = name.substring(3, 4).toLowerCase() + name.substring(4);
            //根据property名称来进行区别处理
            int proType = getPropertyType(property);
            String value = element.getAttribute(property);
            Object reference = value;
            switch (proType) {
                case 1: // registry
                    if (StringUtils.isNotBlank(value)) {
                        parseMultiRef("registry", value, beanDefinition, parserContext);
                    }
                    break;
                case 2: // protocol
                    if (StringUtils.isNotBlank(value)) {
                        beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                        //parseMultiRef("protocol", value, beanDefinition, parserContext);
                    }
                    break;
//                case 3:
//                    break;
//                case 4:
//                    break;
                case 5: // ref
                    if (StringUtils.isNotBlank(value)) {
                        BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                        if (!refBean.isSingleton() && beanClass == ProviderBean.class) {
                            throw new IllegalStateException("[21002]The exported service ref " + value + " must be singleton! Please set the " + value + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                        }
                        reference = new RuntimeBeanReference(value);
                    } else {
                        reference = null;//保持住ref的null值
                    }
                    beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                    break;
                case 6: // parameters 解析子元素
                    parseParameters(element.getChildNodes(), beanDefinition);
                    break;
                case 7:// methods 解析子元素
                    parseMethods(id, element.getChildNodes(), beanDefinition, parserContext);
                    break;
//                case 8:
//                    break;
//                case 9:
//                    break;
//                case 10:
//                    break;
//                case 11:
//                    break;
//                case 12:
//                    break;
//                case 13:
//                    break;
                case 14: // server
                    parseMultiRef(property, value, beanDefinition, parserContext);
                    break;
                case 15: // filter
                    parseFilters(property, value, beanDefinition, parserContext);
                    break;
                case 16: // onReturn/ onConnect / onAvailable / router
                    parseMultiRef(property, value, beanDefinition, parserContext);
                    break;
                case 17: // mockRef / cacheRef / groupRouter
                    if (StringUtils.isNotBlank(value)) {
                        reference = new RuntimeBeanReference(value);
                    } else {
                        reference = null;//保持住ref的null值
                    }
                    beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                    break;
                case 18: // clazz --> class
                    value = element.getAttribute("class");
                    if (value != null) {
                        beanDefinition.getPropertyValues().addPropertyValue(property, value);
                    }
                    break;
                case 19: // interfaceId --> interface
                    value = element.getAttribute("interface");
                    if (value != null) {
                        beanDefinition.getPropertyValues().addPropertyValue(property, value);
                    }
                    break;
                case 20: // providers / consumers
                    if (value != null) { // 非null字符串 绑定值到属性
                        beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                    }
                    break;
                case 21:
                    parseConsumerConfigs(id, element.getChildNodes(), beanDefinition, parserContext);
                    break;
                default:
                    // 默认非空字符串只是绑定值到属性
                    if (StringUtils.isNotBlank(value)) {
                        beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                    }
                    break;
            }
        }

        return beanDefinition;
    }

    /*
     *返回Property Type
     *
     */
    private int getPropertyType(String propertyName) {
        int type = -1;
        if ("registry".equals(propertyName)) {
            type = 1;
        } else if ("protocol".equals(propertyName)) {
            type = 2;
//        } else if ("onReturn".equals(propertyName)) {
//            type = 3;
//        } else if ("onthrow".equals(propertyName)) {
//            type = 4;
        } else if ("ref".equals(propertyName)) {
            type = 5;
        } else if ("parameters".equals(propertyName)) {
            type = 6;
        } else if ("methods".equals(propertyName)) {
            type = 7;
//        } else if ("arguments".equals(propertyName)) {
//            type = 8;
//        } else if ("registries".equals(propertyName)) {
//            type = 9;
//        } else if ("protocols".equals(propertyName)) {
//            type = 10;
//        } else if ("application".equals(propertyName)) {
//            type = 11;
//        } else if ("zkClient".equals(propertyName)) {
//            type = 12;
//        } else if ("zkClients".equals(propertyName)) {
//            type = 13;
        } else if ("server".equals(propertyName)) {
            type = 14;
        } else if ("filter".equals(propertyName)) {
            type = 15;
        } else if ("onReturn".equals(propertyName)
                || "onConnect".equals(propertyName)
                || "onAvailable".equals(propertyName)
                || "router".equals(propertyName)) {  // 逗号分隔的多个ref
            type = 16;
        } else if ("mockRef".equals(propertyName)
                || "cacheRef".equals(propertyName)
                || "groupRouter".equals(propertyName)) { // 单个ref
            type = 17;
        } else if ("clazz".equals(propertyName)) {
            type = 18;
        } else if ("interfaceId".equals(propertyName)) {
            type = 19;
        } else if ("providers".equals(propertyName)
                || "consumers".equals(propertyName)) { // 可以将属性置为 空字符串
            type = 20;
        } else if ("consumerConfigs".equals(propertyName)) {
            type = 21;
        }
        return type;
    }

    /*
     *判断是否有相应get\set方法的property
     */
    private boolean isProperty(Method method, Class beanClass) {
        String methodName = method.getName();
        boolean flag = methodName.length() > 3 && methodName.startsWith("set") && Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 1;
        Method getter = null;
        if (flag) {
            Class<?> type = method.getParameterTypes()[0];
            try {
                getter = beanClass.getMethod("get" + methodName.substring(3), new Class<?>[0]);
            } catch (NoSuchMethodException e) {
                try {
                    getter = beanClass.getMethod("is" + methodName.substring(3), new Class<?>[0]);
                } catch (NoSuchMethodException e2) {
                }
            }
            flag = getter != null && Modifier.isPublic(getter.getModifiers()) && type.equals(getter.getReturnType());
        }
        return flag;
    }

    @SuppressWarnings("unchecked")
    private void parseParameters(NodeList nodeList, RootBeanDefinition beanDefinition) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedMap parameters = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if ("parameter".equals(node.getNodeName())
                            || "parameter".equals(node.getLocalName())) {
                        if (parameters == null) {
                            parameters = new ManagedMap();
                        }
                        String key = ((Element) node).getAttribute("key");
                        if (!ParameterConfig.isValidParamKey(key)) {
                            throw ExceptionUtils.buildRuntime(21000, "param.key", key, "key can not start with "
                                    + BsoaConstants.HIDE_KEY_PREFIX + " and " + BsoaConstants.INTERNAL_KEY_PREFIX);
                        }
                        String value = ((Element) node).getAttribute("value");
                        boolean hide = CommonUtils.isTrue(((Element) node).getAttribute("hide"));
                        if (hide) {
                            key = BsoaConstants.HIDE_KEY_PREFIX + key;
                        }
                        parameters.put(key, new TypedStringValue(value, String.class));
                    }
                }
            }
            if (parameters != null) {
                beanDefinition.getPropertyValues().addPropertyValue("parameters", parameters);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseMethods(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                              ParserContext parserContext) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedMap methods = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    if ("method".equals(node.getNodeName()) || "method".equals(node.getLocalName())) {
                        String methodName = element.getAttribute("name");
                        if (methodName == null || methodName.length() == 0) {
                            throw new IllegalStateException("<jsf:method> name attribute == null");
                        }
                        if (methods == null) {
                            methods = new ManagedMap();
                        }
                        BeanDefinition methodBeanDefinition = parse(((Element) node),
                                parserContext, MethodConfig.class, false);
                        String name = id + "." + methodName;
                        BeanDefinitionHolder methodBeanDefinitionHolder = new BeanDefinitionHolder(
                                methodBeanDefinition, name);
                        methods.put(methodName, methodBeanDefinitionHolder);
                    }
                }
            }
            if (methods != null) {
                beanDefinition.getPropertyValues().addPropertyValue("methods", methods);
            }
        }
    }

    private void parseConsumerConfigs(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                      ParserContext parserContext) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedMap consumerConfigs = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    if ("consumer".equals(node.getNodeName()) || "consumer".equals(node.getLocalName())) {
                        String alias = element.getAttribute("alias");
                        if (StringUtils.isBlank(alias)) {
                            throw new IllegalStateException("Attribute alias of <jsf:consumer> below <jsf:consumerGroup> is empty");
                        }
                        BeanDefinition consumerBeanDefinition = parse(element, parserContext, ConsumerConfig.class, false);
                        BeanDefinitionHolder consumerBeanDefinitionHolder = new BeanDefinitionHolder(
                                consumerBeanDefinition, id + "." + alias);
                        if (consumerConfigs == null) {
                            consumerConfigs = new ManagedMap();
                        }
                        if (consumerConfigs.containsKey(alias)) {
                            throw ExceptionUtils.buildRuntime(21318, "consumerGroup.alias", alias,
                                    "Duplicate alias in consumer group of " + id);
                        }
                        consumerConfigs.put(alias, consumerBeanDefinitionHolder);
                    }
                }
            }
            if (consumerConfigs != null) {
                beanDefinition.getPropertyValues().addPropertyValue("consumerConfigs", consumerConfigs);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseFilters(String property, String value, RootBeanDefinition beanDefinition,
                              ParserContext parserContext) {
        String[] values = value.split("\\s*[,]+\\s*");
        ManagedList list = null;
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (StringUtils.isNotBlank(v)) {
                if (list == null) {
                    list = new ManagedList();
                }
                if (v.startsWith("-")) { // 排除过滤器
                    list.add(new ExcludeFilter(v));
                } else {
                    BeanDefinition fd = parserContext.getRegistry().getBeanDefinition(v);
                    if (fd.isSingleton()) {
                        logger.warn("If custom filter:\"{}\" used by multiple provider/consumer," +
                                " you need to set attribute scope=\"property\"!", v);
                    }
                    if (!Filter.class.isAssignableFrom(ClassLoaderUtils.forName(fd.getBeanClassName()))) {
                        IllegalArgumentException exception = new IllegalArgumentException("Failed to set "
                                + property + ", cause by type of \"" + v + "\" is " + fd.getBeanClassName()
                                + ", not " + Filter.class.getName());
                        logger.error(exception.getMessage());
                        throw exception;
                    }
                    list.add(new RuntimeBeanReference(v));
                }
            }
        }
        beanDefinition.getPropertyValues().addPropertyValue(property, list);
    }

    @SuppressWarnings("unchecked")
    private void parseMultiRef(String property, String value, RootBeanDefinition beanDefinition,
                               ParserContext parserContext) {
        String[] values = value.split("\\s*[,]+\\s*");
        ManagedList list = null;
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (StringUtils.isNotBlank(v)) {
                if (list == null) {
                    list = new ManagedList();
                }
                list.add(new RuntimeBeanReference(v));
            }
        }
        beanDefinition.getPropertyValues().addPropertyValue(property, list);
    }

    /**
     * 分号分隔的属性列表
     */
    private void parseMultiRefWithSemo(String property, String value, RootBeanDefinition beanDefinition,
                                       ParserContext parserContext) {
        String[] values = value.split("\\s*[;]+\\s*");
        ManagedList list = null;
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (StringUtils.isNotBlank(v)) {
                if (list == null) {
                    list = new ManagedList();
                }
                list.add(new RuntimeBeanReference(v));
            }
        }
        beanDefinition.getPropertyValues().addPropertyValue(property, list);
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.xml.BeanDefinitionParser#parse(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
     *
     */
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return parse(element, parserContext, beanClass, required);
    }
}
