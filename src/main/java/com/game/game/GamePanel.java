package com.game.game;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * 游戏画布：持有控制器与刷新定时器，负责绘制并把输入转发给控制器。
 * <p>画布 800x600，约 60 FPS（Timer 间隔 16ms）。右上角有"退出本局"按钮。
 */
public class GamePanel extends JPanel {

    private final GameController controller;
    private final Timer timer;

    /** 右上角"退出本局"按钮：宽 / 高 / 距顶（横坐标按画布实际宽度动态贴右） */
    private static final int EXIT_W = 84;
    private static final int EXIT_H = 28;
    private static final int EXIT_Y = 10;
    /** 点"退出本局"后的回调（由 GameWindow 注入：停循环 + 回主菜单） */
    private Runnable onExit;

    /** 当前处于按下状态的移动键码集合（WASD + 方向键），失焦时清空防卡键 */
    private final Set<Integer> pressedKeys = new HashSet<>();

    /** 最近一次鼠标位置（默认画布中心），用于绘制准星 */
    private int mouseX = 400;
    private int mouseY = 300;

    /**
     * 构造方法。
     *
     * @param c 游戏控制器
     */
    public GamePanel(GameController c) {
        this.controller = c;
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);
        // 鼠标移动/拖拽 → 更新瞄准角度并记录准星位置
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                controller.setAim(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                controller.setAim(e.getX(), e.getY());
            }
        });
        // 鼠标按下 → 先判"退出本局"按钮，否则开火
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isInExitButton(e.getX(), e.getY())) {
                    if (onExit != null) {
                        onExit.run();
                    }
                    return;
                }
                controller.shoot();
            }
        });
        // 键盘 WASD / 方向键 → 记录按下状态，每帧由 tick() 合成移动方向
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                pressedKeys.add(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                pressedKeys.remove(e.getKeyCode());
            }
        });
        // 失焦清空按键，避免"按住后切窗导致角色一直走"
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                pressedKeys.clear();
            }
        });
        this.timer = new Timer(16, e -> tick());
    }

    /**
     * 单帧回调：推进逻辑、重绘、必要时停止定时器。
     */
    private void tick() {
        controller.setMoveDir(moveDirX(), moveDirY());
        controller.update();
        repaint();
        if (!controller.isRunning()) {
            timer.stop();
        }
    }

    /** 启动游戏循环。 */
    public void start() {
        timer.start();
        // 推迟到窗口焦点事务完成后再请求画布焦点，避免某些平台下 WASD 首次点击前不响应
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    /** 停止游戏循环。 */
    public void stop() {
        timer.stop();
    }

    /** 合成横向移动分量：右(+1) - 左(-1)，方向键与 WASD 等价。 */
    private int moveDirX() {
        int dx = 0;
        if (pressedKeys.contains(KeyEvent.VK_D) || pressedKeys.contains(KeyEvent.VK_RIGHT)) {
            dx++;
        }
        if (pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_LEFT)) {
            dx--;
        }
        return dx;
    }

    /** 合成纵向移动分量：下(+1) - 上(-1)，方向键与 WASD 等价。 */
    private int moveDirY() {
        int dy = 0;
        if (pressedKeys.contains(KeyEvent.VK_S) || pressedKeys.contains(KeyEvent.VK_DOWN)) {
            dy++;
        }
        if (pressedKeys.contains(KeyEvent.VK_W) || pressedKeys.contains(KeyEvent.VK_UP)) {
            dy--;
        }
        return dy;
    }

    /**
     * 设置"退出本局"按钮点击后的回调。
     *
     * @param onExit 回调（在 EDT 上触发）
     */
    public void setOnExit(Runnable onExit) {
        this.onExit = onExit;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        // 固定层：底色（不随震屏移动，避免边缘露白）
        g2.setColor(new Color(238, 238, 242));
        g2.fillRect(0, 0, w, h);

        // 世界层：受震屏影响整体平移
        int sx = controller.getShakeX();
        int sy = controller.getShakeY();
        g2.translate(sx, sy);
        drawGrid(g2, w, h);
        controller.getPlayer().draw(g2);
        for (Zombie z : controller.getZombies()) {
            z.draw(g2);
        }
        for (Bullet b : controller.getBullets()) {
            b.draw(g2);
        }
        for (Particle p : controller.getParticles()) {
            p.draw(g2);
        }
        for (FloatingText ft : controller.getFloatingTexts()) {
            ft.draw(g2);
        }
        g2.translate(-sx, -sy);

        // 固定层：暗角 + HUD + 退出按钮
        drawVignette(g2, w, h);
        drawHud(g2);
        drawExitButton(g2);

        // 受伤红屏（全屏覆盖，强度随剩余帧衰减）
        float dmg = controller.getDamageFlash();
        if (dmg > 0f) {
            g2.setColor(new Color(220, 30, 30, (int) (dmg * 110)));
            g2.fillRect(0, 0, w, h);
        }

        // 准星
        drawCrosshair(g2);
    }

    /**
     * 绘制棋盘网格地面（世界层，略微越界以覆盖震屏平移产生的缝隙）。
     *
     * @param g2 画布绘图上下文
     * @param w  画布宽度
     * @param h  画布高度
     */
    private void drawGrid(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(222, 224, 230));
        int step = 40;
        for (int gx = -step; gx <= w + step; gx += step) {
            g2.drawLine(gx, -step, gx, h + step);
        }
        for (int gy = -step; gy <= h + step; gy += step) {
            g2.drawLine(-step, gy, w + step, gy);
        }
    }

    /**
     * 绘制径向暗角（固定层，营造聚光感）。
     *
     * @param g2 画布绘图上下文
     * @param w  画布宽度
     * @param h  画布高度
     */
    private void drawVignette(Graphics2D g2, int w, int h) {
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = (float) Math.hypot(w, h) * 0.55f;
        float[] dist = {0.55f, 1.0f};
        Color[] colors = {new Color(0, 0, 0, 0), new Color(0, 0, 0, 95)};
        g2.setPaint(new RadialGradientPaint(cx, cy, radius, dist, colors));
        g2.fillRect(0, 0, w, h);
    }

    /**
     * 在鼠标位置绘制准星（圆环 + 四向缺口十字 + 中心红点）。
     *
     * @param g2 画布绘图上下文
     */
    private void drawCrosshair(Graphics2D g2) {
        int x = mouseX;
        int y = mouseY;
        g2.setColor(new Color(40, 40, 40, 200));
        g2.drawOval(x - 9, y - 9, 18, 18);
        g2.drawLine(x - 14, y, x - 4, y);
        g2.drawLine(x + 4, y, x + 14, y);
        g2.drawLine(x, y - 14, x, y - 4);
        g2.drawLine(x, y + 4, x, y + 14);
        g2.setColor(new Color(220, 60, 60, 220));
        g2.fillOval(x - 2, y - 2, 4, 4);
    }

    /**
     * 绘制左上角状态：分数 / 血量 / 击杀 / 时间。
     *
     * @param g2 画布绘图上下文
     */
    private void drawHud(Graphics2D g2) {
        // 顶行：分数 / 击杀 / 时间
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("微软雅黑", Font.BOLD, 14));
        String hud = String.format("分数:%d  击杀:%d  时间:%ds",
                controller.getScore(),
                controller.getKillCount(),
                controller.getElapsedSec());
        g2.drawString(hud, 10, 20);

        // 血条：深灰圆角背景 + 分段色前景（绿/黄/红）
        int barX = 10, barY = 28, barW = 200, barH = 14;
        int hp = controller.getHp();
        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(barX, barY, barW, barH, 6, 6);
        int fgW = (int) (barW * hp / 100.0);
        if (hp > 60) {
            g2.setColor(new Color(80, 200, 80));   // 健康：绿
        } else if (hp > 30) {
            g2.setColor(new Color(235, 200, 60));  // 受损：黄
        } else {
            g2.setColor(new Color(220, 70, 70));   // 危险：红
        }
        if (fgW > 0) {
            g2.fillRoundRect(barX, barY, fgW, barH, 6, 6);
        }
        // 血量数值（居中显示在条上）
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        String hpText = "血量 " + hp;
        int tx = barX + (barW - fm.stringWidth(hpText)) / 2;
        int ty = barY + (barH - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(hpText, tx, ty);
    }

    /** 退出按钮横坐标（贴画布右侧）。 */
    private int exitX() {
        return getWidth() - EXIT_W - 10;
    }

    /** 判断坐标是否落在"退出本局"按钮内。 */
    private boolean isInExitButton(int x, int y) {
        return x >= exitX() && x <= exitX() + EXIT_W
                && y >= EXIT_Y && y <= EXIT_Y + EXIT_H;
    }

    /**
     * 绘制右上角"退出本局"按钮。
     *
     * @param g2 画布绘图上下文
     */
    private void drawExitButton(Graphics2D g2) {
        int x = exitX();
        g2.setColor(new Color(220, 80, 80));
        g2.fillRoundRect(x, EXIT_Y, EXIT_W, EXIT_H, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        String label = "退出本局";
        int tx = x + (EXIT_W - fm.stringWidth(label)) / 2;
        int ty = EXIT_Y + (EXIT_H - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(label, tx, ty);
    }
}
