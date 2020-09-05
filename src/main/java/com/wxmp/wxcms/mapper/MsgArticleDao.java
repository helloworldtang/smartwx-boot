package com.wxmp.wxcms.mapper;

import com.wxmp.wxcms.domain.MsgArticle;

import java.util.List;

public interface MsgArticleDao {

    List<MsgArticle> getByNewsId(int id);

    MsgArticle getById(int id);

    void add(MsgArticle article);

    void insertByBatch(List<MsgArticle> articles);

    void update(MsgArticle article);

    void delete(int id);

    void deleteByBatch(int id);
}
