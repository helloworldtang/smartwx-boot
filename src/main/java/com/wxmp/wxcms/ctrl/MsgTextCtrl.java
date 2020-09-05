package com.wxmp.wxcms.ctrl;

import com.wxmp.core.common.BaseCtrl;
import com.wxmp.core.util.AjaxResult;
import com.wxmp.wxcms.domain.MsgText;
import com.wxmp.wxcms.service.MsgTextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author hermit
 * @version 2.0
 * @date 2018-04-17 10:54:58
 */
@RestController
public class MsgTextCtrl extends BaseCtrl {

    @Autowired
    private MsgTextService entityService;

    @RequestMapping(value = "/msgtext/getById")
    public AjaxResult getById(String id) {
        MsgText text = entityService.getById(id);
        return AjaxResult.success(text);
    }

    @RequestMapping(value = "/msgtext/list")
    public AjaxResult list(MsgText searchEntity) {
        List<MsgText> pageList = entityService.getMsgTextByPage(searchEntity);
        return getResult(searchEntity, pageList);
    }

    /**
     * 修改/添加
     *
     * @param entity
     * @return
     */
    @RequestMapping(value = "/msgtext/updateText")
    public AjaxResult updateText(MsgText entity) {
        // 文本消息的关键词需要保证唯一性
        MsgText msgText = entityService.getRandomMsg(entity.getInputcode());
        if (msgText != null) {
            return AjaxResult.failure("关键词重复");
        }
        if (entity.getId() != null) {
            entityService.update(entity);
            // 更新成功
            return AjaxResult.updateSuccess();
        } else {
            // 添加成功
            entityService.add(entity);
            return AjaxResult.saveSuccess();
        }
    }

    @RequestMapping(value = "/msgtext/deleteById")
    public AjaxResult deleteById(String baseId) {
        entityService.delete(baseId);
        return AjaxResult.deleteSuccess();
    }

}
