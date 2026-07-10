package com.game.dao;

import com.game.model.User;
import com.game.util.MD5Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
        String sql = "SELECT id, username, password, nickname, role, create_time FROM user "
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
                    user.setRole(rs.getString("role"));
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

    /**
     * 修改密码：先校验旧密码（WHERE 里比对 MD5），再更新为新密码。
     * 按影响行数返回，旧密码错误则 0 行受影响返回 false。
     *
     * @param userId 用户ID
     * @param oldPwd 旧明文密码
     * @param newPwd 新明文密码
     * @return true 表示修改成功；旧密码错误或出错则返回 false
     */
    public boolean changePassword(int userId, String oldPwd, String newPwd) {
        String sql = "UPDATE user SET password = ? WHERE id = ? AND password = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, MD5Util.md5(newPwd));
            ps.setInt(2, userId);
            ps.setString(3, MD5Util.md5(oldPwd));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 列出全部用户（用户管理界面用），按 id 升序。
     *
     * @return 全部用户列表；出错返回空列表
     */
    public List<User> listAllUsers() {
        String sql = "SELECT id, username, password, nickname, role, create_time FROM user ORDER BY id";
        List<User> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 删除用户：admin 不可删（账号及其数据都受保护，提前拦截不动其任何记录）。
     * 对普通用户：先删该用户的 game_record 和 password_reset_request
     * （清掉外键依赖），再删 user 本身（DELETE 仍带 role &lt;&gt; 'admin' 兜底）。
     *
     * @param userId 用户ID
     * @return true 表示已删除；目标不存在 / 是 admin / 出错则返回 false
     */
    public boolean deleteUser(int userId) {
        // 三步删除包在同一事务里：任一步失败整体回滚，避免出现
        // “战绩/申请已清空但账号还在”的不一致孤儿状态（参考 ResetRequestDao.approve）
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            // 先查 role：admin 账号连数据都不可动，直接拦截
            String role = null;
            try (PreparedStatement ps0 = conn.prepareStatement(
                    "SELECT role FROM user WHERE id = ?")) {
                ps0.setInt(1, userId);
                try (ResultSet rs = ps0.executeQuery()) {
                    if (rs.next()) {
                        role = rs.getString("role");
                    }
                }
            }
            if (!"user".equals(role)) {
                // 用户不存在 或 是 admin：不删（无写操作，回滚以结束事务）
                conn.rollback();
                return false;
            }
            // 普通用户：先清掉两张子表的外键依赖，避免外键约束报错
            try (PreparedStatement ps1 = conn.prepareStatement(
                    "DELETE FROM game_record WHERE user_id = ?")) {
                ps1.setInt(1, userId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "DELETE FROM password_reset_request WHERE user_id = ?")) {
                ps2.setInt(1, userId);
                ps2.executeUpdate();
            }
            // 再删 user，且 admin 不可删（兜底）
            boolean deleted;
            try (PreparedStatement ps3 = conn.prepareStatement(
                    "DELETE FROM user WHERE id = ? AND role <> 'admin'")) {
                ps3.setInt(1, userId);
                deleted = ps3.executeUpdate() > 0;
            }
            // 第三步若实际未删（并发下 role 被改成 admin 等，0 行受影响）则回滚前两步，
            // 不留“子表已清空但账号还在”的孤儿；只有真正删掉才提交
            if (deleted) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return deleted;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    // 把结果集当前行映射成 User 对象（含 role / create_time）
    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setNickname(rs.getString("nickname"));
        user.setRole(rs.getString("role"));
        Timestamp ts = rs.getTimestamp("create_time");
        if (ts != null) {
            user.setCreateTime(new java.util.Date(ts.getTime()));
        }
        return user;
    }
}
