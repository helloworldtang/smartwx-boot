/**
 * @Auther: cheng.tang
 * @Date: 2019/8/5
 * @Description:
 */
package com.wxmp.wxcms.domain.bo;

import lombok.Data;

/**
 * @Auther: cheng.tang
 * @Date: 2019/8/5
 * @Description:
 */
@Data
public class LoginBO {

    //主键id
    private String id;

    //用户名
    private String account;

    //密码
    private String pwd;

    //姓名
    private String trueName;

}
