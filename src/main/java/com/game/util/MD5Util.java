package com.game.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 工具类：把明文密码加密成 32 位十六进制字符串。
 *
 * 重要：加密结果必须与数据库 sql/schema.sql 里 MD5('123456') 一致，
 *       即 "123456" -> e10adc3949ba59abbe56e057f20f883e，
 *       否则注册的用户和测试数据对不上、登录会失败。
 */
public class MD5Util {

    private MD5Util() {
        // 工具类，不实例化
    }

    /**
     * 对明文做 MD5 加密，返回 32 位小写十六进制串。
     *
     * @param plain 明文
     * @return 32 位十六进制 MD5 值；入参为 null 则返回 null
     */
    public static String md5(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                // 每个字节转成 2 位十六进制
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 是 JDK 内置算法，正常不会缺失
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }
}
