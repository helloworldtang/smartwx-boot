package com.wxmp.wxcms.ctrl;

import com.alibaba.fastjson.JSONObject;
import com.wxmp.core.common.BaseCtrl;
import com.wxmp.core.exception.WxErrorException;
import com.wxmp.core.property.ResUploadProperties;
import com.wxmp.core.util.AjaxResult;
import com.wxmp.wxapi.process.MpAccount;
import com.wxmp.wxapi.process.WxApi;
import com.wxmp.wxapi.process.WxApiClient;
import com.wxmp.wxapi.process.WxMemoryCacheClient;
import com.wxmp.wxcms.domain.ImgResource;
import com.wxmp.wxcms.domain.MediaFiles;
import com.wxmp.wxcms.service.ImgResourceService;
import com.wxmp.wxcms.service.MediaFileService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * 语音和视频控制器
 *
 * @author nigualding
 * 后面会把图片和语音上传合并到一个方法中。
 */
@Controller
@RequestMapping("mediaFile")
public class MediaFilesCtrl extends BaseCtrl {

    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private ImgResourceService imgResourceService;

    @Autowired
    private ResUploadProperties resUploadProperties;

    /**
     * 分页展示
     *
     * @param searchEntity
     * @return
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public AjaxResult list(MediaFiles searchEntity) {
        List<MediaFiles> pageList = mediaFileService.getMediaListByPage(searchEntity);
        return getResult(searchEntity, pageList);
    }

    /**
     * 添加视频素材
     *
     * @param mediaFile
     * @return
     * @throws WxErrorException
     */
    @RequestMapping(value = "/addVideo")
    @ResponseBody
    public AjaxResult addVideo(MediaFiles mediaFile) throws WxErrorException {
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();
        String accessToken = WxApiClient.getAccessToken(mpAccount.getAppid(), mpAccount.getAppsecret(), mpAccount.getAccount());
        Map<String, String> params = new HashMap<>();
        JSONObject json = new JSONObject();
        json.put("title", mediaFile.getTitle());
        json.put("introduction", mediaFile.getIntroduction());
        params.put("description", json.toJSONString());
        JSONObject result = WxApi.addMateria(accessToken, "video", mediaFile.getUrl(), params);
        mediaFile.setMediaId(result.getString("media_id"));
        mediaFile.setMediaType("video");
        mediaFile.setCreateTime(new Date(System.currentTimeMillis()));
        mediaFile.setUpdateTime(new Date(System.currentTimeMillis()));
        mediaFileService.add(mediaFile);
        return AjaxResult.saveSuccess();
    }

    /**
     * 删除素材（微信端和本地数据库）
     *
     * @param mediaId
     * @return
     * @throws WxErrorException
     */
    @ResponseBody
    @RequestMapping("delMediaFile")
    public AjaxResult delMediaFile(String mediaId) throws WxErrorException {
        WxApiClient.deleteMaterial(mediaId, WxMemoryCacheClient.getMpAccount());
        mediaFileService.deleteByMediaId(mediaId);
        return AjaxResult.deleteSuccess();
    }

    /**
     * 上传素材文件到本地
     *
     * @param file
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping("uploadFile")
    public AjaxResult uploadFile(MultipartFile file) throws Exception {
        //原文件名称
        String trueName = file.getOriginalFilename();
        //文件后缀名
        String ext = FilenameUtils.getExtension(trueName);

        //系统生成的文件名
        String fileName = System.currentTimeMillis() + new Random().nextInt(10000) + "." + ext;
        //文件上传路径
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
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("src", resURL + fileName);//文件url
        mapData.put("url", filePath);//文件绝对路径url
        mapData.put("title", fileName);//图片名称，这个会显示在输入框里

        return AjaxResult.success(mapData);
    }

    /**
     * 添加语音\图片\缩略图素材
     *
     * @param file
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping("addMateria")
    public AjaxResult addMateria(MultipartFile file, String type) throws Exception {
        JSONObject obj = new JSONObject();
        if (null == file) {
            obj.put("message", "没有文件上传");
        }
        //原文件名称
        String trueName = Objects.requireNonNull(file).getOriginalFilename();
        //文件后缀名
        String ext = FilenameUtils.getExtension(trueName);

        //系统生成的文件名
        String fileName = System.currentTimeMillis() + new Random().nextInt(10000) + "." + ext;
        //文件上传路径
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


        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();
        String accessToken = WxApiClient.getAccessToken(mpAccount.getAppid(), mpAccount.getAppsecret(), mpAccount.getAccount());
        JSONObject result = WxApi.addMateria(accessToken, type, filePath, null);
        String mediaId = result.getString("media_id");
        //图片或者图文的缩略图
        if (type.equals("image") || type.equals("thumb")) {
            //图片url
            String imgUrl = result.getString("url");
            ImgResource img = new ImgResource();
            img.setName(fileName);
            img.setSize((int) file.getSize());
            img.setTrueName(trueName);
            img.setType(type);//这里用来区分image和thumb
            img.setUrl(resURL + fileName);
            img.setHttpUrl(imgUrl);
            img.setMediaId(mediaId);

            String imgRes = this.imgResourceService.addImg(img);
            obj.put("url", imgRes);
            obj.put("imgMediaId", mediaId);
            return AjaxResult.success(obj);
        } else {//音频 voice
            MediaFiles mediaFile = new MediaFiles();
            mediaFile.setUploadUrl(resURL + fileName);
            mediaFile.setUrl(filePath);
            mediaFile.setTitle(fileName);//用title保存文件名
            mediaFile.setMediaId(mediaId);
            mediaFile.setMediaType(type);
            mediaFile.setCreateTime(new Date(System.currentTimeMillis()));
            mediaFile.setUpdateTime(new Date(System.currentTimeMillis()));
            mediaFileService.add(mediaFile);
            return AjaxResult.saveSuccess();
        }
    }
}
