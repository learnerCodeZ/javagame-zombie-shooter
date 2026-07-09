package com.game.ui;

import com.game.dao.RecordDao;
import com.game.game.GameController;
import com.game.game.GamePanel;
import com.game.model.GameRecord;
import com.game.model.User;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 游戏主窗口：承载游戏画布，并在本局结束时（被打死 或 主动点"退出本局"）
 * 把战绩写入数据库、弹窗让玩家选择"再来一局"或"回主菜单"。
 * <p>玩家血量归零由 {@link GameController} 通过回调通知结算；
 * 点"退出本局"按钮也会触发同一套结算；
 * 玩家手动关闭窗口（点 X）会停止游戏循环并回到主菜单（不结算）。
 */
public class GameWindow extends JFrame {

    /** 当前登录用户（结算写 user_id、重开后保持同一身份） */
    private final User currentUser;
    /** 游戏控制器（游戏大脑） */
    private final GameController controller;
    /** 游戏画布（内含刷新定时器） */
    private final GamePanel panel;
    /** 结算是否已执行（防止"死亡回调"与"退出本局"在同一帧内重复触发） */
    private boolean settled;

    /**
     * 构造方法。
     *
     * @param user 当前登录用户
     */
    public GameWindow(User user) {
        if (user == null) {
            throw new IllegalArgumentException("登录用户不能为空");
        }
        this.currentUser = user;

        setTitle("打僵尸");
        setSize(820, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // 创建控制器，注册游戏结束回调（血量归零 → 结算）
        this.controller = new GameController();
        this.controller.setOnGameOver(() -> SwingUtilities.invokeLater(this::settle));

        // 创建画布并装入窗口
        this.panel = new GamePanel(controller);
        add(panel);

        // 右上角"退出本局"按钮：同样走结算（存战绩 + 弹选择框）
        this.panel.setOnExit(() -> SwingUtilities.invokeLater(this::settle));

        // 窗口可见后启动循环；点 X 关窗时停止循环并回主菜单（不结算）
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                panel.start();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                panel.stop();
                new MainFrame(currentUser).setVisible(true);
            }
        });
    }

    /**
     * 本局结算：停循环 → 写战绩（失败不阻断弹窗）→ 弹 [再来一局 / 回主菜单]。
     * <p>死亡与"退出本局"共用本方法；用 {@link #settled} 保证只执行一次。
     * 通过 {@link SwingUtilities#invokeLater} 调用，推迟到当前帧绘制之后再弹模态框，
     * 避免在 Timer 回调中段阻塞。
     */
    private void settle() {
        if (settled) {
            return;
        }
        settled = true;
        panel.stop();

        int sec = controller.getElapsedSec();

        // 1) 构造并写入战绩（失败不阻断结算弹窗）
        GameRecord record = new GameRecord();
        record.setUserId(currentUser.getId());
        record.setScore(controller.getScore());
        record.setKillCount(controller.getKillCount());
        record.setSurviveSec(sec);
        String extra = "";
        try {
            new RecordDao().saveRecord(record);
        } catch (Exception ex) {
            // DBUtil 连不上库时会把 SQLException 包成 RuntimeException 抛出，
            // 这里再兜一层：打印日志并追加提示，保证下方结算弹窗照常弹出
            ex.printStackTrace();
            extra = "\n（战绩保存失败，请检查数据库连接）";
        }

        // 2) 弹窗选择去向
        String msg = String.format("本局结束！\n分数:%d  击杀:%d  存活:%ds",
                controller.getScore(), controller.getKillCount(), sec) + extra;
        int choice = JOptionPane.showOptionDialog(
                this,
                msg,
                "本局结算",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[]{"再来一局", "回主菜单"},
                "再来一局");

        if (choice == 0) {
            // 再来一局：关闭本窗，开新一局
            dispose();
            new GameWindow(currentUser).setVisible(true);
        } else {
            // 回主菜单（用户直接关闭对话框也按"回主菜单"处理）
            dispose();
            new MainFrame(currentUser).setVisible(true);
        }
    }
}
