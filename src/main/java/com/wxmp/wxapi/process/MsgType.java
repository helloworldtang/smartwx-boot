package com.wxmp.wxapi.process;

import lombok.Getter;

/**
 * 消息类型：所有微信涉及到的消息类型统一管理
 */
@Getter
public enum MsgType {
    /**
     * 文本消息
     */
    Text("text"),
    /**
     * 图文消息
     */
    News("news"),
    /**
     * 地理位置消息
     */
    Location("location"),
    /**
     * 图片消息
     */
    Image("image"),
    /**
     * 语音消息
     */
    Voice("voice"),
    /**
     * 视频消息
     */
    Video("video"),
    /**
     * 事件消息
     */
    Event("event"),
    /**
     * 群发图文消息
     */
    MPNEWS("mpnews"),
    /**
     * 订阅消息
     */
    SUBSCRIBE("subscribe"),
    /**
     * 取消订阅
     */
    UNSUBSCRIBE("unsubscribe");

    private String name;

    MsgType(String name) {
        this.name = name;
    }

}
