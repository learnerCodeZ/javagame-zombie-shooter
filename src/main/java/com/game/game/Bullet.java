package com.game.game;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 子弹：沿固定方向匀速直线飞行，出界或命中后移除。
 * <p>黄色小圆。
 */
public class Bullet implements GameObject {

    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private final int radius = 5;
    private boolean dead;

    /**
     * 构造方法。
     *
     * @param x  初始横坐标（枪口）
     * @param y  初始纵坐标（枪口）
     * @param vx 横向速度（像素/帧）
     * @param vy 纵向速度（像素/帧）
     */
    public Bullet(double x, double y, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.dead = false;
    }

    @Override
    public void update() {
        x += vx;
        y += vy;
    }

    @Override
    public void draw(Graphics2D g) {
        int ix = (int) x;
        int iy = (int) y;
        // 外层光晕：半透明黄
        g.setColor(new Color(255, 220, 80, 70));
        g.fillOval(ix - radius - 3, iy - radius - 3, (radius + 3) * 2, (radius + 3) * 2);
        // 拖尾：沿速度反方向的渐淡短线
        g.setColor(new Color(255, 200, 40, 180));
        g.drawLine(ix, iy, (int) (x - vx * 1.6), (int) (y - vy * 1.6));
        // 弹芯：亮黄
        g.setColor(Color.YELLOW);
        g.fillOval(ix - radius, iy - radius, radius * 2, radius * 2);
        // 高光点：白色
        g.setColor(Color.WHITE);
        g.fillOval(ix - 2, iy - 2, 4, 4);
    }

    /**
     * 是否飞出画布范围。
     *
     * @param w 画布宽度
     * @param h 画布高度
     * @return 出界返回 true
     */
    public boolean isOffscreen(int w, int h) {//飞出画布
        return x < -radius || x > w + radius || y < -radius || y > h + radius;
    }

    public boolean isDead() {//命中标记
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
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
