package com.game.model;

import java.util.Date;

/**
 * 密码重置申请实体（对应数据库 password_reset_request 表）。
 * 纯 POJO：私有字段 + getter/setter。
 *
 * 注意：phone / nickname 两个仅显示用字段，由 ResetRequestDao.listPending()
 * 通过 JOIN user 表填充，**不是 password_reset_request 表的真实列**。
 */
public class PasswordResetRequest {

    private int id;
    private int userId;
    /** 状态：pending / approved / rejected */
    private String status;
    private Date requestTime;
    private Date handleTime;

    /**
     * 申请人手机号（仅显示用，非表列，由 JOIN user 填充）。
     */
    private String phone;
    /**
     * 申请人昵称（仅显示用，非表列，由 JOIN user 填充）。
     */
    private String nickname;

    public PasswordResetRequest() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
    }

    /**
     * 获取申请人手机号（仅显示用，非数据库列）。
     *
     * @return 手机号，未 JOIN 时可能为 null
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置申请人手机号（仅显示用，由 JOIN user 填充）。
     *
     * @param phone 手机号
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 获取申请人昵称（仅显示用，非数据库列）。
     *
     * @return 昵称，未 JOIN 时可能为 null
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置申请人昵称（仅显示用，由 JOIN user 填充）。
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return "PasswordResetRequest{id=" + id + ", userId=" + userId
                + ", phone='" + phone + "', nickname='" + nickname
                + "', status='" + status + "', requestTime=" + requestTime + "}";
    }
}
