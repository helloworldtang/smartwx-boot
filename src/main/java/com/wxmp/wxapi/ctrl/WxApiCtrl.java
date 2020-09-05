package com.wxmp.wxapi.ctrl;

import com.alibaba.fastjson.JSONObject;
import com.wxmp.core.common.BaseCtrl;
import com.wxmp.core.exception.WxErrorException;
import com.wxmp.core.util.AjaxResult;
import com.wxmp.core.util.DateUtil;
import com.wxmp.core.util.UploadUtil;
import com.wxmp.core.util.wx.HTTPResultXml;
import com.wxmp.core.util.wx.SignUtil;
import com.wxmp.wxapi.process.ErrCode;
import com.wxmp.wxapi.process.MediaType;
import com.wxmp.wxapi.process.MpAccount;
import com.wxmp.wxapi.process.MsgType;
import com.wxmp.wxapi.process.MsgXmlUtil;
import com.wxmp.wxapi.process.WxApi;
import com.wxmp.wxapi.process.WxApiClient;
import com.wxmp.wxapi.process.WxMemoryCacheClient;
import com.wxmp.wxapi.service.MyService;
import com.wxmp.wxapi.vo.Material;
import com.wxmp.wxapi.vo.MaterialArticle;
import com.wxmp.wxapi.vo.MaterialItem;
import com.wxmp.wxapi.vo.MsgRequest;
import com.wxmp.wxapi.vo.TemplateMessage;
import com.wxmp.wxcms.domain.AccountFans;
import com.wxmp.wxcms.domain.MsgNews;
import com.wxmp.wxcms.domain.MsgText;
import com.wxmp.wxcms.service.MsgNewsService;
import com.wxmp.wxcms.service.MsgTextService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 微信与开发者服务器交互接口
 */
@Slf4j
@RestController
public class WxApiCtrl extends BaseCtrl {

    @Resource
    private MyService myService;

    @Resource
    private MsgTextService msgTextService;

    @Resource
    private MsgNewsService msgNewsService;

    /**
     * GET请求：进行URL、Tocken 认证；
     * 1. 将token、timestamp、nonce三个参数进行字典序排序
     * 2. 将三个参数字符串拼接成一个字符串进行sha1加密
     * 3. 开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
     */
    @GetMapping(value = "/wxapi/{account}/message")
    public String doGet(HttpServletRequest request, @PathVariable String account) {
        //如果是多账号，根据url中的account参数获取对应的MpAccount处理即可
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            log.info("key: {} value: {}", entry.getKey(), Arrays.toString(entry.getValue()));
        }

        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        if (mpAccount != null) {
            String token = mpAccount.getToken();//获取token，进行验证；
            String signature = request.getParameter("signature");// 微信加密签名
            String timestamp = request.getParameter("timestamp");// 时间戳
            String nonce = request.getParameter("nonce");// 随机数
            String echostr = request.getParameter("echostr");// 随机字符串

            // 校验成功返回  echostr，成功成为开发者；否则返回error，接入失败
            if (SignUtil.validSign(signature, token, timestamp, nonce)) {
                return echostr;
            }
        }
        return "error";
    }

    /**
     * POST 请求：进行消息处理；
     */
    @PostMapping(value = "/wxapi/{account}/message")
    public String doPost(HttpServletRequest request, @PathVariable String account, HttpServletResponse response) {
        try {
            //处理用户和微信公众账号交互消息
            MpAccount mpAccount = WxMemoryCacheClient.getMpAccount(account);
            MsgRequest msgRequest = MsgXmlUtil.parseXml(request);//获取发送的消息
            return myService.processMsg(msgRequest, mpAccount);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "error";
        }
    }

    //创建微信公众账号菜单
    @RequestMapping(value = "/wxapi/publishMenu")
    public ModelAndView publishMenu() throws WxErrorException {
        JSONObject rstObj = null;
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();
        if (mpAccount != null) {
            rstObj = myService.publishMenu(mpAccount);
            if (rstObj != null) {//成功，更新菜单组
                if (rstObj.containsKey("menu_id")) {
                    ModelAndView mv = new ModelAndView("common/success");
                    mv.addObject("successMsg", "创建菜单成功");
                    return mv;
                } else if (rstObj.containsKey("errcode") && rstObj.getIntValue("errcode") == 0) {
                    ModelAndView mv = new ModelAndView("common/success");
                    mv.addObject("successMsg", "创建菜单成功");
                    return mv;
                }
            }
        }

        ModelAndView mv = new ModelAndView("common/failure");
        String failureMsg = "创建菜单失败，请检查菜单：可创建最多3个一级菜单，每个一级菜单下可创建最多5个二级菜单。";
        if (rstObj != null) {
            failureMsg += ErrCode.errMsg(rstObj.getIntValue("errcode"));
        }
        mv.addObject("failureMsg", failureMsg);
        return mv;
    }

    //删除微信公众账号菜单
    @RequestMapping(value = "/wxapi/deleteMenu")
    public ModelAndView deleteMenu(HttpServletRequest request) throws WxErrorException {
        JSONObject rstObj = null;
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        if (mpAccount != null) {
            rstObj = myService.deleteMenu(mpAccount);
            if (rstObj != null && rstObj.getIntValue("errcode") == 0) {
                ModelAndView mv = new ModelAndView("common/success");
                mv.addObject("successMsg", "删除菜单成功");
                return mv;
            }
        }
        ModelAndView mv = new ModelAndView("common/failure");
        String failureMsg = "删除菜单失败";
        if (rstObj != null) {
            failureMsg += ErrCode.errMsg(rstObj.getIntValue("errcode"));
        }
        mv.addObject("failureMsg", failureMsg);
        return mv;
    }

    //获取用户列表
    @RequestMapping(value = "/wxapi/syncAccountFansList")
    public AjaxResult syncAccountFansList() throws WxErrorException {
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        if (mpAccount != null) {
            boolean flag = myService.syncAccountFansList(mpAccount);
            if (flag) {
                return AjaxResult.success();
            }
        }
        return AjaxResult.failure();
    }

    /**
     * 同步用户标签列表
     *
     * @return
     */
    @RequestMapping(value = "/wxapi/syncUserTagList")
    public AjaxResult syncUserTagList() {
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        if (mpAccount != null) {
            boolean flag = myService.syncUserTagList(mpAccount);
            if (flag) {
                return AjaxResult.success();
            }
        }
        return AjaxResult.failure();
    }

    //根据用户的ID更新用户信息
    @RequestMapping(value = "/wxapi/syncAccountFans")
    public AjaxResult syncAccountFans(String openId) throws WxErrorException {
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        if (mpAccount != null) {
            AccountFans fans = myService.syncAccountFans(openId, mpAccount, true);//同时更新数据库
            if (fans != null) {
                return AjaxResult.success(fans);
            }
        }
        return AjaxResult.failure();
    }

    //获取永久素材
    @RequestMapping(value = "/wxapi/syncMaterials")
    public AjaxResult syncMaterials(MaterialArticle materialArticle) throws WxErrorException {
        List<MaterialArticle> materialList = new ArrayList<MaterialArticle>();
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        Material material = WxApiClient.syncBatchMaterial(MediaType.News, materialArticle.getPage(), materialArticle.getPageSize(), mpAccount);
        if (material != null) {

            List<MaterialItem> itemList = material.getItems();
            if (itemList != null) {
                for (MaterialItem item : itemList) {
                    MaterialArticle m = new MaterialArticle();
                    if (item.getNewsItems() != null && item.getNewsItems().size() > 0) {
                        MaterialArticle ma = item.getNewsItems().get(0);//用第一个图文的简介、标题、作者、url
                        m.setAuthor(ma.getAuthor());
                        m.setTitle(ma.getTitle());
                        m.setUrl(ma.getUrl());
                    }
                    materialList.add(m);
                }
            }
        }
        return getResult(materialArticle, materialList);
    }


    //上传图文素材
    @RequestMapping(value = "/wxapi/doUploadMaterial")
    public ModelAndView doUploadMaterial(MsgNews msgNews) throws Exception {
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        String rstMsg = "上传图文消息素材";
        List<MsgNews> msgNewsList = new ArrayList<>();
        msgNewsList.add(msgNews);
        JSONObject rstObj = WxApiClient.uploadNews(msgNewsList, mpAccount);
        if (rstObj.containsKey("media_id")) {
            ModelAndView mv = new ModelAndView("common/success");
            mv.addObject("successMsg", "上传图文素材成功,素材 media_id : " + rstObj.getString("media_id"));
            return mv;
        } else {
            rstMsg = ErrCode.errMsg(rstObj.getIntValue("errcode"));
        }
        ModelAndView mv = new ModelAndView("common/failure");
        mv.addObject("failureMsg", rstMsg);
        return mv;
    }

    //获取openid
    @RequestMapping(value = "/wxapi/oauthOpenid.html")
    public ModelAndView oauthOpenid(HttpServletRequest request) throws WxErrorException {
        log.info("-------------------------------------oauthOpenid-----<0>-------------------");
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        log.info("-------------------------------------oauthOpenid-----<1>-------------------mpAccount：" + mpAccount.getAccount());
        if (mpAccount != null) {
            ModelAndView mv = new ModelAndView("wxweb/oauthopenid");
            //拦截器已经处理了缓存,这里直接取
            String openid = WxMemoryCacheClient.getOpenid(request.getSession().getId());
            log.info("-------------------------------------oauthOpenid-----<2>-------------------openid:" + openid);
            AccountFans fans = myService.syncAccountFans(openid, mpAccount, false);//同时更新数据库
            mv.addObject("openid", openid);
            mv.addObject("fans", fans);
            log.info("-------------------------------------oauthOpenid-----<3>-------------------fans:" + fans.getNicknameStr());
            return mv;
        } else {
            ModelAndView mv = new ModelAndView("common/failuremobile");
            mv.addObject("message", "OAuth获取openid失败");
            log.info("-------------------------------------oauthOpenid-----<4>-------------------");
            return mv;
        }
    }

    /**
     * 生成二维码
     *
     * @param request
     * @param num     二维码参数
     * @return
     */
    @RequestMapping(value = "/wxapi/createQrcode", method = RequestMethod.POST)
    public ModelAndView createQrcode(HttpServletRequest request, Integer num) throws WxErrorException {
        ModelAndView mv = new ModelAndView("wxcms/qrcode");
        mv.addObject("cur_nav", "qrcode");
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        if (num != null) {
            byte[] qrcode = WxApiClient.createQRCode(60, num, mpAccount);//有效期60s
            String url = UploadUtil.byteToImg(request.getServletContext().getRealPath("/"), qrcode);
            mv.addObject("qrcode", url);
        }
        mv.addObject("num", num);
        return mv;
    }

    //以根据openid群发文本消息为例
    @RequestMapping(value = "/wxapi/massSendTextMsg", method = RequestMethod.POST)
    public void massSendTextMsg(HttpServletResponse response, String openid, String content) throws WxErrorException {
        content = "群发文本消息";
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        String rstMsg = "根据openid群发文本消息失败";
        if (mpAccount != null && !StringUtils.isBlank(openid)) {
            List<String> openidList = new ArrayList<String>();
            openidList.add(openid);
            //根据openid群发文本消息
            JSONObject result = WxApiClient.massSendTextByOpenIds(openidList, content, mpAccount);
            try {
                if (result.getIntValue("errcode") != 0) {
                    response.getWriter().write("send failure");
                } else {
                    response.getWriter().write("send success");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ModelAndView mv = new ModelAndView("common/failure");
        mv.addObject("failureMsg", rstMsg);
    }

    /**
     * 发送客服消息
     *
     * @param openid ： 粉丝的openid
     * @return
     */
    @RequestMapping(value = "/wxapi/sendCustomTextMsg", method = RequestMethod.POST)
    public void sendCustomTextMsg(HttpServletRequest request, HttpServletResponse response, String openid) throws WxErrorException {
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        String content = "微信派官方测试客服消息";
        JSONObject result = WxApiClient.sendCustomTextMessage(openid, content, mpAccount);
        try {
            if (result.getIntValue("errcode") != 0) {
                response.getWriter().write("send failure");
            } else {
                response.getWriter().write("send success");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送模板消息
     *
     * @return
     */
    @RequestMapping(value = "/wxapi/sendTemplateMessage", method = RequestMethod.POST)
    public AjaxResult sendTemplateMessage(String openIds) throws WxErrorException {
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        TemplateMessage tplMsg = new TemplateMessage();

        String[] openIdArray = StringUtils.split(openIds, ",");
        for (String openId : openIdArray) {
            tplMsg.setOpenid(openId);
            //微信公众号号的template id，开发者自行处理参数
            tplMsg.setTemplateId("azx4q5sQjWUk1O3QY0MJSJkwePQmjR-T5rCyjyMUw8U");

            tplMsg.setUrl("#");
            Map<String, String> dataMap = new HashMap<String, String>();
            dataMap.put("first", "smartadmin管理后台已经上线，欢迎吐槽");
            dataMap.put("keyword1", "时间：" + DateUtil.changeDateTOStr(new Date()));
            dataMap.put("keyword2", "码云平台地址：https://gitee.com/qingfengtaizi/wxmp");
            dataMap.put("keyword3", "github平台地址：https://github.com/qingfengtaizi/wxmp-web");
            dataMap.put("remark", "我们期待您的加入");
            tplMsg.setDataMap(dataMap);

            JSONObject result = WxApiClient.sendTemplateMessage(tplMsg, mpAccount);
        }

        return AjaxResult.success();
    }

    /**
     * 微信异步返回
     *
     * @param requestBodyXml
     * @return
     */
    @RequestMapping(value = "/wxapi/wxipay_noity")
    public HTTPResultXml wxipay_noity(@RequestBody String requestBodyXml) {
        log.info("-------------------------------------wxipay_noity-----<0>-------------------requestBodyXml:" + requestBodyXml);

        Map map = new HashMap();
        try {
            map = com.wxmp.core.util.wx.TenpayUtil2.doXMLParseByDom4j(requestBodyXml);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String return_code = (String) map.get("return_code");
        String return_msg;
        if ("SUCCESS".equals(return_code)) {
            String transaction_id = (String) map.get("transaction_id");
            String out_trade_no = (String) map.get("out_trade_no");

            return_code = "<![CDATA[SUCCESS]]>";
            return_msg = "<![CDATA[OK]]>";
        } else {
            return_msg = (String) map.get("err_code_des");
            return_code = "<![CDATA[FAIL]]>";
            return_msg = "<![CDATA[" + return_msg + "]]>";
        }


        com.wxmp.core.util.wx.HTTPResultXml httpResultXml = new com.wxmp.core.util.wx.HTTPResultXml();
        httpResultXml.setReturn_code(return_code);
        httpResultXml.setReturn_msg(return_msg);

        //String openid = WxMemoryCacheClient.getOpenid(request.getSession().getId());//先从缓存中获取openid
	
	/*	if(!array_key_exists("transaction_id", $data)){
			$msg = "输入参数不正确";
			return false;
		}
		//查询订单，判断订单真实性
		if(!$this->Queryorder($data["transaction_id"])){
			$msg = "订单查询失败";
			return false;
		}
		*/
        return httpResultXml;
    }


    /**
     * 给粉丝发送文本消息
     *
     * @param msgId
     * @param openid
     * @return
     */
    @RequestMapping(value = "/wxapi/sendTextMsgByOpenId", method = RequestMethod.POST)
    public AjaxResult sendTextMsgByOpenId(String msgId, String openid) throws WxErrorException {
        MsgText msgText = msgTextService.getById(msgId);
        String content = msgText.getContent();
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        JSONObject result = WxApiClient.sendCustomTextMessage(openid, content, mpAccount);

        if (result.getIntValue("errcode") != 0) {
            return AjaxResult.failure(result.toString());
        } else {
            return AjaxResult.success();
        }
    }

    /**
     * 客服接口-发送图文消息
     *
     * @param id
     * @param openid
     * @return
     */
    @RequestMapping(value = "/wxapi/sendNewsByOpenId", method = RequestMethod.POST)
    public AjaxResult sendNewsByOpenId(String id, String openid) throws WxErrorException {

        MsgNews msgNews = this.msgNewsService.getById(id);

        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        JSONObject result = WxApiClient.sendCustomNews(openid, msgNews, mpAccount);
        log.info(" 客服接口-发送图文消息：" + result.toString());
        if (result.getIntValue("errcode") != 0) {
            return AjaxResult.failure(result.toString());
        } else {
            return AjaxResult.success();
        }
    }

    /**
     * 客服接口 -批量发送文本消息
     *
     * @param textId
     * @param openIds
     * @return
     */
    @RequestMapping(value = "/wxapi/batchSendText", method = RequestMethod.POST)
    public String batchSendText(String textId, String openIds) throws WxErrorException {
        String code = "1";
        MsgText msgText = msgTextService.getById(textId);
        String[] openIdAarry = {"0"};
        if (openIds.contains(",")) {
            openIdAarry = openIds.split(",");
        } else {
            openIdAarry[0] = openIds;
        }
        String content = msgText.getContent();

        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号

        for (String openid : openIdAarry) {
            JSONObject result = WxApiClient.sendCustomTextMessage(openid, content, mpAccount);
        }

        return code;
    }

    /**
     * 群发-文本消息
     *
     * @param textId
     * @param openIds
     * @return
     */
    @RequestMapping(value = "/wxapi/massSendTextByOpenIds", method = RequestMethod.POST)
    public String massSendTextByOpenIds(String textId, String openIds) throws WxErrorException {
        MsgText msgText = msgTextService.getById(textId);
        //分隔字符串
        String[] openIdArray = openIds.split(",");

        String content = msgText.getContent();

        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        //openids
        List<String> openidList = new ArrayList<>(Arrays.asList(openIdArray));
        JSONObject result = WxApiClient.massSendTextByOpenIds(openidList, content, mpAccount);
        log.info(" 群发-文本消息：" + result.toString());
        if (result.getIntValue("errcode") != 0) {
            return result.toString();//发送失败
        }
        return "1";//发送成功
    }

    /**
     * 高级群发-图文消息|
     *
     * @param newsId
     * @param openIds
     * @return
     */
    @RequestMapping(value = "/wxapi/massSendNewsByOpenIds", method = RequestMethod.POST)
    public String massSendNewsByOpenIds(String newsId, String openIds) throws WxErrorException {
        String code = "";
        MsgNews msgNews = this.msgNewsService.getById(newsId);

        List<MsgNews> msgNewsList = new ArrayList<>();
        msgNewsList.add(msgNews);
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        //先上传图文素材
        //JSONObject newsObject = WxApiClient.uploadNews(msgNewsList, mpAccount);
        //媒体id
        String media_id = msgNews.getMediaId();

        //分隔字符串
        String[] openIdAarry = openIds.split(",");
        //openids
        List<String> openidList = new ArrayList<>(Arrays.asList(openIdAarry));
        JSONObject massQesultObj = WxApiClient.massSendByOpenIds(openidList, media_id, MsgType.MPNEWS, mpAccount);

        if (massQesultObj.getIntValue("errcode") != 0) {
            code = massQesultObj.toString();
        } else {
            code = "1";//发送成功
        }
        return code;
    }


    /**
     * 高级群发-图文消息|
     *
     * @param mediaId
     * @param openIds
     * @return
     */
    @RequestMapping(value = "/wxapi/sendMaterialByOpenIds", method = RequestMethod.POST)
    public String sendMaterialByOpenIds(String mediaId, String openIds) throws WxErrorException {
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        //分隔字符串
        String[] openIdAarry = openIds.split(",");
        //openids
        List<String> openidList = new ArrayList<>(Arrays.asList(openIdAarry));
        JSONObject massQesultObj = WxApiClient.massSendByOpenIds(openidList, mediaId, MsgType.MPNEWS, mpAccount);

        if (massQesultObj.getIntValue("errcode") != 0) {
            return massQesultObj.toString();
        }
        return "1";//发送成功
    }

    //创建微信公众账号菜单
    @RequestMapping(value = "/wxapi/doPublishMenu")
    public AjaxResult doPublishMenu() throws WxErrorException {
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();
        if (mpAccount != null) {
            JSONObject rstObj = myService.publishMenu(mpAccount);
            if (rstObj != null) {//成功，更新菜单组
                return AjaxResult.success();
//				if(rstObj.containsKey("menu_id")){
//					ModelAndView mv = new ModelAndView("common/success");
//					mv.addObject("successMsg", "创建菜单成功");
//					code = "1";
//					return code;
//				}else if(rstObj.containsKey("errcode") && rstObj.getIntValue("errcode") == 0){
//					ModelAndView mv = new ModelAndView("common/success");
//					mv.addObject("successMsg", "创建菜单成功");
//					code = "1";
//					return code;
//				}
            }
        }
        return AjaxResult.failure();
    }


    //删除微信公众账号菜单
    @RequestMapping(value = "/wxapi/deletePublicMenu")
    public String deletePublicMenu(HttpServletRequest request) throws WxErrorException {
        JSONObject rstObj = null;
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        String code;
        if (mpAccount != null) {
            rstObj = myService.deleteMenu(mpAccount);
            if (rstObj != null && rstObj.getIntValue("errcode") == 0) {
                code = "1";
                return code;
            }
        }

        String failureMsg = "删除菜单失败";
        if (rstObj != null) {
            failureMsg += ErrCode.errMsg(rstObj.getIntValue("errcode"));
        }
        code = failureMsg;
        return code;
    }

    /**
     * 统计分析
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return
     * @throws WxErrorException
     */
    @GetMapping(value = "/wxapi/dataCube")
    public AjaxResult dataCube(String type, String start, String end) throws WxErrorException {
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
        String accessToken = WxApiClient.getAccessToken(mpAccount.getAppid(), mpAccount.getAppsecret(), mpAccount.getAccount());
        JSONObject result = WxApi.forDataCube(accessToken, type, start, end);
        return AjaxResult.success(result);
    }


}