package com.game.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * 浮动文字：击杀僵尸时在其位置向上飘升并淡出的得分提示（如 "+10"）。
 * <p>每帧上移固定像素、寿命按帧倒数衰减，寿命归零由控制器清理。
 */
public class FloatingText {

    private double x;
    private double y;
    private final String text;
    private final Color color;
    private int life;
    private final int maxLife;

    /**
     * 构造方法。
     *
     * @param x     起始横坐标
     * @param y     起始纵坐标（基线）
     * @param text  显示文本
     * @param color 文字颜色
     * @param life  寿命（帧）
     */
    public FloatingText(double x, double y, String text, Color color, int life) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.life = life;
        this.maxLife = life;
    }

    /** 每帧上移并递减寿命。 */
    public void update() {
        y -= 0.8;// 每帧上移 0.8 像素(固定速度,无阻尼)
        life--; // 寿命 -1
    }

    /** 绘制：按剩余寿命比例淡出。 */
    public void draw(Graphics2D g) {
        float ratio = Math.max(0f, life / (float) maxLife);// 剩余比例 1.0→0
        int alpha = (int) (ratio * 255); // 透明度 = 比例×255
        if (alpha <= 0) {
            return;// 完全透明就不画
        }
        g.setFont(new Font("微软雅黑", Font.BOLD, 16));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));//Color
        g.drawString(text, (int) x, (int) y); // 画文字
    }

    /** 寿命是否已尽。 */
    public boolean isDead() {
        return life <= 0;
    }
}
