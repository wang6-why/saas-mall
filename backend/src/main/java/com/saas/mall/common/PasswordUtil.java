package com.saas.mall.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具：企业 SaaS 绝不能明文存密码。
 * 兼容存量明文数据：校验时若库中不是 BCrypt 格式则明文比对，登录成功后自动升级为密文。
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public static String encode(String raw) {
        return ENCODER.encode(raw);
    }

    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) return false;
        if (isBcrypt(stored)) {
            return ENCODER.matches(raw, stored);
        }
        return raw.equals(stored); // 存量明文，交由调用方决定是否升级
    }

    public static boolean isBcrypt(String stored) {
        return stored != null && stored.startsWith("$2");
    }
}
