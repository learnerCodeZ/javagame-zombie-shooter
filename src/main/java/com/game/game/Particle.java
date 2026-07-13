package com.game.game;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 粒子：用于命中飞溅、僵尸死亡爆裂、枪口火花等视觉反馈。
 * <p>每帧按速度推进并施加阻尼（模拟空气阻力），寿命按帧倒数衰减，
 * 寿命归零即死亡，由 {@link GameController} 统一清理。
 */
public class Particle {

    private double x;
    private double y;
    private double vx;
    private double vy;
    private final Color color;
    private final int size;
    private int life;
    private final int maxLife;

    /**
     * 构造方法。
     *
     * @param x     初始横坐标
     * @param y     初始纵坐标
     * @param vx    横向速度（像素/帧）
     * @param vy    纵向速度（像素/帧）
     * @param color 粒子颜色
     * @param size  基准半径（像素）
     * @param life  寿命（帧）
     */
    public Particle(double x, double y, double vx, double vy, Color color, int size, int life) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.size = size;
        this.life = life;
        this.maxLife = life;
    }

    /** 每帧推进：位移 + 阻尼 + 寿命递减。 */
    public void update() {
        x += vx;
        y += vy;
        vx *= 0.92;
        vy *= 0.92;
        life--;
    }

    /** 绘制：按剩余寿命比例淡出并缩小。 */
    public void draw(Graphics2D g) {
        float ratio = Math.max(0f, life / (float) maxLife);// ① 剩余寿命比例 1.0→0
        int alpha = (int) (ratio * 255);         // ② 透明度 = 比例 ×255
        if (alpha <= 0) {                        // ③ 完全透明就不画 
            return;
        }
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));// ④ 带 alpha 的颜色
        int s = Math.max(1, (int) (size * ratio)); // ⑤ 大小也按比例缩(最小 1)
        int ix = (int) x;
        int iy = (int) y;
        g.fillOval(ix - s, iy - s, s * 2, s * 2);   // ⑥ 画圆
    }

    /** 寿命是否已尽。 */
    public boolean isDead() {
        return life <= 0;
    }
}
