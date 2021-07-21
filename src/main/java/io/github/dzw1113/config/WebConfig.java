package io.github.dzw1113.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.dzw1113.multirequest.MultiRequestBodyArgumentResolver;


/**
 * Web配置 增强解析Request Json
 *
 * @author dzw
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 添加MultiRequestBody参数解析器
        argumentResolvers.add(new MultiRequestBodyArgumentResolver());
    }
    
}
