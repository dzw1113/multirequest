package io.github.dzw1113.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller中方法接收多个JSON对象
 * @author dzw
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiRequestBody {
    /**
     * 是否必须出现的参数
     * @return boolean
     */
    boolean required() default false;
    
    /**
     * 当value的值或者参数名不匹配时，是否允许解析最外层属性到该对象
     * @return boolean
     */
    boolean parseAllFields() default true;

    /**
     * 解析时用到的JSON的key
     * @return String
     */
    String value() default "";
}