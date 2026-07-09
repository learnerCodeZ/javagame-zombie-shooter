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
    private Date recordTime;

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

    public Date getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(Date recordTime) {
        this.recordTime = recordTime;
    }

    @Override
    public String toString() {
        return "GameRecord{userId=" + userId + ", score=" + score
                + ", kill=" + killCount + ", survive=" + surviveSec + "s}";
    }
}
