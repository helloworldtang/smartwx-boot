package com.wxmp.wxapi.service;

import com.alibaba.fastjson.JSONObject;
import com.wxmp.core.exception.WxErrorException;
import com.wxmp.wxapi.process.MpAccount;
import com.wxmp.wxapi.vo.MsgRequest;
import com.wxmp.wxcms.domain.AccountFans;

/**
 * 我的微信服务接口，主要用于结合自己的业务和微信接口
 */
public interface MyService {

    //消息处理
    String processMsg(MsgRequest msgRequest, MpAccount mpAccount) throws WxErrorException;

    //发布菜单
    JSONObject publishMenu(MpAccount mpAccount) throws WxErrorException;

    //删除菜单
    JSONObject deleteMenu(MpAccount mpAccount) throws WxErrorException;

    //获取用户列表
    boolean syncAccountFansList(MpAccount mpAccount) throws WxErrorException;

    //获取单个用户信息
    AccountFans syncAccountFans(String openId, MpAccount mpAccount, boolean merge) throws WxErrorException;

    //同步服务器的用户标签
    boolean syncUserTagList(MpAccount mpAccount);
}
