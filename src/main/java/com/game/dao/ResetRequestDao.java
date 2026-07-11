package com.game.dao;

import com.game.model.PasswordResetRequest;
import com.game.util.MD5Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 密码重置申请数据访问对象：封装 password_reset_request 表的读写。
 * 提供申请提交、查重、列出待审核、通过（重置为 123456）、拒绝。
 * 全部使用 PreparedStatement 防止 SQL 注入。
 *
 * admin 账号不可被重置：approve 时若目标 role 不是 'user'，影响行数为 0，
 * 申请会被置为 rejected。
 */
public class ResetRequestDao {

    /**
     * 提交重置申请：查 userId；手机号不存在返回 false；
     * 已有 pending 申请则不重复插入返回 false；否则插入一条 pending 记录返回 true。
     *
     * @param phone 申请人手机号
     * @return true 表示已成功提交；手机号不存在 / 已有待审 / 出错则返回 false
     */
    public boolean requestReset(String phone) {
        try (Connection conn = DBUtil.getConnection()) {
            // 1) 查 userId
            Integer userId = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM user WHERE phone = ?")) {
                ps.setString(1, phone);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getInt("id");
                    }
                }
            }
            if (userId == null) {
                return false;
            }
            // 2) 已有 pending 申请则不重复
            if (hasPending(userId)) {
                return false;
            }
            // 3) 插入 pending 申请（status / request_time 显式写，避免依赖默认值）
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO password_reset_request(user_id, status, request_time) "
                    + "VALUES (?, 'pending', NOW())")) {
                ps.setInt(1, userId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 该用户是否已有待审核（pending）申请。
     *
     * @param userId 用户ID
     * @return true 表示存在 pending 申请；出错返回 false
     */
    public boolean hasPending(int userId) {
        String sql = "SELECT COUNT(*) FROM password_reset_request "
                + "WHERE user_id = ? AND status = 'pending'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
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
     * 列出全部待审核申请，JOIN user 带上 phone / nickname，按申请时间升序。
     *
     * @return 待审核申请列表；出错返回空列表
     */
    public List<PasswordResetRequest> listPending() {
        String sql = "SELECT r.id, r.user_id, r.status, r.request_time, r.handle_time, "
                + "u.phone, u.nickname "
                + "FROM password_reset_request r JOIN user u ON r.user_id = u.id "
                + "WHERE r.status = 'pending' ORDER BY r.request_time";
        List<PasswordResetRequest> list = new ArrayList<>();
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
     * 通过申请：把目标用户密码重置为 MD5('123456')（仅 role='user' 可重置）。
     * 影响行数 &gt; 0 则置申请为 approved 返回 true；否则（admin 等）置 rejected 返回 false。
     * 两步写操作用同一连接的事务保证一致性。
     *
     * @param requestId 申请ID
     * @param userId    目标用户ID
     * @return true 表示已通过并重置；目标不可重置 / 出错则返回 false（申请被置 rejected）
     */
    public boolean approve(int requestId, int userId) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            // 1) 重置密码（仅普通用户）
            boolean reset;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE user SET password = ? WHERE id = ? AND role = 'user'")) {
                ps.setString(1, MD5Util.md5("123456"));
                ps.setInt(2, userId);
                reset = ps.executeUpdate() > 0;
            }
            // 2) 按结果置申请状态（approved / rejected），并写处理时间
            String newStatus = reset ? "approved" : "rejected";
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE password_reset_request SET status = ?, handle_time = NOW() WHERE id = ?")) {
                ps.setString(1, newStatus);
                ps.setInt(2, requestId);
                ps.executeUpdate();
            }
            conn.commit();
            return reset;
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

    /**
     * 拒绝申请：置为 rejected 并写处理时间。
     *
     * @param requestId 申请ID
     * @return true 表示已拒绝；申请不存在 / 出错则返回 false
     */
    public boolean reject(int requestId) {
        String sql = "UPDATE password_reset_request SET status = 'rejected', handle_time = NOW() WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 把结果集当前行映射成 PasswordResetRequest（含 JOIN 来的 phone / nickname）
    private PasswordResetRequest map(ResultSet rs) throws SQLException {
        PasswordResetRequest r = new PasswordResetRequest();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setStatus(rs.getString("status"));
        Timestamp req = rs.getTimestamp("request_time");
        if (req != null) {
            r.setRequestTime(new Date(req.getTime()));
        }
        Timestamp handle = rs.getTimestamp("handle_time");
        if (handle != null) {
            r.setHandleTime(new Date(handle.getTime()));
        }
        r.setPhone(rs.getString("phone"));
        r.setNickname(rs.getString("nickname"));
        return r;
    }
}
