package com.bingli.lihuaAgent.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.util.SaResult;

import com.bingli.lihuaAgent.common.BaseResponse;
import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.beans.PropertyEditorSupport;

/**
 * 全局异常处理器
 *
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理空字符串到数字类型的绑定，避免前端传空字符串导致类型转换失败
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Integer.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    try {
                        setValue(Integer.parseInt(text.trim()));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("参数格式错误，需要整数");
                    }
                }
            }
        });
        binder.registerCustomEditor(Long.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    try {
                        setValue(Long.parseLong(text.trim()));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("参数格式错误，需要整数");
                    }
                }
            }
        });
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<?> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    @ExceptionHandler(NotLoginException.class)
    public SaResult handlerNotLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return SaResult.error(e.getMessage());
    }

    @ExceptionHandler(NotRoleException.class)
    public BaseResponse<?> handlerNotRoleException(NotRoleException e) {
        log.error("NotRoleException: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR);
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> handlerNotPermissionException(NotPermissionException e) {
        log.error("NotPermissionException: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "无此权限：" + e.getPermission());
    }

}
