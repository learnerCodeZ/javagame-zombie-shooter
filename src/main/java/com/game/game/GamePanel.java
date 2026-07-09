package com.game.game;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

    /**
     * 构造方法。
     *
     * @param c 游戏控制器
     */
    public GamePanel(GameController c) {
        this.controller = c;
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);
        // 鼠标移动/拖拽 → 更新瞄准角度
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                controller.setAim(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
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
        this.timer = new Timer(16, e -> tick());
    }

    /**
     * 单帧回调：推进逻辑、重绘、必要时停止定时器。
     */
    private void tick() {
        controller.update();
        repaint();
        if (!controller.isRunning()) {
            timer.stop();
        }
    }

    /** 启动游戏循环。 */
    public void start() {
        timer.start();
    }

    /** 停止游戏循环。 */
    public void stop() {
        timer.stop();
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
        // 浅色背景
        g2.setColor(new Color(245, 245, 245));
        g2.fillRect(0, 0, getWidth(), getHeight());
        // 绘制对象
        controller.getPlayer().draw(g2);
        for (Zombie z : controller.getZombies()) {
            z.draw(g2);
        }
        for (Bullet b : controller.getBullets()) {
            b.draw(g2);
        }
        // 左上角 HUD
        drawHud(g2);
        // 右上角"退出本局"按钮
        drawExitButton(g2);
    }

    /**
     * 绘制左上角状态：分数 / 血量 / 击杀 / 时间。
     *
     * @param g2 画布绘图上下文
     */
    private void drawHud(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("微软雅黑", Font.BOLD, 14));
        String hud = String.format("分数:%d  血量:%d  击杀:%d  时间:%ds",
                controller.getScore(),
                controller.getHp(),
                controller.getKillCount(),
                controller.getElapsedSec());
        g2.drawString(hud, 10, 20);
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
