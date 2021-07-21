package com.github.dzw1113.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.dzw1113.multirequest.MultiRequestBodyArgumentResolver;


/**
 * @author dzw
 * @description Web配置 增强解析Request Json
 * @date 2019/2/28 13:12
 **/
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 添加MultiRequestBody参数解析器
        argumentResolvers.add(new MultiRequestBodyArgumentResolver());
    }
    
}
