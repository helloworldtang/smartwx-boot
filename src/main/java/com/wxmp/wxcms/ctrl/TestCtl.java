package com.wxmp.wxcms.ctrl;

import com.wxmp.core.util.AjaxResult;
import com.wxmp.wxapi.process.WxMemoryCacheClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/test")
public class TestCtl {
    @RequestMapping("/index")
    @ResponseBody
    public AjaxResult index() {
        return AjaxResult.success(WxMemoryCacheClient.getAccount());
    }
}
