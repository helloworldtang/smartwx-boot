package com.wxmp.wxapi.process;

import lombok.Getter;

/**
 * 消息类型
 */
@Getter
public enum OAuthScope {
    /**
     * 用户openid
     */
    Base("snsapi_base"),
    /**
     * 用户信息
     */
    Userinfo("userinfo");

    private String name;

    OAuthScope(String name) {
        this.name = name;
    }

}
