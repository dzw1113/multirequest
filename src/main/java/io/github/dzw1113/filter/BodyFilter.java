package io.github.dzw1113.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;


/**
 * 可反复获取body数据
 * @author dzw
 */
public class BodyFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletRequest requestWrapper = null;
        if (request instanceof HttpServletRequest) {
            // 只处理json以及contentType为null的
            HttpServletRequest httpServletRequest = (HttpServletRequest)request;
            if (HttpMethod.POST.toString().equals(httpServletRequest.getMethod())
                && (httpServletRequest.getContentType() == null || httpServletRequest.getContentType().contains("json"))) {
                requestWrapper = new BodyRequestWrapper(httpServletRequest);
            }
        }
        if (requestWrapper == null) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(requestWrapper, response);
        }

    }

    @Override
    public void destroy() {
        // do nothing
    }

}
