package com.wxmp.wxcms.mapper;

import com.wxmp.wxcms.domain.Account;

import java.util.List;

/**
 * @author hermit
 * @version 2.0
 * @date 2018-04-17 10:54:58
 */

public interface AccountDao {

    Account getById(Long id);

    Account getByAccount(String account);

    Account getSingleAccount();

    List<Account> listForPage();

    void add(Account entity);

    void update(Account entity);

    void delete(Account entity);


}
