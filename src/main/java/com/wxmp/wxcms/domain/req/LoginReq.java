/**
 * @Auther: cheng.tang
 * @Date: 2019/8/5
 * @Description:
 */
package com.wxmp.wxcms.domain.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * @Auther: cheng.tang
 * @Date: 2019/8/5
 * @Description:
 */
@ApiModel("登录参数")
@Data
public class LoginReq {

    @ApiModelProperty("用户名")
    @NotEmpty(message = "用户名不能为空")
    @Size(min = 1, max = 100, message = "无效的用户名")
    private String account;

    @ApiModelProperty("密码")
    @NotEmpty(message = "密码不能为空")
    @Size(min = 5, max = 30, message = "无效的密码")
    private String pwd;

}
