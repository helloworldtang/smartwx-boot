package com.wxmp.wxcms.ctrl;

import com.alibaba.fastjson.JSONObject;
import com.wxmp.core.common.BaseCtrl;
import com.wxmp.core.property.ResUploadProperties;
import com.wxmp.core.util.AjaxResult;
import com.wxmp.core.util.MediaTypeUtil;
import com.wxmp.wxapi.process.MediaType;
import com.wxmp.wxapi.process.MpAccount;
import com.wxmp.wxapi.process.WxApi;
import com.wxmp.wxapi.process.WxApiClient;
import com.wxmp.wxapi.process.WxMemoryCacheClient;
import com.wxmp.wxcms.domain.ImgResource;
import com.wxmp.wxcms.service.ImgResourceService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * @author : hermit
 */
@Controller
@RequestMapping("managerImg")
public class ImgResourceCtrl extends BaseCtrl {

    @Autowired
    private ImgResourceService imgResourceService;

    @Autowired
    private ResUploadProperties resUploadProperties;


    @RequestMapping(value = "/list")
    @ResponseBody
    public AjaxResult list(ImgResource searchEntity) {
        List<ImgResource> pageList = imgResourceService.getImgListByPage(searchEntity);
        return getResult(searchEntity, pageList);
    }

    /**
     * 上传图片(需要同步微信用)-抛弃
     *
     * @return
     */
    @ResponseBody
    @RequestMapping("uploadImg")
    public AjaxResult saveFile(MultipartFile file) throws Exception {
        JSONObject obj = new JSONObject();
        if (null == file) {
            obj.put("message", "没有图片上传");
        }
        //原文件名称
        String trueName = Objects.requireNonNull(file).getOriginalFilename();
        //文件后缀名
        String ext = FilenameUtils.getExtension(trueName);
        if (!MediaTypeUtil.isImg(ext)) {
            obj.put("message", "图片格式不正确");
        }

        if (file.getSize() > 1024 * 1024) {

            obj.put("message", "上传图片不能大于1M");
        }

        //系统生成的文件名
        String fileName = System.currentTimeMillis() + new Random().nextInt(10000) + "." + ext;
        //图片上传路径
        String resURL = resUploadProperties.getUrl();
        String filePath = request.getSession().getServletContext().getRealPath("/");

        //读取配置文上传件的路径
        if (resUploadProperties.getPath() != null) {
            filePath = resUploadProperties.getPath() + fileName;
        } else {
            filePath = filePath + "/upload/" + fileName;
        }

        File saveFile = new File(filePath);

        if (!saveFile.exists()) {
            saveFile.mkdirs();
        }
        file.transferTo(saveFile);

        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号

        String accessToken = WxApiClient.getAccessToken(mpAccount.getAppid(), mpAccount.getAppsecret(), mpAccount.getAccount());
        //添加永久图片--图文的封面应该为thumb
        String materialType = MediaType.Image.toString();
        //将图片同步到微信，返回mediaId
        JSONObject imgResultObj = WxApi.addMateria(accessToken, materialType, filePath, null);
//		JSONObject imgResultObj = WxApiClient.addMaterial(filePath, materialType, mpAccount);

        //微信返回来的媒体素材id
        String imgMediaId = imgResultObj.getString("media_id");
        //图片url
        String imgUrl = imgResultObj.getString("url");

        ImgResource img = new ImgResource();
        img.setName(fileName);
        img.setSize((int) file.getSize());
        img.setTrueName(trueName);
        img.setType(ext);
        img.setUrl(resURL + fileName);
        img.setHttpUrl(imgUrl);
        img.setMediaId(imgMediaId);

        String result = this.imgResourceService.addImg(img);
        if (result != null) {
            obj.put("url", result);
            obj.put("imgMediaId", imgMediaId);
        } else {
            obj.put("url", null);
            obj.put("imgMediaId", null);
        }
        return AjaxResult.success(obj);
    }

    /**
     * 上传图片(富文本)
     *
     * @return
     */
    @ResponseBody
    @RequestMapping("uploadFile")
    public Map uploadFile(MultipartFile file) throws Exception {
        //原文件名称
        String trueName = file.getOriginalFilename();
        //文件后缀名
        String ext = FilenameUtils.getExtension(trueName);

        //系统生成的文件名
        String fileName = System.currentTimeMillis() + new Random().nextInt(10000) + "." + ext;
        //图片上传路径
        String resURL = resUploadProperties.getUrl();
        String filePath = request.getSession().getServletContext().getRealPath("/");

        //读取配置文上传件的路径
        if (resUploadProperties.getPath() != null) {
            filePath = resUploadProperties.getPath() + fileName;
        } else {
            filePath = filePath + "/upload/" + fileName;
        }

        File saveFile = new File(filePath);

        if (!saveFile.exists()) {
            saveFile.mkdirs();
        }
        file.transferTo(saveFile);
        //构造返回参数
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        map.put("code", 0);//0表示成功，1失败
        map.put("msg", "上传成功");//提示消息
        map.put("data", mapData);//提示消息
        mapData.put("src", resURL + fileName);//图片url
        mapData.put("title", fileName);//图片名称，这个会显示在输入框里
        return map;
    }

    /**
     * 删除图片
     *
     * @param id
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping("delMediaImg")
    public AjaxResult delMediaImg(String id) throws Exception {
        ImgResource img = imgResourceService.getImg(id);
        WxApiClient.deleteMaterial(img.getMediaId(), WxMemoryCacheClient.getMpAccount());
        imgResourceService.delImg(id);
        return AjaxResult.deleteSuccess();
    }
}
