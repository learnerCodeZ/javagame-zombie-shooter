package com.game.model;

import java.util.Date;

/**
 * 游戏记录实体（对应数据库 game_record 表）。
 * 纯 POJO：私有字段 + getter/setter。
 */
public class GameRecord {

    private int id;
    private int userId;
    private int score;
    private int killCount;
    private int surviveSec;
    /** 难度（EASY/HARD，对应 {@link com.game.game.Difficulty#name()}）；默认 EASY */
    private String difficulty = "EASY";
    private Date recordTime;

    /**
     * 昵称（仅用于显示）。
     * 注意：nickname 不是数据库 game_record 表的列，
     * 只用于排行榜显示，由 RecordDao.topN 的 JOIN user 填充。
     */
    private String nickname;

    public GameRecord() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getKillCount() {
        return killCount;
    }

    public void setKillCount(int killCount) {
        this.killCount = killCount;
    }

    public int getSurviveSec() {
        return surviveSec;
    }

    public void setSurviveSec(int surviveSec) {
        this.surviveSec = surviveSec;
    }

    /**
     * 获取难度（EASY/HARD）。
     *
     * @return 难度字符串，默认 "EASY"
     */
    public String getDifficulty() {
        return difficulty;
    }

    /**
     * 设置难度（存档时由 GameWindow 传入 {@code Difficulty.name()}）。
     *
     * @param difficulty 难度字符串
     */
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Date getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(Date recordTime) {
        this.recordTime = recordTime;
    }

    /**
     * 获取昵称（仅显示用，非数据库列）。
     *
     * @return 昵称，可能为 null（如 mine 结果未填充）
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称（仅显示用，由 JOIN user 填充）。
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return "GameRecord{userId=" + userId + ", score=" + score
                + ", kill=" + killCount + ", survive=" + surviveSec + "s"
                + ", difficulty=" + difficulty + "}";
    }
}
