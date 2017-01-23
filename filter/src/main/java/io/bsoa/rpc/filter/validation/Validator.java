package io.bsoa.rpc.filter.validation;

/**
 * Validator
 *
 * @author william.liangf
 */
public interface Validator {

    /**
     * 校验方法下的参数
     *
     * @param methodName
     *         方法名
     * @param parameterTypes
     *         方法参数类型
     * @param arguments
     *         方法值列表
     * @throws Exception
     *         校验不通过抛出的异常
     */
    void validate(String methodName, String[] parameterTypes, Object[] arguments) throws Exception;
}
