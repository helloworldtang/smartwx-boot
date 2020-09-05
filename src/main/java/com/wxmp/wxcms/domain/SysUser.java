package com.wxmp.wxcms.domain;

import com.wxmp.core.page.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUser extends Page implements Serializable {

    //主键id
    private String id;
    //用户名
    private String account;
    //密码
    private String pwd;
    //性别 0男 1女
    private String sex;
    //手机号
    private String phone;
    //姓名
    private String trueName;
    //创建时间
    private String createTime;
    //更新时间
    private String updateTime;
    //状态
    private String flag;

    //新登录密码
    private String newpwd;
}
