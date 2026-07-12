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
        String sql = "INSERT INTO game_record(user_id, score, kill_count, survive_sec, difficulty) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, record.getUserId());
            ps.setInt(2, record.getScore());
            ps.setInt(3, record.getKillCount());
            ps.setInt(4, record.getSurviveSec());
            ps.setString(5, record.getDifficulty());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 全局排行榜：指定难度、按分数倒序取前 n 名。
     *
     * @param n          取前 n 条
     * @param difficulty 难度（EASY/HARD，传 {@code Difficulty.name()}）
     * @return 该难度的排行榜列表（已按分数从高到低）
     */
    public List<GameRecord> topN(int n, String difficulty) {
        String sql = "SELECT g.id, g.user_id, g.score, g.kill_count, g.survive_sec, "
                + "g.difficulty, g.record_time, u.nickname "
                + "FROM game_record g JOIN user u ON g.user_id = u.id "
                + "WHERE g.difficulty = ? ORDER BY g.score DESC LIMIT ?";
        List<GameRecord> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, difficulty);
            ps.setInt(2, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapWithUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 某用户的全部记录：不分难度、按时间倒序(最近发生的在最前)，含难度字段(供界面标注)。
     *
     * @param userId 用户ID
     * @return 该用户所有难度的记录列表(最近的排最前)
     */
    public List<GameRecord> mine(int userId) {
        String sql = "SELECT id, user_id, score, kill_count, survive_sec, difficulty, record_time "
                + "FROM game_record WHERE user_id = ? ORDER BY record_time DESC";
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
        r.setDifficulty(rs.getString("difficulty"));
        Timestamp ts = rs.getTimestamp("record_time");
        if (ts != null) {
            r.setRecordTime(new java.util.Date(ts.getTime()));
        }
        return r;
    }

    // topN 专用映射：JOIN user 结果集多出 nickname 列。
    // 注意：与 map 分开，避免 mine 因结果集无 nickname 列而报错。
    private GameRecord mapWithUser(ResultSet rs) throws SQLException {
        GameRecord r = map(rs);
        r.setNickname(rs.getString("nickname"));
        return r;
    }
}
