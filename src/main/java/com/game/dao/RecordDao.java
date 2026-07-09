package com.game.dao;

import com.game.model.GameRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏记录数据访问对象：封装 game_record 表的写入与查询。
 */
public class RecordDao {

    /**
     * 保存一条游戏记录。
     *
     * @param record 游戏记录
     * @return true 表示写入成功
     */
    public boolean saveRecord(GameRecord record) {
        String sql = "INSERT INTO game_record(user_id, score, kill_count, survive_sec) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, record.getUserId());
            ps.setInt(2, record.getScore());
            ps.setInt(3, record.getKillCount());
            ps.setInt(4, record.getSurviveSec());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 全局排行榜：按分数倒序取前 n 名。
     *
     * @param n 取前 n 条
     * @return 排行榜列表（已按分数从高到低）
     */
    public List<GameRecord> topN(int n) {
        String sql = "SELECT id, user_id, score, kill_count, survive_sec, record_time "
                + "FROM game_record ORDER BY score DESC LIMIT ?";
        List<GameRecord> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 某用户自己的记录：按分数倒序。
     *
     * @param userId 用户ID
     * @return 该用户的记录列表
     */
    public List<GameRecord> mine(int userId) {
        String sql = "SELECT id, user_id, score, kill_count, survive_sec, record_time "
                + "FROM game_record WHERE user_id = ? ORDER BY score DESC";
        List<GameRecord> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 把结果集当前行映射成 GameRecord 对象
    private GameRecord map(ResultSet rs) throws SQLException {
        GameRecord r = new GameRecord();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setScore(rs.getInt("score"));
        r.setKillCount(rs.getInt("kill_count"));
        r.setSurviveSec(rs.getInt("survive_sec"));
        Timestamp ts = rs.getTimestamp("record_time");
        if (ts != null) {
            r.setRecordTime(new java.util.Date(ts.getTime()));
        }
        return r;
    }
}
