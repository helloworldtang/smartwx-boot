package com.wxmp.wxcms.mapper;

import com.wxmp.wxcms.domain.AccountFans;

import java.util.List;

public interface AccountFansDao {

    AccountFans getById(String id);

    AccountFans getByOpenId(String openId);

    List<AccountFans> list(AccountFans searchEntity);

    List<AccountFans> getFansListByPage(AccountFans searchEntity);

    AccountFans getLastOpenId();

    void add(AccountFans entity);

    void addList(List<AccountFans> list);

    void update(AccountFans entity);

    void delete(AccountFans entity);

    void deleteByOpenId(String openId);

    /**
     * 根据多个openId查看粉丝列表
     *
     * @param openIds
     * @return List
     */
    List<AccountFans> getFansByOpenIdListByPage(List<AccountFans> openIds);
}
