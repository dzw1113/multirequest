# Getting Started

maven引用

```
<dependency>
    <groupId>io.github.dzw1113</groupId>
    <artifactId>multirequest</artifactId>
    <version>1.0.1</version>
</dependency>
```
---
@RequestBody:并不能支持多个参数，当有多个参数时，必须封装成对象，较麻烦

@MultiRequestBody:支持关系映射、多参数（基础类型、集合、对象），支持Spring Validated校验。支持Swagger2的话，需要额外配置代码。

用法：
在controller方法种直接引用：

```
public HttpResult<HdTodayReviewPO> get(@MultiRequestBody HdTodayReviewQueryPO hdTodayReviewQueryPO) 

public HttpResult check(@MultiRequestBody(required = true) Long id, @MultiRequestBody(required = true) String operationCode) {
```

---

过滤器：BodyFilter,支持反复读取Request里的流，需自己注入到FilterRegistrationBean里。

注入：
```
@Bean
@Order(3)
public FilterRegistrationBean<BodyFilter> Filters() {
    FilterRegistrationBean<BodyFilter> registrationBean = new FilterRegistrationBean<BodyFilter>();
    registrationBean.setFilter(new BodyFilter());
    registrationBean.addUrlPatterns("/*");
    registrationBean.setAsyncSupported(Boolean.TRUE);
    registrationBean.setName("bodyFilter");
    return registrationBean;
}
```
取法：

```
HttpServletRequest request = servletRequestAttributes.getRequest();
jsonBody = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
RequestUtils.getRequest().setAttribute("JSON_REQUEST_BODY", jsonBody);
```
