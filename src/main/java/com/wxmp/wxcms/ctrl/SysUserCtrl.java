package com.wxmp.wxcms.ctrl;

import com.wxmp.core.common.BaseCtrl;
import com.wxmp.core.util.AjaxResult;
import com.wxmp.core.util.MD5Util;
import com.wxmp.core.util.SessionUtil;
import com.wxmp.wxapi.process.WxMemoryCacheClient;
import com.wxmp.wxcms.domain.Account;
import com.wxmp.wxcms.domain.SysUser;
import com.wxmp.wxcms.domain.bo.LoginBO;
import com.wxmp.wxcms.domain.req.LoginReq;
import com.wxmp.wxcms.service.AccountService;
import com.wxmp.wxcms.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@RestController
public class SysUserCtrl extends BaseCtrl {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private AccountService accountService;

    @PostMapping(value = "/user/login")
    public AjaxResult login(@RequestBody @Validated LoginReq loginReq) {
        LoginBO loginBO = sysUserService.getSysUser(loginReq.getAccount());
        String inputPwd = MD5Util.getMD5Code(loginReq.getPwd());
        if (loginBO == null || !Objects.equals(inputPwd, loginBO.getPwd())) {
            return AjaxResult.failure("用户名或者密码错误");
        }

        SessionUtil.setUser(loginBO);
        //设置登陆者默认公众号
        Account account = accountService.getSingleAccount();
        if (account != null) {
            WxMemoryCacheClient.setAccount(account.getAccount());
        }
        return AjaxResult.success(loginBO.getTrueName());
    }

    @PostMapping(value = "/user/updatepwd")
    public AjaxResult updatepwd(SysUser user) {
        if (!SessionUtil.getUser().getPwd().equals(MD5Util.getMD5Code(user.getPwd()))) {
            return AjaxResult.failure("用户名或密码错误");
        }
        user.setNewpwd(MD5Util.getMD5Code(user.getNewpwd()));
        this.sysUserService.updateLoginPwd(user);
        //注销用户
        request.getSession().invalidate();
        return AjaxResult.success();
    }

    @PostMapping("/user/logout")
    public AjaxResult logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return AjaxResult.success();
    }


}
