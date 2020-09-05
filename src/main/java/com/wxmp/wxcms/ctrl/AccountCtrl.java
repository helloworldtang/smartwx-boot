package com.wxmp.wxcms.ctrl;

import com.wxmp.core.common.BaseCtrl;
import com.wxmp.core.util.AjaxResult;
import com.wxmp.core.util.wx.WxUtil;
import com.wxmp.wxapi.process.WxMemoryCacheClient;
import com.wxmp.wxcms.domain.Account;
import com.wxmp.wxcms.service.AccountService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author hermit
 * @version 2.0
 * @date 2018-04-17 10:54:58
 */
@RestController
public class AccountCtrl extends BaseCtrl {

    @Autowired
    private AccountService entityService;

    @GetMapping(value = "/account/getById")
    public AjaxResult getById(long id) {
        entityService.getById(id);
        return AjaxResult.success();
    }

    @GetMapping("/account/listForPage")
    public AjaxResult listForPage(Account searchEntity) {
        List<Account> list = entityService.listForPage();
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.success();
        }
        //account存入缓存中
        Account account;
        if (null != searchEntity && null != searchEntity.getId()) {
            account = entityService.getById(searchEntity.getId());
        } else {
            account = entityService.getByAccount(WxMemoryCacheClient.getAccount());
        }
        WxMemoryCacheClient.setAccount(account.getAccount());
        return AjaxResult.success(WxUtil.getAccount(list, account.getName()));
    }


}
