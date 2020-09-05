package com.wxmp.core.spring;

import com.wxmp.core.exception.BusinessException;
import com.wxmp.core.util.AjaxResult;
import com.wxmp.utils.RequestHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class ExceptionHolder {
    //日志

    /**
     * 处理business异常
     *
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public AjaxResult processException(HttpServletRequest request, BusinessException e) {
        log.error(" url:{} , 业务异常：{} ", RequestHolder.getLastAccessUrl(), e.getMessage(), e);
        String message = e.getMessage();
        return AjaxResult.failure(message);
    }

    @ExceptionHandler({Exception.class})
    public AjaxResult processException(HttpServletRequest request, Exception e) {
        log.error(" url:{} , 处理异常：{} ", RequestHolder.getLastAccessUrl(), e.getMessage(), e);
        String message = e.getMessage();
        return AjaxResult.failure(message);
    }

    @ExceptionHandler(BindException.class)
    public AjaxResult processException(HttpServletRequest request, BindException e) {
        log.error(" url:{} , 处理异常：{} ", RequestHolder.getLastAccessUrl(), e.getMessage());
        String errorMsg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(System.lineSeparator()));
        return AjaxResult.failure(errorMsg);
    }


}
