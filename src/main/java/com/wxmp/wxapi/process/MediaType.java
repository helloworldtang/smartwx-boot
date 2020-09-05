package com.wxmp.wxapi.process;

import lombok.Getter;

/**
 * 多媒体文件类型
 */
@Getter
public enum MediaType {

    /**
     * //图文
     */
    News("news"),
    /**
     * 图片
     */
    Image("image"),
    /**
     * 语音
     */
    Voice("voice"),
    /**
     * 视频
     */
    Video("video"),
    /**
     * 缩略图
     */
    Thumb("thumb");

    private String name;

    MediaType(String name) {
        this.name = name;
    }

}
