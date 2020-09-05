package com.wxmp.wxcms.ctrl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wxmp.core.common.BaseCtrl;
import com.wxmp.core.exception.WxErrorException;
import com.wxmp.core.spring.SpringFreemarkerContextPathUtil;
import com.wxmp.core.util.AjaxResult;
import com.wxmp.wxapi.process.MediaType;
import com.wxmp.wxapi.process.MpAccount;
import com.wxmp.wxapi.process.WxApiClient;
import com.wxmp.wxapi.process.WxMemoryCacheClient;
import com.wxmp.wxcms.domain.MediaFiles;
import com.wxmp.wxcms.domain.MsgArticle;
import com.wxmp.wxcms.domain.MsgNews;
import com.wxmp.wxcms.service.MsgArticleService;
import com.wxmp.wxcms.service.MsgNewsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wxmp.core.util.DateUtilOld.COMMON_FULL;


@RestController
public class MsgNewsCtrl extends BaseCtrl {

    @Autowired
    private MsgNewsService msgNewsService;

    @Autowired
    private MsgArticleService articleService;

    @RequestMapping(value = "/msgnews/detail")
    public AjaxResult getById(String id) {
        return AjaxResult.success(msgNewsService.getById(id));
    }

    @GetMapping(value = "/msgnews/list")
    public AjaxResult list(MsgNews searchEntity) {
        // TODO: 2019/9/5 此接口有报错。需要处理
        List<MsgNews> pageList = msgNewsService.getWebNewsListByPage(searchEntity);
        return getResult(searchEntity, pageList);
    }

    /**
     * 将保存到本地的素材信息同步到微信
     *
     * @param newsId
     * @return
     */
    @RequestMapping(value = "/msgnews/sendNewsMaterial", method = RequestMethod.POST)
    public String sendNewsMaterial(String newsId, HttpServletRequest request) throws Exception {
        String code = "";
        MsgNews msgNews = msgNewsService.getById(newsId);
        List<MsgNews> msgNewsList = new ArrayList<MsgNews>();
        msgNewsList.add(msgNews);
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();// 获取缓存中的唯一账号
        // 添加永久图片
        String materialType = MediaType.Image.toString();
        String imgPath = msgNews.getPicpath();
        String fileName = imgPath.substring(imgPath.lastIndexOf("/") + 1, imgPath.length());
        String filePath = SpringFreemarkerContextPathUtil.getImgBasePath(request, fileName);
        // 返回mediaId
        JSONObject imgResultObj = WxApiClient.addMaterial(filePath, materialType, mpAccount);
        // 上传图片的id
        String imgMediaId = "";
        if (imgResultObj != null && imgResultObj.containsKey("media_id")) {
            imgMediaId = imgResultObj.getString("media_id");

            JSONObject newsResultObj = WxApiClient.addNewsMaterial(msgNewsList, imgMediaId, mpAccount);
            if (newsResultObj != null && newsResultObj.containsKey("media_id")) {
                String newsMediaId = newsResultObj.getString("media_id");
                MsgNews entity = new MsgNews();
                entity.setId(Long.valueOf(newsId));
                entity.setMediaId(newsMediaId);
                msgNewsService.updateMediaId(entity);
                code = "1";
            } else {
                code = Objects.requireNonNull(newsResultObj).toString();
            }
        } else {
            // 上传永久图片失败
            code = "-1";
        }

        return code;
    }

    /**
     * 添加单图文
     *
     * @param msgNews
     * @param request
     * @return
     */
    @RequestMapping(value = "/msgnews/addSingleNews", method = RequestMethod.POST)
    public AjaxResult addSingleNews(MsgNews msgNews, HttpServletRequest request)
            throws Exception {

        String filePath = request.getSession().getServletContext().getRealPath("/");

        String description = msgNews.getDescription();
        String description2 = msgNews.getDescription();

        description = description.replaceAll("'", "\"");
        // 去多个img的src值
        String subFilePath = "";
        String subOldFilePath = "";
        if (description.contains("img")) {
            Pattern p = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
            Matcher m = p.matcher(description);

            while (m.find()) {
                String imgSrc = m.group(1);
                subOldFilePath += imgSrc + ",";
                String[] split = imgSrc.split("/");
                int k = imgSrc.indexOf(split[split.length - 2]);
                String subImgSrc = imgSrc.substring(k, imgSrc.length());
                subFilePath += filePath + subImgSrc + ",";
            }
        }
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();// 获取缓存中的唯一账号
        if (StringUtils.isNotBlank(subFilePath)) {

            subFilePath = subFilePath.substring(0, subFilePath.length() - 1);
            subOldFilePath = subOldFilePath.substring(0, subOldFilePath.length() - 1);

            // 本地图片地址
            String[] imgPathArry = subFilePath.split(",");
            String[] imgOldPathArry = subOldFilePath.split(",");

            String[] newPathArry = new String[imgPathArry.length];
            for (int i = 0; i < imgPathArry.length; i++) {
                String newFilePath = imgPathArry[i];
                // 将图片上传到微信，返回url
                JSONObject imgResultObj = WxApiClient.uploadMaterialImg(newFilePath, mpAccount);
                // 上传图片的id
                // String contentImgMediaId = "";
                String contentContentUrl = "";
                if (imgResultObj != null && imgResultObj.containsKey("url")) {
                    // 图片url
                    contentContentUrl = imgResultObj.getString("url");
                }
                newPathArry[i] = contentContentUrl;
            }

            for (int i = 0; i < imgPathArry.length; i++) {
                description = description.replace(imgOldPathArry[i], newPathArry[i]);
            }
        }

        // 内容保存
        msgNews.setDescription(description);

        List<MsgNews> msgNewsList = new ArrayList<MsgNews>();
        msgNewsList.add(msgNews);
        // 封面图片媒体id
        String imgMediaId = msgNews.getThumbMediaId();

        JSONObject resultObj = WxApiClient.addNewsMaterial(msgNewsList, imgMediaId, mpAccount);

        if (resultObj != null && resultObj.containsKey("media_id")) {
            String newsMediaId = resultObj.getString("media_id");
            JSONObject newsResult = WxApiClient.getMaterial(newsMediaId, mpAccount);

            JSONArray articles = newsResult.getJSONArray("news_item");
            JSONObject article = (JSONObject) articles.get(0);
            MsgNews newsPo = new MsgNews();
            newsPo.setMultType(1);// 指定为1，代表单图文
            newsPo.setTitle(article.getString("title"));
            newsPo.setAuthor(article.getString("author"));
            newsPo.setBrief(article.getString("digest"));
            newsPo.setDescription(description2);
            newsPo.setFromurl(article.getString("content_source_url"));
            newsPo.setUrl(article.getString("url"));
            newsPo.setShowpic(article.getIntValue("show_cover_pic"));
            newsPo.setPicpath(msgNews.getPicpath());
            newsPo.setMediaId(newsMediaId);
            newsPo.setThumbMediaId(imgMediaId);
            newsPo.setNewsIndex(0);

            MediaFiles entity = new MediaFiles();
            entity.setMediaId(newsMediaId);
            entity.setMediaType("news");
            entity.setCreateTime(COMMON_FULL.getLongDate(Long.parseLong(newsResult.getString("create_time"))));
            entity.setUpdateTime(COMMON_FULL.getLongDate(Long.parseLong(newsResult.getString("update_time"))));

            int resultCount = this.msgNewsService.addSingleNews(newsPo, entity);

            if (resultCount > 0) {
                return AjaxResult.success();
            } else {
                return AjaxResult.failure();
            }
        }
        return AjaxResult.failure();

    }

    /**
     * 添加多图文
     *
     * @param rows
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/msgnews/addMoreNews", method = RequestMethod.POST)
    public AjaxResult addMoreNews(String rows, HttpServletRequest request) throws Exception {

        String filePath = request.getSession().getServletContext().getRealPath("/");

        List<MsgArticle> listArts = new ArrayList<MsgArticle>();// 数据库所有图文集合
        JSONArray arrays = JSONArray.parseArray(rows);
        JSONArray arryarticles = new JSONArray();// 微信所用json集合
        MsgNews msgNew = new MsgNews();
        for (int i = 0; i < arrays.size(); i++) {
            JSONObject obj = arrays.getJSONObject(i);
            JSONObject json = new JSONObject();
            json.put("title", obj.getString("title"));
            json.put("author", obj.getString("author"));
            json.put("thumb_media_id", obj.getString("thumbMediaId") == null ? "" : obj.getString("thumbMediaId"));
            json.put("digest", obj.getString("brief") == null ? "" : obj.getString("brief"));
            json.put("show_cover_pic", obj.getIntValue("showpic"));
            json.put("content_source_url", obj.getString("fromurl") == null ? "" : obj.getString("fromurl"));
            MsgArticle art = new MsgArticle();
            art.setNewsIndex(i);
            art.setTitle(obj.getString("title"));
            art.setAuthor(obj.getString("author") == null ? "" : obj.getString("author"));
            art.setContentSourceUrl(obj.getString("fromurl") == null ? "" : obj.getString("fromurl"));
            art.setDigest(obj.getString("brief") == null ? "" : obj.getString("brief"));
            art.setPicUrl(obj.getString("picpath") == null ? "" : obj.getString("picpath"));
            art.setShowCoverPic(obj.getIntValue("showpic"));
            art.setThumbMediaId(obj.getString("thumbMediaId") == null ? "" : obj.getString("thumbMediaId"));
            art.setContent(obj.getString("description") == null ? "" : obj.getString("description"));
            if (i == 0) {
                msgNew.setAuthor(art.getAuthor());
                msgNew.setBrief(art.getDigest());
                msgNew.setDescription(art.getContent());
                msgNew.setFromurl(art.getContentSourceUrl());
                msgNew.setMultType(2);
                msgNew.setPicpath(art.getPicUrl());
                msgNew.setShowpic(art.getShowCoverPic());
                msgNew.setTitle(art.getTitle());
                msgNew.setThumbMediaId(art.getThumbMediaId());
            }
            // 注意这是图文正文部分
            String description = obj.getString("description") == null ? "" : obj.getString("description");
            description = description.replaceAll("'", "\"");
            // 去多个img的src值
            String subFilePath = "";
            String subOldFilePath = "";
            if (description.contains("img")) {
                Pattern p = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
                Matcher m = p.matcher(description);

                while (m.find()) {
                    String imgSrc = m.group(1);
                    subOldFilePath += imgSrc + ",";
                    String[] split = imgSrc.split("/");
                    int k = imgSrc.indexOf(split[split.length - 2]);
                    String subImgSrc = imgSrc.substring(k, imgSrc.length());
                    subFilePath += filePath + subImgSrc + ",";
                }
            }
            if (StringUtils.isNotBlank(subFilePath)) {

                subFilePath = subFilePath.substring(0, subFilePath.length() - 1);
                subOldFilePath = subOldFilePath.substring(0, subOldFilePath.length() - 1);

                MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();// 获取缓存中的唯一账号
                // 本地图片地址
                String[] imgPathArry = subFilePath.split(",");
                String[] imgOldPathArry = subOldFilePath.split(",");

                String[] newPathArry = new String[imgPathArry.length];
                for (int j = 0; j < imgPathArry.length; j++) {
                    String newFilePath = imgPathArry[j];
                    // 添加永久图片
                    String materialType = MediaType.Image.toString();
                    // 将图片上传到微信，返回url
                    JSONObject imgResultObj = WxApiClient.uploadMaterialImg(newFilePath, mpAccount);

                    // 上传图片的id
                    // String contentImgMediaId = "";
                    String contentContentUrl = "";
                    if (imgResultObj != null && imgResultObj.containsKey("url")) {
                        // 微信返回来的媒体素材id
                        // contentImgMediaId = imgResultObj.getString("media_id");
                        // 图片url
                        contentContentUrl = imgResultObj.getString("url");
                    }
                    newPathArry[j] = contentContentUrl;
                }

                for (int j = 0; j < imgPathArry.length; j++) {
                    description = description.replace(imgOldPathArry[j], newPathArry[j]);
                }
            }
            json.put("content", description);
            arryarticles.add(json);
            listArts.add(art);
        }

        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();// 获取缓存中的唯一账号
        // 添加多图文永久素材
        JSONObject resultObj = WxApiClient.addMoreNewsMaterial2(arryarticles, mpAccount);
        // 数据库存储使用
        List<MsgArticle> listArticles = new ArrayList<MsgArticle>();// 数据库所有图文集合

        if (resultObj != null && resultObj.containsKey("media_id")) {
            String newsMediaId = resultObj.getString("media_id");
            msgNew.setMediaId(newsMediaId);
            JSONObject newsResult = WxApiClient.getMaterial(newsMediaId, mpAccount);

            JSONArray articles = newsResult.getJSONArray("news_item");

            for (int i = 0; i < articles.size(); i++) {
                JSONObject article = (JSONObject) articles.get(i);
                if (i == 0) {
                    msgNew.setUrl(article.getString("url"));
                }
                MsgArticle msgart = listArts.get(i);
                msgart.setUrl(article.getString("url"));
                msgart.setMediaId(newsMediaId);
                listArticles.add(msgart);
            }
            msgNew.setArticles(listArticles);

            int bl = this.msgNewsService.addMoreNews(msgNew);
            if (bl == 1) {
                return AjaxResult.success();
            }
        }
        return AjaxResult.failure();
    }

    /**
     * 删除永久图文素材
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/msgnews/deleteMaterial", method = RequestMethod.POST)
    public AjaxResult deleteMaterial(String id) throws WxErrorException {
        MsgNews news = msgNewsService.getById(id);
        // 添加多图文永久素材
        JSONObject jsonObject = WxApiClient.deleteMaterial(news.getMediaId(), WxMemoryCacheClient.getMpAccount());

        if (null != jsonObject && jsonObject.containsKey("errcode") && jsonObject.getIntValue("errcode") == 0) {

            try {
                this.msgNewsService.delete(news);
                return AjaxResult.deleteSuccess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return AjaxResult.failure();
    }

    /**
     * 跳转到更新页面
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/msgnews/toUpdateSingleNews", method = RequestMethod.POST)
    public AjaxResult toUpdateSingleNews(String id) {
        MsgNews newsObj = msgNewsService.getById(id);

        return AjaxResult.success(newsObj);
    }

    /**
     * 更新单图文素材
     *
     * @param msgNews
     * @param request
     * @return
     */
    @RequestMapping(value = "/msgnews/updateSingleNews", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult updateSingleNews(MsgNews msgNews, HttpServletRequest request) throws Exception {
        String filePath = request.getSession().getServletContext().getRealPath("/");

        String description = msgNews.getDescription();
        String description2 = msgNews.getDescription();
        description = description.replaceAll("'", "\"");
        // 去多个img的src值
        String subFilePath = "";
        String subOldFilePath = "";
        if (description.contains("img")) {
            Pattern p = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
            Matcher m = p.matcher(description);

            while (m.find()) {
                String imgSrc = m.group(1);
                subOldFilePath += imgSrc + ",";
                String[] split = imgSrc.split("/");
                int k = imgSrc.indexOf(split[split.length - 2]);
                String subImgSrc = imgSrc.substring(k, imgSrc.length());
                subFilePath += filePath + subImgSrc + ",";
            }
        }
        if (StringUtils.isNotBlank(subFilePath)) {
            subFilePath = subFilePath.substring(0, subFilePath.length() - 1);
            subOldFilePath = subOldFilePath.substring(0, subOldFilePath.length() - 1);
            // 本地图片地址
            String[] imgPathArry = subFilePath.split(",");
            String[] imgOldPathArry = subOldFilePath.split(",");

            String[] newPathArry = new String[imgPathArry.length];
            for (int i = 0; i < imgPathArry.length; i++) {
                String newFilePath = imgPathArry[i];
                // 添加永久图片
                String materialType = MediaType.Image.toString();
                // 将图片上传到微信，返回url
                JSONObject imgResultObj = WxApiClient.uploadMaterialImg(newFilePath,
                        WxMemoryCacheClient.getMpAccount());

                // 上传图片的id
                // String contentImgMediaId = "";
                String contentContentUrl = "";
                if (imgResultObj != null && imgResultObj.containsKey("url")) {
                    // 微信返回来的媒体素材id
                    // contentImgMediaId = imgResultObj.getString("media_id");
                    // 图片url
                    contentContentUrl = imgResultObj.getString("url");
                }
                newPathArry[i] = contentContentUrl;
            }

            for (int i = 0; i < imgPathArry.length; i++) {
                description = description.replace(imgOldPathArry[i], newPathArry[i]);
            }
        }

        // 内容保存
        msgNews.setDescription(description);
        List<MsgNews> msgNewsList = new ArrayList<MsgNews>();

        msgNewsList.add(msgNews);

        String mediaId = msgNews.getMediaId();

        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();// 获取缓存中的唯一账号

        JSONObject resultObj = WxApiClient.updateNewsMaterial(msgNewsList, 0, mediaId, mpAccount);

        if (null != resultObj && resultObj.containsKey("errcode") && resultObj.getIntValue("errcode") == 0) {
            msgNews.setDescription(description2);
            try {
                // 更新成功
                this.msgNewsService.updateSingleNews(msgNews);
                return AjaxResult.updateSuccess();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return AjaxResult.failure();
    }

    /**
     * 跳转到更新页面
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/msgnews/toUpdateMoreNews")
    public AjaxResult toUpdateMoreNews(String id) {
        MsgNews newsObj = msgNewsService.getById(id);
        return AjaxResult.success(newsObj.getArticles());
    }

    /**
     * 更新多图文
     *
     * @param rows
     * @param request
     * @return
     */
    @RequestMapping(value = "/msgnews/updateSubMoreNews", method = RequestMethod.POST)
    public AjaxResult updateMoreNews(String rows, HttpServletRequest request) throws Exception {
        String filePath = request.getSession().getServletContext().getRealPath("/");
        MsgArticle article = JSONObject.parseObject(rows, MsgArticle.class);
        String description = article.getContent();
        String description2 = article.getContent();
        description = description.replaceAll("'", "\"");
        // 去多个img的src值
        String subFilePath = "";
        String subOldFilePath = "";
        if (description.contains("img")) {
            Pattern p = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
            Matcher m = p.matcher(description);

            while (m.find()) {
                String imgSrc = m.group(1);
                subOldFilePath += imgSrc + ",";
                String[] split = imgSrc.split("/");
                int k = imgSrc.indexOf(split[split.length - 2]);
                String subImgSrc = imgSrc.substring(k, imgSrc.length());
                subFilePath += filePath + subImgSrc + ",";
            }
        }
        if (StringUtils.isNotBlank(subFilePath)) {

            subFilePath = subFilePath.substring(0, subFilePath.length() - 1);
            subOldFilePath = subOldFilePath.substring(0, subOldFilePath.length() - 1);

            // 本地图片地址
            String[] imgPathArry = subFilePath.split(",");
            String[] imgOldPathArry = subOldFilePath.split(",");

            String[] newPathArry = new String[imgPathArry.length];
            for (int i = 0; i < imgPathArry.length; i++) {
                String newFilePath = imgPathArry[i];
                // 添加永久图片
                String materialType = MediaType.Image.toString();
                // 将图片上传到微信，返回url
                JSONObject imgResultObj = WxApiClient.uploadMaterialImg(newFilePath,
                        WxMemoryCacheClient.getMpAccount());

                // 上传图片的id
                // String contentImgMediaId = "";
                String contentContentUrl = "";
                if (imgResultObj != null && imgResultObj.containsKey("url")) {
                    // 微信返回来的媒体素材id
                    // contentImgMediaId = imgResultObj.getString("media_id");
                    // 图片url
                    contentContentUrl = imgResultObj.getString("url");
                }
                newPathArry[i] = contentContentUrl;
            }

            for (int i = 0; i < imgPathArry.length; i++) {
                description = description.replace(imgOldPathArry[i], newPathArry[i]);
            }
        }

        // 内容保存
        article.setContent(description);
        MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();// 获取缓存中的唯一账号

        JSONObject jsonObj = new JSONObject();
        // 上传图片素材
        jsonObj.put("thumb_media_id", article.getThumbMediaId());
        if (article.getAuthor() != null) {
            jsonObj.put("author", article.getAuthor());
        } else {
            jsonObj.put("author", "");
        }
        if (article.getTitle() != null) {
            jsonObj.put("title", article.getTitle());
        } else {
            jsonObj.put("title", "");
        }
        if (article.getContentSourceUrl() != null) {
            jsonObj.put("content_source_url", article.getContentSourceUrl());
        } else {
            jsonObj.put("content_source_url", "");
        }
        if (article.getDigest() != null) {
            jsonObj.put("digest", article.getDigest());
        } else {
            jsonObj.put("digest", "");
        }
        if (article.getShowCoverPic() != null) {
            jsonObj.put("show_cover_pic", article.getShowCoverPic());
        } else {
            jsonObj.put("show_cover_pic", 1);
        }
        jsonObj.put("content", article.getContent());

        JSONObject resultObj = WxApiClient.updateNewsMaterial2(jsonObj, article.getNewsIndex(), article.getMediaId(),
                mpAccount);

        if (null != resultObj && resultObj.containsKey("errcode") && resultObj.getIntValue("errcode") == 0) {
            article.setContent(description2);
            // 更新成功
            this.articleService.update(article);
            // 修改图文news表数据
            MsgNews msgNews = this.msgNewsService.getById(String.valueOf(article.getNewsId()));
            List<MsgArticle> newArticles = msgNews.getArticles();
            if (Objects.equals(newArticles.get(0).getArId(), article.getArId())) {
                // 这里只修改title 为了模糊查询的时候可以查询到数据
                msgNews.setTitle(article.getTitle());
                this.msgNewsService.updateMediaId(msgNews);
            }
            return AjaxResult.updateSuccess();
        } else {
            return AjaxResult.failure();
        }

    }

}
