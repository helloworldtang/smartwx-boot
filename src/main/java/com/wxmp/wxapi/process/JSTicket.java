package com.wxmp.wxapi.process;

import com.wxmp.core.util.CalendarUtil;
import lombok.Data;

/**
 * 接口凭证
 */
@Data
public class JSTicket {
    /**
     * 接口访问凭证
     */
    private String ticket;
    /**
     * 凭证有效期，单位：秒
     */
    private int expiresIn;
    /**
     * 创建时间，单位：秒 ，用于判断是否过期
     */
    private long createTime;
    /**
     * 错误编码
     */
    private Integer errcode;
    /**
     * 错误消息
     */
    private String errmsg;

    public JSTicket() {
        this.createTime = CalendarUtil.getTimeInSeconds();
    }

    public JSTicket(String ticket, int expiresIn) {
        this.ticket = ticket;
        this.expiresIn = expiresIn;
        this.createTime = CalendarUtil.getTimeInSeconds();
    }


    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
        this.errmsg = ErrCode.errMsg(errcode);
    }

    /**
     * 是否超时
     *
     * @return true-超时；false-没有超时
     */
    public boolean isExpires() {
        long now = CalendarUtil.getTimeInSeconds();
        /**
         * 预留 10s
         */
        return now - this.createTime - 10 >= this.expiresIn;
    }

    /**
     * 是否超时
     *
     * @return true-超时；false-没有超时
     */
    public boolean isExpires(Long expireTime) {
        long now = CalendarUtil.getTimeInSeconds();
        /**
         * 预留 10s
         */
        return now - this.createTime - 10 >= expireTime;
    }


}
