package com.wxmp.core.util;

import com.wxmp.wxcms.domain.bo.LoginBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.HashMap;

@Slf4j
@Component
public final class SessionUtil {

    public static HttpSession session;

    private static HashMap<String, HttpSession> sessions;
    private static HashMap<String, String> userSessionIds;
    public static final String SESSION_USER = "session_user";

    static {
        sessions = new HashMap<>();
        userSessionIds = new HashMap<>();
    }

    /**
     * 设置session的值
     *
     * @param key
     * @param value
     */
    public static void setAttr(String key, Object value) {
        session.setAttribute(key, value);
    }

    /**
     * 获取session的值
     *
     * @param key
     */
    public static Object getAttr(String key) {
        return session.getAttribute(key);
    }

    /**
     * 删除Session值
     *
     * @param key
     */
    public static void removeAttr(String key) {
        session.removeAttribute(key);
    }

    /**
     * 设置用户信息 到session
     *
     * @param loginBO
     */
    public synchronized static void setUser(LoginBO loginBO) {
        // 以有次用户，踢出此用户
        kickOutUserByUserAcc(loginBO.getAccount());
        session.setAttribute(SESSION_USER, loginBO);
        log.debug("[session管理]用户有效，开始将session缓存起来，sessionid为：" + session.getId() + ",用户登陆账号：" + loginBO.getAccount());
        userSessionIds.put(loginBO.getAccount(), session.getId());
        addSession(session);
    }


    /**
     * 从session中获取用户信息
     *
     * @return SysUser
     */
    public static LoginBO getUser() {
        return (LoginBO) session.getAttribute(SESSION_USER);
    }

    /**
     * 从session中获取用户信息
     *
     * @return SysUser
     */
    public static String getUserId() {
        LoginBO user = getUser();
        if (user != null) {
            return user.getId();
        }
        return null;
    }

    // 根据用户账户踢出用户
    public synchronized static void kickOutUserByUserAcc(String account) {
        if (userSessionIds.get(account) != null) {
            // 找到此用户
            HttpSession session = sessions.get(userSessionIds.get(account));
            if (session != null) {
                log.debug("[session管理]踢出用户，sessionid为：" + session.getId() + ",用户登陆账号：" + account);
                removeSession(session);
                log.debug("[session管理]踢出用户，将session变为无效，sessionid为：" + session.getId());
                session.invalidate();
                sessions.remove(session.getId());
                // 发送下线通知.
                System.out.println("用户在线，踢出用户：" + session.getId());
                log.debug("[session管理]踢出用户，开始发送下线通知，sessionid为：" + session.getId() + ",用户登陆账号：" + account);
            }
            userSessionIds.remove(account);
        }
    }

    /**
     * 将session加入到缓存中.
     *
     * @param session
     */
    public synchronized static void addSession(HttpSession session) {
        log.debug("[session管理]设置用户信息，将此session保存至内存，sessionid为：" + session.getId());
        sessions.put(session.getId(), session);
    }

    private synchronized static void removeSession(HttpSession session) {
        log.debug("[session管理]将session从内存中移除，sessionid为：" + session.getId());
        sessions.remove(session.getId());
    }

    @Autowired
    public void setSession(HttpSession session) {
        this.session = session;
    }
}
