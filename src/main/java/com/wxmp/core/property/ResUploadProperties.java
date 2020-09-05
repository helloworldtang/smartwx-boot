/**
 * @Auther: cheng.tang
 * @Date: 2019/8/2
 * @Description:
 */
package com.wxmp.core.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 上传到nginx好的路径
 *
 * @Auther: cheng.tang
 * @Date: 2019/8/2
 * @Description:
 */
@ConfigurationProperties(prefix = "res.upload")
@Component
@Data
public class ResUploadProperties {
    /**
     *
     */
    private String url;

    /**
     *
     */
    private String path;

}
