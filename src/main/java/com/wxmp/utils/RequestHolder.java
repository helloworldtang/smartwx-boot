/**
 * @Auther: cheng.tang
 * @Date: 2019/2/12
 * @Description:
 */
package com.wxmp.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Auther: cheng.tang
 * @Date: 2019/2/12
 * @Description:
 */
public class RequestHolder {

    public static HttpServletRequest getRequestFacade() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        return servletRequestAttributes.getRequest();
    }

    public static HttpServletResponse getResponseFacade() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        return servletRequestAttributes.getResponse();
    }


    public static String getLastAccessUrl() {
        HttpServletRequest httpServletRequest = getRequestFacade();
        String requestURI = httpServletRequest.getRequestURI();
        String queryString = httpServletRequest.getQueryString();
        if (StringUtils.isBlank(queryString)) {
            return String.format("[%s] %s", httpServletRequest.getMethod(), requestURI);
        }
        return String.format("[%s] %s?%s", httpServletRequest.getMethod(), requestURI, queryString);

    }


}
