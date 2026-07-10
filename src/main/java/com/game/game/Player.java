package com.game.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 玩家：可由 WASD/方向键驱动的角色，被僵尸撞击会扣血。
 * <p>移动速度 {@link #SPEED}；位置由 {@link GameController} 归一化并钳制在画布内；
 * 枪口朝 {@code angle}（鼠标方向）。
 * <p>绘制：优先贴 {@code resources/images/player.png} 精灵图（按瞄准方向左右翻转，朝向鼠标侧）；
 * 读图失败则回退画橙色小猫（耳朵+眼睛+枪管），保证不崩。被撞时短暂红闪。
 * 瞄准由画面准星 + 子弹方向表达（不再单独画随鼠标转的枪管）。
 */
public class Player implements GameObject {

    /** 移动速度（像素/帧），控制器与画布共用此单一来源 */
    static final double SPEED = 4.0;

    /** 玩家精灵图（透明背景），类级懒加载；为 null 表示读图失败，draw 回退画橙猫 */
    private static BufferedImage SPRITE;

    private double x;
    private double y;
    private int hp = 100;
    private double angle;
    private final int radius = 26;
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

    /**
     * 懒加载玩家精灵图；读图失败返回 null（draw 会回退画橙猫）。
     *
     * @return 精灵图，或 null
     */
    private static BufferedImage sprite() {
        if (SPRITE == null) {
            try {
                SPRITE = ImageIO.read(Player.class.getResourceAsStream("/images/player.png"));
            } catch (IOException | IllegalArgumentException e) {
                SPRITE = null; // 读不到：保持 null，draw 回退画橙猫
            }
        }
        return SPRITE;
    }

    @Override
    public void draw(Graphics2D g) {
        int ix = (int) x;
        int iy = (int) y;
        BufferedImage sp = sprite();

        if (sp != null) {
            // 贴精灵图：缩放到玩家直径，居中于 (ix, iy)；瞄准左侧时水平镜像
            int sw = sp.getWidth();
            int sh = sp.getHeight();
            boolean faceLeft = Math.cos(angle) < 0;
            if (faceLeft) {
                g.drawImage(sp, ix + radius, iy - radius, ix - radius, iy + radius, 0, 0, sw, sh, null);
            } else {
                g.drawImage(sp, ix - radius, iy - radius, ix + radius, iy + radius, 0, 0, sw, sh, null);
            }
            // 受伤红闪：SRC_ATOP 只把角色本体(不透明像素)染红，不污染透明背景
            if (hitFlash > 0) {
                float a = Math.min(1f, hitFlash / 10f) * 0.5f;
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, a));
                g.setColor(new Color(255, 60, 60));
                g.fillRect(ix - radius, iy - radius, radius * 2, radius * 2);
                g.setComposite(old);
            }
        } else {
            // 回退：代码画橙色小猫（耳朵+身体+眼睛+枪管）——精灵图读不到时用，保证不崩
            g.setColor(Color.ORANGE);
            int[] lex = {ix - 14, ix - 4, ix - 16};
            int[] ley = {iy - 18, iy - 16, iy - 30};
            g.fillPolygon(lex, ley, 3);
            int[] rex = {ix + 14, ix + 4, ix + 16};
            int[] rey = {iy - 18, iy - 16, iy - 30};
            g.fillPolygon(rex, rey, 3);
            g.fillOval(ix - radius, iy - radius, radius * 2, radius * 2);
            g.setColor(Color.WHITE);
            g.fillOval(ix - 9, iy - 6, 7, 9);
            g.fillOval(ix + 2, iy - 6, 7, 9);
            double px = Math.cos(angle) * 2.0;
            double py = Math.sin(angle) * 2.0;
            g.setColor(Color.BLACK);
            g.fillOval((int) (ix - 7 + px), (int) (iy - 3 + py), 3, 4);
            g.fillOval((int) (ix + 4 + px), (int) (iy - 3 + py), 3, 4);
            int barrel = radius + 12;
            g.setColor(Color.DARK_GRAY);
            g.drawLine(ix, iy, (int) (x + Math.cos(angle) * barrel), (int) (y + Math.sin(angle) * barrel));
            if (hitFlash > 0) {
                float a = Math.min(1f, hitFlash / 10f) * 0.5f;
                g.setColor(new Color(255, 60, 60, (int) (a * 255)));
                g.fillOval(ix - radius, iy - radius, radius * 2, radius * 2);
            }
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
