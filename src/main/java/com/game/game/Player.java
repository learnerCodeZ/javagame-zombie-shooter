package com.game.game;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 玩家：可由 WASD/方向键驱动的橙色小角色（头顶三角耳朵 + 眼睛），枪口朝 angle 方向。
 * <p>移动速度 {@link #SPEED}；位置由 {@link GameController} 归一化并钳制在画布内；
 * 被僵尸撞击会扣血。
 */
public class Player implements GameObject {

    /** 移动速度（像素/帧），控制器与画布共用此单一来源 */
    static final double SPEED = 4.0;

    private double x;
    private double y;
    private int hp = 100;
    private double angle;
    private final int radius = 20;
    /** 受伤泛红剩余帧（被僵尸撞到时短暂红闪） */
    private int hitFlash;

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
        // 位置由 GameController.update() 归一化并钳制后回填；
        // 这里只负责衰减受伤红闪计时
        if (hitFlash > 0) {
            hitFlash--;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        int ix = (int) x;
        int iy = (int) y;
        g.setColor(Color.ORANGE);
        // 头顶两只三角耳朵
        int[] lex = {ix - 14, ix - 4, ix - 16};
        int[] ley = {iy - 18, iy - 16, iy - 30};
        g.fillPolygon(lex, ley, 3);
        int[] rex = {ix + 14, ix + 4, ix + 16};
        int[] rey = {iy - 18, iy - 16, iy - 30};
        g.fillPolygon(rex, rey, 3);
        // 身体：橙色圆
        g.fillOval(ix - radius, iy - radius, radius * 2, radius * 2);
        // 两只眼睛：白底
        g.setColor(Color.WHITE);
        g.fillOval(ix - 9, iy - 6, 7, 9);
        g.fillOval(ix + 2, iy - 6, 7, 9);
        // 黑色瞳孔：朝瞄准方向微偏，显得盯着目标
        double px = Math.cos(angle) * 2.0;
        double py = Math.sin(angle) * 2.0;
        g.setColor(Color.BLACK);
        g.fillOval((int) (ix - 7 + px), (int) (iy - 3 + py), 3, 4);
        g.fillOval((int) (ix + 4 + px), (int) (iy - 3 + py), 3, 4);
        // 枪管：朝 angle 方向的短线
        int barrel = radius + 12;
        g.setColor(Color.DARK_GRAY);
        g.drawLine(ix, iy,
                (int) (x + Math.cos(angle) * barrel),
                (int) (y + Math.sin(angle) * barrel));
        // 受伤红闪：叠加半透明红
        if (hitFlash > 0) {
            float a = Math.min(1f, hitFlash / 10f) * 0.5f;
            g.setColor(new Color(255, 60, 60, (int) (a * 255)));
            g.fillOval(ix - radius, iy - radius, radius * 2, radius * 2);
        }
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
        this.hitFlash = 10;
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

    /**
     * 设置横坐标（由控制器移动逻辑钳制后回填）。
     *
     * @param x 新横坐标
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * 设置纵坐标（由控制器移动逻辑钳制后回填）。
     *
     * @param y 新纵坐标
     */
    public void setY(double y) {
        this.y = y;
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
