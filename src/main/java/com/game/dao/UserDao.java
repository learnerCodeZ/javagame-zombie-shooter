package com.game.dao;

import com.game.model.User;
import com.game.util.MD5Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * 用户数据访问对象：封装 user 表的增删改查。
 * 全部使用 PreparedStatement 防止 SQL 注入。
 */
public class UserDao {

    /**
     * 查询用户名是否已存在（注册时查重用）。
     *
     * @param username 用户名
     * @return true 表示已存在
     */
    public boolean findByName(String username) {
        String sql = "SELECT COUNT(*) FROM user WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 注册：把用户写入 user 表。密码会先做 MD5。
     *
     * @param user 待注册用户（明文密码）
     * @return true 表示注册成功；用户名已存在或出错则返回 false
     */
    public boolean register(User user) {
        if (findByName(user.getUsername())) {
            System.out.println("[注册] 用户名已存在：" + user.getUsername());
            return false;
        }
        String sql = "INSERT INTO user(username, password, nickname) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, MD5Util.md5(user.getPassword()));
            ps.setString(3, user.getNickname());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 登录：按用户名 + 密码(先 MD5) 校验。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 校验通过返回 User 对象；失败返回 null
     */
    public User login(String username, String password) {
        String sql = "SELECT id, username, password, nickname, create_time FROM user "
                + "WHERE username = ? AND password = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, MD5Util.md5(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setNickname(rs.getString("nickname"));
                    Timestamp ts = rs.getTimestamp("create_time");
                    if (ts != null) {
                        user.setCreateTime(new java.util.Date(ts.getTime()));
                    }
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
