package com.wxmp.wxapi.process;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信公众号信息
 */
@Data
public class MpAccount implements Serializable {
    private static final long serialVersionUID = -6315146640254918207L;

    /**
     * 账号
     */
    private String account;
    /**
     * appid
     */
    private String appid;
    /**
     * appsecret
     */
    private String appsecret;
    /**
     * 验证时用的url
     */
    private String url;
    /**
     * token
     */
    private String token;
    /**
     * 自动回复消息条数;默认是5条
     */
    private Integer msgcount;

    public Integer getMsgcount() {
        if (msgcount == null) {
            /**
             * 默认5条
             */
            msgcount = 5;
        }
        return msgcount;
    }

}
