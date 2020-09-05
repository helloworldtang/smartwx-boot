package com.wxmp.wxcms.mapper;

import com.wxmp.wxcms.domain.UserTag;

import java.util.List;

/**
 * @author fuzi Kong
 * @version 2.0
 * @date 2018-04-17 10:54:58
 */
public interface UserTagDao {

    UserTag getById(Integer id);

    // Jackie TODO : 2019/8/7 user_tag需要增加appId字段
    List<UserTag> getUserTagListByPage(UserTag searchEntity);

    void add(UserTag entity);

    void addList(List<UserTag> list);

    void update(UserTag entity);

    void delete(UserTag entity);

    /**
     * <p>
     * 删除（根据ID 批量删除）
     * </p>
     *
     * @param ids 主键ID列表
     * @return int
     */
    Integer deleteBatchIds(String[] ids);

    /**
     * 获取最大的ID和本地库存的比较决定是否同步
     *
     * @return
     */
    Integer getMaxId();
}
