package com.game.model;

import java.util.Date;

/**
 * 用户实体（对应数据库 user 表）。
 * 纯 POJO：私有字段 + getter/setter。
 */
public class User {

    private int id;
    /** 登录手机号（11 位），全局唯一 */
    private String phone;
    private String password;
    private String nickname;
    /** 角色：admin（管理员）/ user（普通用户），默认 user */
    private String role;
    private Date createTime;

    public User() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取角色。
     *
     * @return 角色：admin / user；未设置时可能为 null
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置角色。
     *
     * @param role 角色：admin / user
     */
    public void setRole(String role) {
        this.role = role;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", phone='" + phone + "', nickname='" + nickname
                + "', role=" + role + "}";
    }
}
