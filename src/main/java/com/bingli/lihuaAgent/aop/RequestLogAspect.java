package com.bingli.lihuaAgent.aop;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Aspect
@Component
public class RequestLogAspect {

    /**
     * 请求日志
     */
    @Around("execution(* com.bingli.lihuaAgent.controller..*.*(..))")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.currentTimeMillis();

        // 生成 traceId
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put("traceId", traceId);

        HttpServletRequest request =
                RequestHolder.getRequest();

        String url = request.getRequestURI();
        String method = request.getMethod();
        String ip = request.getRemoteAddr();

        String classMethod =
                joinPoint.getSignature().getDeclaringTypeName()
                        + "."
                        + joinPoint.getSignature().getName();

        Object[] args = filterArgs(joinPoint.getArgs());

        log.info("\n================ 请求开始 ================\n" +
                        "traceId     : {}\n" +
                        "URL         : {}\n" +
                        "HTTP Method : {}\n" +
                        "IP          : {}\n" +
                        "Class Method: {}\n" +
                        "Request Args: {}\n",
                traceId,
                url,
                method,
                ip,
                classMethod,
                maskSensitiveFields(args)
        );

        Object result;

        try {

            result = joinPoint.proceed();

            long cost = System.currentTimeMillis() - start;

            log.info("\n================ 请求结束 ================\n" +
                            "traceId     : {}\n" +
                            "Response    : {}\n" +
                            "Cost Time   : {} ms\n",
                    traceId,
                    JSONUtil.toJsonStr(result),
                    cost
            );

            return result;

        } catch (Throwable e) {

            long cost = System.currentTimeMillis() - start;

            log.error("\n================ 请求异常 ================\n" +
                            "traceId     : {}\n" +
                            "Error Msg   : {}\n" +
                            "Cost Time   : {} ms\n",
                    traceId,
                    e.getMessage(),
                    cost,
                    e
            );

            throw e;

        } finally {
            MDC.clear();
        }
    }

    /**
     * 过滤特殊参数
     */
    private Object[] filterArgs(Object[] args) {

        return Arrays.stream(args)
                .filter(arg ->
                        !(arg instanceof MultipartFile)
                                && !(arg instanceof ServletRequest)
                                && !(arg instanceof ServletResponse)
                )
                .toArray();
    }

    /**
     * 脱敏日志中的敏感字段（密码、token 等）
     */
    private String maskSensitiveFields(Object[] args) {
        List<Object> masked = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                masked.add(null);
                continue;
            }
            // 基本类型直接保留
            if (arg.getClass().isPrimitive() || arg instanceof String
                    || arg instanceof Number || arg instanceof Boolean) {
                masked.add(arg);
                continue;
            }
            // 对象类型：转为 Map 后递归脱敏
            Map<String, Object> map = BeanUtil.beanToMap(arg, false, false);
            masked.add(doMask(map));
        }
        return JSONUtil.toJsonStr(masked);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> doMask(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (key.toLowerCase().contains("password")
                    || key.toLowerCase().contains("token")
                    || key.toLowerCase().contains("secret")) {
                entry.setValue("******");
            } else if (value instanceof Map) {
                doMask((Map<String, Object>) value);
            }
        }
        return map;
    }
}