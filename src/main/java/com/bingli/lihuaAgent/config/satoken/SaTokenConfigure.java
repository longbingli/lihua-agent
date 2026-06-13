package com.bingli.lihuaAgent.config.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private static final SaInterceptor SA_INTERCEPTOR = new SaInterceptor(handler -> SaRouter
            .match("/**")
            .notMatch(
                    "/user/login",
                    "/user/register",
                    "/ai/chat/stream",
                    "**/error",
                    "/favicon.ico",
                    "/doc.html",
                    "/swagger-ui/**",
                    "/webjars/**",
                    "/v3/api-docs/**"
            )
            .check(r -> StpUtil.checkLogin())
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                DispatcherType dispatcherType = request.getDispatcherType();
                if (dispatcherType != DispatcherType.REQUEST) {
                    return true;
                }
                return SA_INTERCEPTOR.preHandle(request, response, handler);
            }
        }).addPathPatterns("/**");
    }
}
