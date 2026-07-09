package com.game.game;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 僵尸：从画布边缘生成，朝目标（玩家）匀速直线移动。
 * <p>绿色圆形，血量 1，一发子弹即可击杀。
 */
public class Zombie implements GameObject {

    private double x;
    private double y;
    private final double speed;
    private int hp = 1;
    private final int radius = 18;
    private final double targetX;
    private final double targetY;

    /**
     * 构造方法。
     *
     * @param x       生成横坐标
     * @param y       生成纵坐标
     * @param speed   移动速度（像素/帧）
     * @param targetX 目标横坐标（玩家位置）
     * @param targetY 目标纵坐标（玩家位置）
     */
    public Zombie(double x, double y, double speed, double targetX, double targetY) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    @Override
    public void update() {
        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.hypot(dx, dy);
        if (dist > 1e-4) {
            x += dx / dist * speed;
            y += dy / dist * speed;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.GREEN);
        g.fillOval((int) x - radius, (int) y - radius, radius * 2, radius * 2);
    }

    /**
     * 承受伤害。
     *
     * @param dmg 伤害点数（正数）
     */
    public void takeDamage(int dmg) {
        hp -= dmg;
    }

    /**
     * 立即标记为死亡（如撞到玩家后消失），不计为击杀得分。
     */
    public void kill() {
        hp = 0;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getRadius() {
        return radius;
    }
}
