package com.wxmp.wxcms.mapper;

import com.wxmp.wxcms.domain.SysUser;
import com.wxmp.wxcms.domain.bo.LoginBO;
import org.apache.ibatis.annotations.Param;

public interface SysUserDao {

    /**
     * 根据用户名密码查询
     *
     * @param account
     * @return
     */
    LoginBO getSysUser(@Param("account") String account);

    /**
     * 根据用户名密码查询
     *
     * @param userId
     * @return
     */
    SysUser getSysUserById(String userId);

    /**
     * 修改登录密码
     *
     * @param sysUser
     */
    void updateLoginPwd(SysUser sysUser);
}
