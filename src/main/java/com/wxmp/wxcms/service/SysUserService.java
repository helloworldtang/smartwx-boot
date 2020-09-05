package com.wxmp.wxcms.service;

import com.wxmp.wxcms.domain.SysUser;
import com.wxmp.wxcms.domain.bo.LoginBO;

public interface SysUserService {

    LoginBO getSysUser(String account);

    SysUser getSysUserById(String userId);

    int updateLoginPwd(SysUser sysUser);
}
