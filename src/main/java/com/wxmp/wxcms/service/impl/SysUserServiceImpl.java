package com.wxmp.wxcms.service.impl;

import com.wxmp.wxcms.domain.SysUser;
import com.wxmp.wxcms.domain.bo.LoginBO;
import com.wxmp.wxcms.mapper.SysUserDao;
import com.wxmp.wxcms.service.SysUserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class SysUserServiceImpl implements SysUserService {

    @Resource
    private SysUserDao sysUserDao;


    @Override
    public LoginBO getSysUser(String account) {
        return this.sysUserDao.getSysUser(account);
    }


    /* (non-Javadoc)
     * @see com.wxmp.backstage.sys.ISysUserService#getSysUserById(java.lang.String)
     */
    @Override
    public SysUser getSysUserById(String userId) {
        SysUser resUser = this.sysUserDao.getSysUserById(userId);

        if (resUser != null) {
            return resUser;
        } else {
            return null;
        }
    }


    @Override
    public int updateLoginPwd(SysUser sysUser) {
        int n = 0;
        try {
            sysUserDao.updateLoginPwd(sysUser);
            n = 1;
        } catch (Exception ignored) {
        }
        return n;
    }
}
