package com.wxmp.wxapi.interceptor;

import com.wxmp.core.util.HttpUtil;
import com.wxmp.wxapi.process.MpAccount;
import com.wxmp.wxapi.process.OAuthScope;
import com.wxmp.wxapi.process.WxApi;
import com.wxmp.wxapi.process.WxApiClient;
import com.wxmp.wxapi.process.WxMemoryCacheClient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 微信客户端用户请求验证拦截器
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class WxOAuth2Interceptor extends HandlerInterceptorAdapter {

    /**
     * 开发者自行处理拦截逻辑，
     * 方便起见，此处只处理includes
     */
    /**
     * 不需要拦截的
     */
    private String[] excludes;
    /**
     * 需要拦截的
     */
    private String[] includes;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        log.info("-------------------------------------preHandle-----<0>-------------------uri:" + uri);

        /**
         * 为方便展示的参数，开发者自行处理
         */
        boolean oauthFlag = false;
        for (String s : includes) {
            /**
             * 如果包含，就拦截
             */
            if (uri.contains(s)) {
                oauthFlag = true;
                break;
            }
        }
        /**
         * 如果不需要oauth认证
         */
        if (!oauthFlag) {
            return true;
        }

        String sessionId = request.getSession().getId();
        /**
         * 先从缓存中获取openid
         */
        String openid = WxMemoryCacheClient.getOpenid(sessionId);
        log.info("-------------------------------------preHandle-----<1>-------------------openid:" + openid);

        /**
         * 没有，通过微信页面授权获取
         */
        if (StringUtils.isBlank(openid)) {
            String code = request.getParameter("code");
            log.info("-------------------------------------preHandle-----<2-1>-------------------code:" + code);

            /**
             * 如果request中包括code，则是微信回调
             */
            if (!StringUtils.isBlank(code)) {
                log.info("-------------------------------------preHandle-----<2-2>-------------------code:" + code);
                try {
                    openid = WxApiClient.getOAuthOpenId(WxMemoryCacheClient.getMpAccount(), code);
                    log.info("-------------------------------------preHandle-----<2-3>-------------------openid:" + openid);
                    if (!StringUtils.isBlank(openid)) {
                        /**
                         * 缓存openid
                         */
                        WxMemoryCacheClient.setOpenid(sessionId, openid);
                        return true;
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {//oauth获取code
                /**
                 * 获取缓存中的唯一账号
                 */
                MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();
                log.info("-------------------------------------preHandle-----<3-1>-------------------mpAccount:" + mpAccount.getAccount());
                /**
                 * 请求code的回调url
                 */
                String redirectUrl = HttpUtil.getRequestFullUriNoContextPath(request);
                if (!HttpUtil.existHttpPath(redirectUrl)) {
                    //以上不存在就拼接全部url（包括context）
                    redirectUrl = HttpUtil.getRequestFullUri(request);
                }
                log.info("-------------------------------------preHandle-----<3-2>-------------------redirectUrl:" + redirectUrl);
                String state = OAuth2RequestParamHelper.prepareState(request);
                log.info("-------------------------------------preHandle-----<3-3>-------------------state:" + state);
                String url = WxApi.getOAuthCodeUrl(mpAccount.getAppid(), redirectUrl, OAuthScope.Base.toString(), state);
                log.info("-------------------------------------preHandle-----<3-4>-------------------url:" + url);
                HttpUtil.redirectHttpUrl(request, response, url);
                return false;
            }
        } else {
            return true;
        }
        HttpUtil.redirectUrl(request, response, "/error/101.html");
        return false;
    }

}
