package com.game.game;

import com.game.util.UIStyle;// UI 颜色/字体常量

import javax.swing.JPanel;// 游戏画布
import javax.swing.Timer;// Swing 定时器（游戏循环）
import javax.swing.SwingUtilities;// Swing 工具类（线程调度）
//AWT —— 底层绘图与事件
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;//适配器
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * 游戏画布：持有控制器与刷新定时器，负责绘制并把输入转发给控制器。
 * <p>画布 800x600，约 60 FPS（Timer 间隔 16ms）。右上角有"设置"按钮（点开"设置/暂停"菜单）。
 */
public class GamePanel extends JPanel {

    private final GameController controller;// 游戏控制器（逻辑层）
    private final Timer timer;// Swing 定时器，驱动游戏循环（约 60FPS）

    /** 用于把鼠标"锁"在画布内(游戏运行中,鼠标移出即拉回中心);拿不到 Robot 时降级为不锁。 */
    private Robot robot;// 用来把鼠标"锁"在画布内

    /** 右上角"设置"按钮：宽 / 高 / 距顶（横坐标按画布实际宽度动态贴右） */
    private static final int EXIT_W = 84;
    private static final int EXIT_H = 28;
    private static final int EXIT_Y = 10;
    /** 点"设置"按钮后的回调（由 GameWindow 注入：停循环 + 打开"设置/暂停"菜单） */
    private Runnable onSettings;// 点击"设置"按钮后执行的回调

    /** 当前处于按下状态的移动键码集合（WASD + 方向键），失焦时清空防卡键 */
    private final Set<Integer> pressedKeys = new HashSet<>();// 当前按下的键集合

    /** 最近一次鼠标位置（默认画布中心），用于绘制准星 */
    private int mouseX = 400;// 鼠标最近 x 坐标（默认画布中心）
    private int mouseY = 300;// 鼠标最近 y 坐标（默认画布中心）

    /**
     * 构造方法。
     *
     * @param c 游戏控制器
     */
    public GamePanel(GameController c) {
        this.controller = c;
        setPreferredSize(new Dimension(800, 600));// 画布大小 800×600
        setFocusable(true);// 允许接收键盘输入
        // 鼠标锁定:懒加载 Robot(个别环境拿不到则降级——不锁,但不影响游戏)
        try {
            robot = new Robot();
        } catch (AWTException e) {
            robot = null;
        }
        // 隐藏系统光标:画布上只显示自绘准星(失败则保留默认光标)
        try {
            setCursor(getToolkit().createCustomCursor(
                    new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), 
                    // 1×1 全透明图片
                    new Point(0, 0), "blank"));
        } catch (Exception e) {
            // 个别平台不支持自定义光标,忽略
        }
        // 鼠标移动/拖拽 → 更新瞄准角度并记录准星位置
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {//没按
                mouseX = e.getX(); // 记录准星位置
                mouseY = e.getY();
                controller.setAim(e.getX(), e.getY());// 告诉控制器：玩家朝这瞄准
            }

            @Override
            public void mouseDragged(MouseEvent e) {//按下
                mouseX = e.getX();// 拖拽时也更新（按住左键拖动）
                mouseY = e.getY();
                controller.setAim(e.getX(), e.getY());
            }
        });
        // 鼠标按下 → 先判"设置"按钮，否则开火
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isInExitButton(e.getX(), e.getY())) {
                    if (onSettings != null) {
                        onSettings.run();// ← 调用 Runnable
                       // Runnable 就是"一段你可以提前告诉它、以后再执行的代码"
                    }
                    return;
                }
                controller.shoot();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                grabMouseBack(e);// 鼠标滑出画布 → 推回来
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
                pressedKeys.clear();// 焦点丢失时清空所有按键状态

            }
        });
        this.timer = new Timer(16, e -> tick());
        // 16ms ≈ 62.5 FPS（约 60FPS）
    }

    /**
     * 单帧回调：推进逻辑、重绘、必要时停止定时器。
     */
    private void tick() {
        controller.setMoveDir(moveDirX(), moveDirY());// 把键盘输入转换为移动方向
        controller.update();// 推进一帧游戏逻辑
        repaint();// 请求重绘画面
        if (!controller.isRunning()) {
            timer.stop();//4) 游戏结束就停定时器
        }
    }

    /** 启动游戏循环。 */
    public void start() {
        timer.start();
        // 推迟到窗口焦点事务完成后再请求画布焦点，避免某些平台下 WASD 首次点击前不响应
        SwingUtilities.invokeLater(this::requestFocusInWindow);
        // 延迟请求焦点
    }

    /** 停止游戏循环。 */
    public void stop() {
        timer.stop();
    }

    /**
     * 游戏运行中,把滑出画布的鼠标推回"出界点内侧"几像素,防止点到别处。
     * <p>比"瞬移到中心"更柔和——保留玩家原本的瞄准方向,只在边界处轻微反弹。
     * <p>仅当:游戏循环在跑(timer 运行中)<b>且</b>本窗口处于活动态(用户没切到别的程序)时才推回；
     * 暂停(设置菜单)/本局结束/切到别的窗口时自动放开,不困住鼠标。
     */
    private void grabMouseBack(MouseEvent e) {
        if (robot == null || !timer.isRunning()) {
            return;// 没 Robot 或游戏暂停 → 不管
        }
        Window win = SwingUtilities.getWindowAncestor(this);
        if (win == null || !win.isActive()) {
            return; // 用户切到别的窗口:别抢鼠标
        }
        // 推回出界点内侧 margin 像素:把出界坐标钳进画布、离边 margin,保留瞄准方向
        final int margin = 8;
        int cx = Math.max(margin, Math.min(getWidth() - margin, e.getX()));
        int cy = Math.max(margin, Math.min(getHeight() - margin, e.getY()));
        Point p = new Point(cx, cy);
        SwingUtilities.convertPointToScreen(p, this);// 画布坐标 → 屏幕坐标
        robot.mouseMove(p.x, p.y);// 把鼠标推回去
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
     * 设置"设置"按钮点击后的回调。
     *
     * @param onSettings 回调（在 EDT 上触发）
     */
    public void setOnSettings(Runnable onSettings) { 
        // 点击"设置"按钮后执行的回调
        this.onSettings = onSettings;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);// 先让父类清理背景
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        // 固定层：底色（不随震屏移动，避免边缘露白）—— 暗色主题底
        g2.setColor(UIStyle.BG);
        g2.fillRect(0, 0, w, h);

        // 世界层：受震屏影响整体平移
        int sx = controller.getShakeX();
        int sy = controller.getShakeY();
        g2.translate(sx, sy); // 整体平移（震屏效果）
        drawGrid(g2, w, h);// 网格地面
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
        drawVignette(g2, w, h);// 暗角效果
        drawHud(g2);// 左上角状态信息
        drawExitButton(g2);// 右上角设置按钮

        // 受伤红屏（全屏覆盖，强度随剩余帧衰减）
        float dmg = controller.getDamageFlash();
        if (dmg > 0f) {
            g2.setColor(new Color(220, 30, 30, (int) (dmg * 110)));
            // 红色半透明
            g2.fillRect(0, 0, w, h);
            // 全屏覆盖
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
    private void drawGrid(Graphics2D g2, int w, int h) {//网格
        g2.setColor(UIStyle.PANEL);
        int step = 40;
        for (int gx = -step; gx <= w + step; gx += step) {// 竖线
            g2.drawLine(gx, -step, gx, h + step);
        }
        for (int gy = -step; gy <= h + step; gy += step) {//横线
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
    //让画面四周变暗，中间明亮
    private void drawVignette(Graphics2D g2, int w, int h) {
        float cx = w / 2f;// 画布中心
        float cy = h / 2f;
        float radius = (float) Math.hypot(w, h) * 0.55f;// 渐变半径
        float[] dist = {0.55f, 1.0f};// 渐变起止
        Color[] colors = {new Color(0, 0, 0, 0), new Color(0, 0, 0, 95)};
        // 透明 → 半透明黑
        g2.setPaint(new RadialGradientPaint(cx, cy, radius, dist, colors));
        g2.fillRect(0, 0, w, h);
    }

    /**
     * 在鼠标位置绘制准星（圆环 + 四向缺口十字 + 中心红点）。
     *
     * @param g2 画布绘图上下文
     */
    private void drawCrosshair(Graphics2D g2) {
        // 圆环（外圈）
        int x = mouseX;
        int y = mouseY;
        // 四向缺口十字线（中间留 8px 空隙给中心红点）
        g2.setColor(new Color(40, 40, 40, 200));
        g2.drawOval(x - 9, y - 9, 18, 18);
        g2.drawLine(x - 14, y, x - 4, y);
        g2.drawLine(x + 4, y, x + 14, y);
        g2.drawLine(x, y - 14, x, y - 4);
        g2.drawLine(x, y + 4, x, y + 14);
        // 中心红点
        g2.setColor(new Color(220, 60, 60, 220));
        g2.fillOval(x - 2, y - 2, 4, 4);
    }

    /**
     * 绘制左上角状态：分数 / 血量 / 击杀 / 时间。
     *
     * @param g2 画布绘图上下文
     */
    private void drawHud(Graphics2D g2) {
        // 顶行：分数 / 击杀 / 时间（暗色画布上用亮色文字保证清晰）
        g2.setColor(UIStyle.TEXT);
        g2.setFont(new Font("微软雅黑", Font.BOLD, 14));
        String hud = String.format("分数:%d  击杀:%d  时间:%ds",
                controller.getScore(),
                controller.getKillCount(),
                controller.getElapsedSec());
        g2.drawString(hud, 10, 20);

        // 难度标签（紧跟 HUD 行右侧）：困难红、简单绿，让玩家一眼知道当前档位
        Difficulty diff = controller.getDifficulty();
        FontMetrics hfm = g2.getFontMetrics();
        g2.setColor(diff == Difficulty.HARD
                ? new Color(235, 90, 90)
                : new Color(90, 200, 120));
        g2.drawString("[" + diff.label + "]", 10 + hfm.stringWidth(hud) + 10, 20);

        // 血条：暗色圆角背景 + 分段色前景（绿/黄/红）
        int barX = 10, barY = 28, barW = 200, barH = 14;
        int hp = controller.getHp();
        g2.setColor(UIStyle.FIELD);
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

    /** 判断坐标是否落在"设置"按钮内。 */
    private boolean isInExitButton(int x, int y) {
        return x >= exitX() && x <= exitX() + EXIT_W
                && y >= EXIT_Y && y <= EXIT_Y + EXIT_H;
    }

    /**
     * 绘制右上角"设置"按钮。
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
        String label = "设置";
        int tx = x + (EXIT_W - fm.stringWidth(label)) / 2;
        int ty = EXIT_Y + (EXIT_H - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(label, tx, ty);
    }
}
