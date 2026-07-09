package com.game.game;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 玩家：固定在画布中央，不可移动。
 * <p>橙色圆形 + 朝向 angle 的枪管线；被僵尸撞击会扣血。
 */
public class Player implements GameObject {

    private double x;
    private double y;
    private int hp = 100;
    private double angle;
    private final int radius = 20;

    /**
     * 构造方法。
     *
     * @param x 初始横坐标
     * @param y 初始纵坐标
     */
    public Player(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        // 玩家固定不动
    }

    @Override
    public void draw(Graphics2D g) {
        int ix = (int) x;
        int iy = (int) y;
        // 身体：橙色圆
        g.setColor(Color.ORANGE);
        g.fillOval(ix - radius, iy - radius, radius * 2, radius * 2);
        // 枪管：朝 angle 方向的短线
        int barrel = radius + 12;
        g.setColor(Color.DARK_GRAY);
        g.drawLine(ix, iy,
                (int) (x + Math.cos(angle) * barrel),
                (int) (y + Math.sin(angle) * barrel));
    }

    /**
     * 承受伤害。
     *
     * @param dmg 伤害点数（正数）
     */
    public void takeDamage(int dmg) {
        this.hp -= dmg;
        if (this.hp < 0) {
            this.hp = 0;
        }
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public int getRadius() {
        return radius;
    }
}
