package com.game.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 僵尸：从画布边缘生成，匀速追击目标（玩家）；目标可由控制器每帧更新，实现持续追踪。
 * <p>两种变体：
 * <ul>
 *   <li>{@link Type#NORMAL}：普通绿僵，半径 18、血量 1、得分 10。</li>
 *   <li>{@link Type#BRUTE}：壮硕僵尸，半径 26、血量 3、速度更慢、得分 25，头顶带血条。</li>
 * </ul>
 * 绘制：优先贴 {@code resources/images/zombie.png} 精灵图（透明背景），缩放到僵尸直径；
 * 读图失败则回退画绿圆，保证不崩。身体随帧轻微起伏摇摆；被击中但未死时短暂闪白。
 */
public class Zombie implements GameObject {

    /** 僵尸变体。 */
    public enum Type { NORMAL, BRUTE }

    /** 僵尸精灵图（透明背景），类级懒加载；为 null 表示读图失败，draw 会回退画绿圆。 */
    private static BufferedImage SPRITE;

    private final Type type;
    private double x;
    private double y;
    private final double speed;
    private int hp;
    private final int maxHp;
    private final int radius;
    private final int scoreValue;
    /** 追踪目标横坐标（随玩家移动每帧更新） */
    private double targetX;
    /** 追踪目标纵坐标（随玩家移动每帧更新） */
    private double targetY;
    /** 命中闪白剩余帧（被打到但未死时短暂泛白） */
    private int hitFlash;
    /** 动画帧计数（用于身体起伏摇摆） */
    private int animFrame;
    /** 当前是否朝左（据移动方向更新；贴图时据此做水平镜像，向哪走朝哪） */
    private boolean facingLeft = false;

    /**
     * 构造普通僵尸（兼容旧调用）。
     *
     * @param x       生成横坐标
     * @param y       生成纵坐标
     * @param speed   移动速度（像素/帧）
     * @param targetX 初始目标横坐标（玩家位置）
     * @param targetY 初始目标纵坐标（玩家位置）
     */
    public Zombie(double x, double y, double speed, double targetX, double targetY) {
        this(Type.NORMAL, x, y, speed, targetX, targetY);//调用本类的另一个构造方法，把自己变成"调用另一个构造方法并把参数传过去"的简写
    } 

    /**
     * 构造指定变体的僵尸。
     *
     * @param type    变体
     * @param x       生成横坐标
     * @param y       生成纵坐标
     * @param speed   移动速度（像素/帧）
     * @param targetX 初始目标横坐标（玩家位置）
     * @param targetY 初始目标纵坐标（玩家位置）
     */
    public Zombie(Type type, double x, double y, double speed, double targetX, double targetY) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.speed = speed;
        if (type == Type.BRUTE) {
            this.radius = 26;
            this.maxHp = 3;
            this.hp = 3;
            this.scoreValue = 25;
        } else {
            this.radius = 18;
            this.maxHp = 1;
            this.hp = 1;
            this.scoreValue = 10;
        }
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
        // 据水平移动方向更新朝向：往左朝左、往右朝右；基本垂直移动时保持上次朝向避免抖动
        if (dx < -1e-4) {
            facingLeft = true;
        } else if (dx > 1e-4) {
            facingLeft = false;
        }
        if (hitFlash > 0) {
            hitFlash--;
        }
        animFrame++;
    }

    /**
     * 懒加载僵尸精灵图；读图失败返回 null（draw 会回退画绿圆）。
     *
     * @return 精灵图，或 null
     */
    private static BufferedImage sprite() {
        if (SPRITE == null) {
            try {
                SPRITE = ImageIO.read(Zombie.class.getResourceAsStream("/images/zombie.png"));
            } catch (IOException | IllegalArgumentException e) {
                SPRITE = null; // 读不到：保持 null，draw 回退画绿圆
            }
        }
        return SPRITE;
    }

    @Override
    public void draw(Graphics2D g) {
        int ix = (int) x;
        // 身体起伏：以正弦做轻微上下摆动，显得在蹒跚前行
        int bob = (int) Math.round(Math.sin(animFrame * 0.25) * 2.0);
        int iy = (int) y + bob;
        boolean brute = type == Type.BRUTE;
        BufferedImage sp = sprite();

        if (sp != null) {
            // 贴精灵图：缩放到僵尸直径，居中于 (ix, iy)；朝左时目标 x 反向 = 水平镜像
            int sw = sp.getWidth();
            int sh = sp.getHeight();
            if (facingLeft) {
                g.drawImage(sp, ix + radius, iy - radius, ix - radius, iy + radius, 0, 0, sw, sh, null);
            } else {
                g.drawImage(sp, ix - radius, iy - radius, ix + radius, iy + radius, 0, 0, sw, sh, null);
            }
            // 命中闪白：用 SRC_ATOP 只把僵尸本体(不透明像素)染白，不污染透明背景区
            if (hitFlash > 0) {
                float a = Math.min(1f, hitFlash / 6f) * 0.6f;
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, a));
                g.setColor(Color.WHITE);
                g.fillRect(ix - radius, iy - radius, radius * 2, radius * 2);
                g.setComposite(old);
            }
        } else {
            // 回退：代码画绿圆 + 描边 + 红眼 + 嘴（精灵图读不到时用，保证不崩）
            g.setColor(brute ? new Color(96, 124, 46) : Color.GREEN);
            g.fillOval(ix - radius, iy - radius, radius * 2, radius * 2);
            g.setColor(brute ? new Color(54, 74, 24) : new Color(0, 110, 0));
            g.drawOval(ix - radius, iy - radius, radius * 2, radius * 2);
            int eye = brute ? 7 : 5;
            int eo = brute ? 10 : 8;
            g.setColor(Color.RED);
            g.fillOval(ix - eo - eye / 2, iy - 5, eye, eye);
            g.fillOval(ix + eo - eye / 2, iy - 5, eye, eye);
            int mw = brute ? 16 : 10;
            g.setColor(Color.BLACK);
            g.fillRect(ix - mw / 2, iy + 4, mw, 2);
            if (hitFlash > 0) {
                float a = Math.min(1f, hitFlash / 6f) * 0.6f;
                g.setColor(new Color(255, 255, 255, (int) (a * 255)));
                g.fillOval(ix - radius, iy - radius, radius * 2, radius * 2);
            }
        }

        // 壮硕头顶血条（仅多血量且已掉血时显示）
        if (maxHp > 1 && hp < maxHp) {
            int barW = radius * 2;
            int barX = ix - radius;
            int barY = iy - radius - 10;
            g.setColor(new Color(50, 50, 50));
            g.fillRect(barX, barY, barW, 4);
            g.setColor(new Color(220, 70, 70));
            g.fillRect(barX, barY, (int) (barW * hp / (double) maxHp), 4);
        }
    }

    /**
     * 更新追踪目标（由控制器每帧回填玩家当前位置）。
     *
     * @param x 目标横坐标
     * @param y 目标纵坐标
     */
    public void setTarget(double x, double y) {
        this.targetX = x;
        this.targetY = y;
    }

    /**
     * 承受伤害，并触发命中闪白。
     *
     * @param dmg 伤害点数（正数）
     */
    public void takeDamage(int dmg) {
        hp -= dmg;
        hitFlash = 6;
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

    /** 变体。 */
    public Type getType() {
        return type;
    }

    /** 击杀得分。 */
    public int getScoreValue() {
        return scoreValue;
    }

    /** 死亡爆裂用的血液颜色（按变体区分深浅）。 */
    public Color getBloodColor() {
        return type == Type.BRUTE ? new Color(70, 96, 30) : new Color(40, 170, 40);
    }
}
