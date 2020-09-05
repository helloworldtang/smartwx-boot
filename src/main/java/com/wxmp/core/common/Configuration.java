/*
 * FileName：Configuration.java
 * <p>
 * Copyright (c) 2017-2020, <a href="http://www.webcsn.com">hermit (794890569@qq.com)</a>.
 * <p>
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/gpl-3.0.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.wxmp.core.common;

import com.wxmp.core.util.FileUtil;
import com.wxmp.core.util.StringUtil;
import com.wxmp.core.util.ValidateUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author : hermit
 */
public class Configuration {
    public static XMLConfiguration Config = null;
    public static String ROOT;
    public static String CURRENT_PATH;
    public static String TEMPLATE_PATH;
    public static String TEMPLATE_WEB_PATH;
    public static String TEMPORARY_PATH;
    public static String LOG_PATH;
    public static String CLASSPATH;
    public static String CONFIG_PATH;

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");

    static {
        ROOT = new File(Configuration.class.getResource("/").getPath()).getParentFile().getParent();
        CURRENT_PATH = new File(Configuration.class.getResource("/").getPath()).toString();
        TEMPLATE_PATH = StringUtil.concat(ROOT, File.separator, "WEB-INF", File.separator, "template");
        TEMPLATE_WEB_PATH = StringUtil.concat(TEMPLATE_PATH, File.separator, "views");
        TEMPORARY_PATH = StringUtil.concat(ROOT, File.separator, "WEB-INF", File.separator, "tmp");
        CONFIG_PATH = StringUtil.concat(ROOT, File.separator, "WEB-INF", File.separator, "conf");
        LOG_PATH = StringUtil.concat(ROOT, File.separator, "WEB-INF", File.separator, "logs");
        StringBuilder sb = new StringBuilder();
        List<File> files = FileUtil.getFiles(new File(StringUtil.concat(ROOT, File.separator, "WEB-INF", File.separator, "lib")));
        sb.append(".");
        for (File file : files) {
            sb.append(System.getProperty("path.separator"));
            sb.append(file.getAbsolutePath());
        }
        sb.append(System.getProperty("path.separator"));
        sb.append(CURRENT_PATH);
        CLASSPATH = sb.toString();
        try {
            Config = new XMLConfiguration();
            Config.setFile(new File(StringUtil.concat(CURRENT_PATH, File.separator, "system.cfg.xml")));
            Config.setReloadingStrategy(new FileChangedReloadingStrategy());
            Config.load();
            Config.setEncoding("UTF-8");
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return Config.getString(key);
    }

    public static String getEncoding() {
        return "utf-8";
    }

    public static String getContentType() {
        return "text/html;charset=UTF-8";
    }


}
