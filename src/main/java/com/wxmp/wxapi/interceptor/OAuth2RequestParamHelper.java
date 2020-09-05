package com.wxmp.wxapi.interceptor;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信oauth interceptor 处理参数转换；
 * 对于interceptor的url如果有参数，业务中请用此类获取参数；
 */
public class OAuth2RequestParamHelper {

    private static final String REGEX = "!";
    public static final String SEPARATE_EQUAL_FLAG = "=";

    /**
     * 准备state参数
     *
     * @param request
     * @return
     */
    static String prepareState(HttpServletRequest request) {
        Map<String, String[]> map = request.getParameterMap();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String[] entryValue = entry.getValue();
            if (entryValue != null && entryValue.length > 0) {
                //用！间隔
                sb.append(entry.getKey())
                        .append(SEPARATE_EQUAL_FLAG)
                        .append(entryValue[0])
                        .append(REGEX);
            }
        }
        String str = sb.toString();
        if (StringUtils.isBlank(str)) {
            return "";
        } else {
            return str.substring(0, str.length() - 1);
        }
    }

    /**
     * 获取state参数map
     *
     * @param request
     * @return
     */
    public static Map<String, String> getStateParam(HttpServletRequest request) {
        String state = request.getParameter("state");
        //用！间隔
        String[] stateArr = state.split(REGEX);
        Map<String, String> param = new HashMap<>(stateArr.length);
        if (stateArr.length > 0) {
            for (String s : stateArr) {
                String key = s.split(SEPARATE_EQUAL_FLAG)[0];
                String value = s.split(SEPARATE_EQUAL_FLAG)[1];
                param.put(key, value);
            }
        }
        return param;
    }

    /**
     * 根据key获取；如果有state，从state中获取；否则从request中获取
     *
     * @param request
     * @param name
     * @return
     */
    public static String getParameter(HttpServletRequest request, String name) {
        String state = request.getParameter("state");
        if (state != null) {
            //用！间隔
            String[] stateArr = state.split(REGEX);
            if (stateArr.length > 0) {
                for (String s : stateArr) {
                    String key = s.split(SEPARATE_EQUAL_FLAG)[0];
                    String value = s.split(SEPARATE_EQUAL_FLAG)[1];
                    if (name.equals(key)) {
                        return value;
                    }
                }
            }
            return null;
        } else {
            return request.getParameter(name);
        }
    }

}
