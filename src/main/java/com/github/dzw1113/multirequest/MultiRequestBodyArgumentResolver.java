package com.github.dzw1113.multirequest;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.github.dzw1113.annotation.MultiRequestBody;


/**
 * @author dzw
 * @description MultiRequestBody解析器
 * @date 2019/2/28 13:13
 **/
public class MultiRequestBodyArgumentResolver implements HandlerMethodArgumentResolver {
    
    public static final String JSONBODY_ATTRIBUTE = "JSON_REQUEST_BODY";
    
    private static final Logger log = LoggerFactory.getLogger(MultiRequestBodyArgumentResolver.class);
    
    /**
     * @param jsonObj
     * @return com.alibaba.fastjson.JSONObject
     * @description 将json对象中包含的""替换成null
     * @author dzw
     * @date 2019/3/12 15:45
     **/
    public static JSONObject filterNull(JSONObject jsonObj) {
        Set<String> set = jsonObj.keySet();
        Iterator<String> it = set.iterator();
        Object obj;
        String key;
        while (it.hasNext()) {
            key = it.next();
            obj = jsonObj.get(key);
            if (obj instanceof JSONObject) {
                filterNull((JSONObject) obj);
            }
            if (obj instanceof JSONArray) {
                JSONArray objArr = (JSONArray) obj;
                for (int i = 0; i < objArr.size(); i++) {
                    if (objArr.get(i) instanceof JSONObject) {
                        filterNull((JSONObject) objArr.get(i));
                    }
                }
            }
            if ("".equals(obj)) {
                jsonObj.put(key, null);
            }
        }
        return jsonObj;
    }
    
    /**
     * @param parameter 方法参数
     * @return boolean 是否支持
     * @description 设置支持的方法参数类型
     * @author dzw
     * @date 2019/2/28 13:14
     **/
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 支持带@MultiRequestBody注解的参数
        return parameter.hasParameterAnnotation(MultiRequestBody.class);
    }
    
    /**
     * @param parameter     参数
     * @param mavContainer
     * @param webRequest
     * @param binderFactory
     * @return java.lang.Object
     * @description 参数解析，注意：非基本类型返回null会报空指针异常，要通过反射或者JSON工具类创建一个空对象
     * @author dzw
     * @date 2019/2/28 13:14
     **/
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        // 根据@MultiRequestBody注解value作为json解析的key
        MultiRequestBody parameterAnnotation = parameter.getParameterAnnotation(MultiRequestBody.class);
        // 注解的value是JSON的key
        String key = parameterAnnotation.value();
        String jsonBody = getRequestBody(webRequest);
        log.debug("request json:" + jsonBody);
        Object object = JSON.parse(jsonBody);
        Object retVal = null;
        JSONObject jsonObject = null;
        if (object instanceof JSONObject) {
            jsonObject = JSON.parseObject(jsonBody);
        }
        if (object instanceof JSONArray) {
            ParameterizedType pt = (ParameterizedType) parameter.getGenericParameterType();
            Class<?> tt = (Class<?>) pt.getActualTypeArguments()[0];
            retVal = JSON.parseArray(jsonBody, tt);
            valid(webRequest, binderFactory, parameter, retVal);
            return retVal;
        }
        // 如果为空，且非必填，放行
        if (object == null && !parameterAnnotation.required()) {
            return null;
        }
        try {
            jsonObject = filterNull(jsonObject);// ////////////////////////////////??????五哥说把字符串转null。。。
        } catch (Exception e) {
            String name = parameter.getParameter().getName();
            throw new MethodArgumentNotValidException(parameter, new BeanPropertyBindingResult(jsonObject, name));
        }
        Object value;
        // 如果@MultiRequestBody注解没有设置value，则取参数名FrameworkServlet作为json解析的key
        if (Objects.nonNull(key) && !"".equals(key)) {
            value = jsonObject.get(key);
            // 如果设置了value但是解析不到，报错
            if (value == null && parameterAnnotation.required()) {
                throw new IllegalArgumentException(String.format("required param %s is not present", key));
            }
        } else {
            // 注解为设置value则用参数名当做json的key
            key = parameter.getParameter().getName();
            value = jsonObject.get(key);
            if (value == null) {
                value = jsonObject.get(parameter.getParameterName());
            }
        }
        
        // 获取的注解后的类型 Long
        Class<?> parameterType = parameter.getParameterType();
        // 通过注解的value或者参数名解析，能拿到value进行解析
        if (value != null) {
            // 基本类型
            if (parameterType.isPrimitive()) {
                retVal = parsePrimitive(parameterType.getName(), value);
                valid(webRequest, binderFactory, parameter, retVal);
                return retVal;
            }
            // 基本类型包装类
            if (isBasicDataTypes(parameterType)) {
                retVal = parseBasicTypeWrapper(parameterType, value);
                valid(webRequest, binderFactory, parameter, retVal);
                return retVal;
                // 字符串类型
            } else if (parameterType == String.class) {
                retVal = value.toString();
                valid(webRequest, binderFactory, parameter, retVal);
                return retVal;
            } else if (List.class.isAssignableFrom(parameterType)) {
                ParameterizedType pt = (ParameterizedType) parameter.getGenericParameterType();
                // 其他复杂对象
                retVal = JSON.parseObject(value.toString(), pt);
                valid(webRequest, binderFactory, parameter, retVal);
                return retVal;
            }
            // 其他复杂对象
            retVal = JSON.parseObject(value.toString(), parameterType);
            valid(webRequest, binderFactory, parameter, retVal);
            return retVal;
        }
        
        // 解析不到则将整个json串解析为当前参数类型
        if (isBasicDataTypes(parameterType)) {
            if (parameterAnnotation.required()) {
                throw new IllegalArgumentException(String.format("required param %s is not present", key));
            } else {
                valid(webRequest, binderFactory, parameter, null);
                return null;
            }
        }
        // 字符串类型且为null || // 日期类型且为null
        if (parameterType == String.class
                || (parameterType == Date.class || parameterType == LocalDate.class || parameterType == LocalDateTime.class)) {
            if (parameterAnnotation.required()) {
                throw new IllegalArgumentException(String.format("required param %s is not present", key));
            } else {
                valid(webRequest, binderFactory, parameter, null);
                return null;
            }
        }
        
        // 非基本类型，不允许解析所有字段，必备参数则报错，非必备参数则返回null
        if (!parameterAnnotation.parseAllFields()) {
            // 如果是必传参数抛异常
            if (parameterAnnotation.required()) {
                throw new IllegalArgumentException(String.format("required param %s is not present", key));
            }
            valid(webRequest, binderFactory, parameter, null);
            // 否则返回null
            return null;
        }
        // 非基本类型，允许解析，将外层属性解析
        Object result;
        try {
            result = JSON.parseObject(jsonObject.toString(), parameterType);
        } catch (JSONException jsonException) {
            throw new IllegalArgumentException("Request parameter is not a standard JSON format", jsonException);
        }
        
        // 如果非必要参数直接返回，否则如果没有一个属性有值则报错
        if (!parameterAnnotation.required()) {
            valid(webRequest, binderFactory, parameter, result);
            return result;
        } else {
            boolean haveValue = false;
            Field[] declaredFields = parameterType.getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                if (field.get(result) != null) {
                    haveValue = true;
                    break;
                }
            }
            if (!haveValue) {
                throw new IllegalArgumentException(String.format("required param %s is not present", key));
            }
            valid(webRequest, binderFactory, parameter, result);
            return result;
        }
    }
    
    /**
     * @param webRequest
     * @param binderFactory
     * @param parameter
     * @param arg
     * @return void
     * @description 支持Valid注解
     * @author dzw
     * @date 2020/11/7 17:17
     **/
    private void valid(NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory, MethodParameter parameter, Object arg)
            throws Exception {
        if (binderFactory != null) {
            String name = Conventions.getVariableNameForParameter(parameter);
            WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
            Annotation[] annotations = parameter.getParameterAnnotations();
            for (Annotation ann : annotations) {
                Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
                if (validatedAnn != null) {
                    Object hints = validatedAnn.value();
                    Object[] validationHints = (Object[]) hints;
                    binder.validate(validationHints);
                    break;
                }
            }
            if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
                throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
            }
        }
    }
    
    protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
        int i = parameter.getParameterIndex();
        Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
        boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
        return !hasBindingResult;
    }
    
    /**
     * @param parameterTypeName 入参类型
     * @param value             入参值
     * @return java.lang.Object
     * @description 基本类型解析
     * @author dzw
     * @date 2019/2/28 13:16
     **/
    private Object parsePrimitive(String parameterTypeName, Object value) {
        final String booleanTypeName = "boolean";
        if (booleanTypeName.equals(parameterTypeName)) {
            return Boolean.valueOf(value.toString());
        }
        final String intTypeName = "int";
        if (intTypeName.equals(parameterTypeName)) {
            return Integer.valueOf(value.toString());
        }
        final String charTypeName = "char";
        if (charTypeName.equals(parameterTypeName)) {
            return value.toString().charAt(0);
        }
        final String shortTypeName = "short";
        if (shortTypeName.equals(parameterTypeName)) {
            return Short.valueOf(value.toString());
        }
        final String longTypeName = "long";
        if (longTypeName.equals(parameterTypeName)) {
            return Long.valueOf(value.toString());
        }
        final String floatTypeName = "float";
        if (floatTypeName.equals(parameterTypeName)) {
            return Float.valueOf(value.toString());
        }
        final String doubleTypeName = "double";
        if (doubleTypeName.equals(parameterTypeName)) {
            return Double.valueOf(value.toString());
        }
        final String byteTypeName = "byte";
        if (byteTypeName.equals(parameterTypeName)) {
            return Byte.valueOf(value.toString());
        }
        return null;
    }
    
    /**
     * @param parameterType 入参类型
     * @param value         入参值
     * @return java.lang.Object
     * @description 基本类型包装类解析
     * @author dzw
     * @date 2019/2/28 13:16
     **/
    private Object parseBasicTypeWrapper(Class<?> parameterType, Object value) {
        if (Number.class.isAssignableFrom(parameterType)) {
            if (parameterType == Integer.class) {
                return Integer.parseInt(value.toString());
            } else if (parameterType == Short.class) {
                return Short.parseShort(value.toString());
            } else if (parameterType == Long.class) {
                return Long.parseLong(value.toString());
            } else if (parameterType == Float.class) {
                return Float.parseFloat(value.toString());
            } else if (parameterType == Double.class) {
                return Double.parseDouble(value.toString());
            } else if (parameterType == Byte.class) {
                return Byte.parseByte(value.toString());
            }
        } else if (parameterType == Boolean.class) {
            if (Integer.class.equals(value.getClass())) {
                return ((Integer) value) == 0 ? Boolean.FALSE : Boolean.TRUE;
            }
            return Boolean.parseBoolean(value.toString());
        } else if (parameterType == Character.class) {
            return value.toString().charAt(0);
        }
        return null;
    }
    
    /**
     * @param clazz 类
     * @return boolean
     * @description 判断是否为基本数据类型包装类
     * @author dzw
     * @date 2019/2/28 13:16
     **/
    private boolean isBasicDataTypes(Class clazz) {
        Set<Class> classSet = new HashSet<>();
        classSet.add(Integer.class);
        classSet.add(Long.class);
        classSet.add(Short.class);
        classSet.add(Float.class);
        classSet.add(Double.class);
        classSet.add(Boolean.class);
        classSet.add(Byte.class);
        classSet.add(Character.class);
        return classSet.contains(clazz);
    }
    
    /**
     * @param webRequest request
     * @return java.lang.String
     * @description 获取请求体JSON字符串
     * @author dzw
     * @date 2019/2/28 13:17
     **/
    private String getRequestBody(NativeWebRequest webRequest) {
        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        
        // 有就直接获取
        String jsonBody = (String) webRequest.getAttribute(JSONBODY_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        // 没有就从请求中读取
        if (jsonBody == null && servletRequest != null) {
            try {
                jsonBody = IOUtils.toString(servletRequest.getInputStream(), StandardCharsets.UTF_8);
                webRequest.setAttribute(JSONBODY_ATTRIBUTE, jsonBody, NativeWebRequest.SCOPE_REQUEST);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return jsonBody;
    }
    
}
