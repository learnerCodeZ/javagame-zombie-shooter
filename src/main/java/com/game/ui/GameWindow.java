package com.game.ui;

import com.game.dao.RecordDao;
import com.game.game.Difficulty;
import com.game.game.GameController;
import com.game.game.GamePanel;
import com.game.model.GameRecord;
import com.game.model.User;
import com.game.util.GameSettings;
import com.game.util.UIStyle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 游戏主窗口：承载游戏画布，负责局内结算与"设置 / 暂停"菜单。
 *
 * <p>三种去向：
 * <ul>
 *   <li>玩家被打死：{@link GameController} 通过 onGameOver 回调触发
 *       {@link #settle()}（写战绩 + 弹"再来一局 / 回主菜单"，死亡不二次确认）；</li>
 *   <li>点右上角"设置"按钮：打开 {@link #openSettings()} 暂停循环，菜单内可调音量/静音、
 *       继续、再来一局（确认）、回主菜单（确认）；</li>
 *   <li>点 X 关窗：停止循环并回主菜单（不结算）。</li>
 * </ul>
 *
 * <p>用 {@link #settled} 保证死亡结算只执行一次；用 {@link #settingsOpen} 防止设置菜单被重复打开。
 */
public class GameWindow extends JFrame {

    /** 当前登录用户（结算写 user_id、重开后保持同一身份） */
    private final User currentUser;
    /** 本局难度（"再来一局"重开时保持同一难度，不掉档） */
    private final Difficulty difficulty;
    /** 游戏控制器（游戏大脑） */
    private final GameController controller;
    /** 游戏画布（内含刷新定时器） */
    private final GamePanel panel;
    /** 结算是否已执行（防止"死亡回调"重复触发，并阻止死亡后再开设置菜单） */
    private boolean settled;
    /** 设置菜单是否已打开（防重复打开） */
    private boolean settingsOpen;

    /**
     * 构造方法。
     *
     * @param user       当前登录用户
     * @param difficulty 本局难度（重开时由调用方原样传入，保持同一档位）
     */
    public GameWindow(User user, Difficulty difficulty) {
        if (user == null) {
            throw new IllegalArgumentException("登录用户不能为空");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("难度不能为空");
        }
        this.currentUser = user;
        this.difficulty = difficulty;

        setTitle("打僵尸 [" + difficulty.label + "]");
        setSize(820, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        // 窗体内容底设为最深的 BG，与暗色主题统一（画布会完整覆盖，此处主要防边缘闪白）
        getContentPane().setBackground(UIStyle.BG);

        // 创建控制器，注册游戏结束回调（血量归零 → 结算）；按本局难度注入刷怪/伤害参数
        this.controller = new GameController(difficulty);
        this.controller.setOnGameOver(() -> SwingUtilities.invokeLater(this::settle));

        // 创建画布并装入窗口
        this.panel = new GamePanel(controller);
        add(panel);

        // 右上角"设置"按钮：暂停循环 + 打开"设置/暂停"菜单
        this.panel.setOnSettings(() -> SwingUtilities.invokeLater(this::openSettings));

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
     * 把本局战绩写入数据库（失败仅打印日志，不弹窗、不阻断后续流程）。
     * <p>供死亡结算 {@link #settle()} 与设置菜单中的"再来一局 / 回主菜单"复用。
     */
    private void saveCurrentRecord() {
        GameRecord record = new GameRecord();
        record.setUserId(currentUser.getId());
        record.setScore(controller.getScore());
        record.setKillCount(controller.getKillCount());
        record.setSurviveSec(controller.getElapsedSec());
        record.setDifficulty(difficulty.name());
        try {
            new RecordDao().saveRecord(record);
        } catch (Exception ex) {
            // DBUtil 连不上库时会把 SQLException 包成 RuntimeException 抛出，
            // 这里兜一层：只打印日志，不打断调用方流程
            ex.printStackTrace();
        }
    }

    /**
     * 本局结算（玩家死亡用）：停循环 → 写战绩 → 弹 [再来一局 / 回主菜单]。
     * <p>用 {@link #settled} 保证只执行一次；死亡结算不二次确认（直接给去向选择）。
     * 通过 {@link SwingUtilities#invokeLater} 调用，推迟到当前帧绘制之后再弹模态框，
     * 避免在 Timer 回调中段阻塞。
     */
    private void settle() {
        if (settled) {
            return;
        }
        settled = true;
        panel.stop();
        saveCurrentRecord();

        int sec = controller.getElapsedSec();
        // 弹窗选择去向（死亡不二次确认）
        String msg = String.format("本局结束！\n分数:%d  击杀:%d  存活:%ds",
                controller.getScore(), controller.getKillCount(), sec);
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
            new GameWindow(currentUser, difficulty).setVisible(true);
        } else {
            // 回主菜单（用户直接关闭对话框也按"回主菜单"处理）
            dispose();
            new MainFrame(currentUser).setVisible(true);
        }
    }

    /**
     * 打开"设置 / 暂停"菜单：先停游戏循环（暂停），弹出模态对话框。
     * <p>菜单内含：音量滑块、静音勾选（实时写入 {@link GameSettings}），
     * 以及「继续游戏 / 再来一局 / 回主菜单」三个按钮。
     * <p>X 关闭按「继续游戏」处理（恢复循环）；
     * 用 {@link #settingsOpen} 防重复打开，用 {@link #settled} 阻止死亡结算后再开。
     */
    private void openSettings() {
        if (settled || settingsOpen || !controller.isRunning()) {
            // 已死亡结算 / 已开菜单 / 游戏已结束时不再开设置菜单，避免与死亡结算双弹窗
            return;
        }
        settingsOpen = true;
        panel.stop(); // 暂停游戏循环

        JDialog dialog = new JDialog(this, "设置 / 暂停", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(16, 20, 16, 20));
        UIStyle.apply(content); // 设置菜单面板：PANEL 底色 + 不透明

        // 音量行：标签 + 实时数值
        Box volRow = Box.createHorizontalBox();
        volRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel volLabel = UIStyle.body("音量:");
        JLabel volValue = UIStyle.body(String.valueOf(GameSettings.getVolume()));
        volValue.setPreferredSize(new Dimension(36, 22));
        volRow.add(volLabel);
        volRow.add(Box.createHorizontalStrut(8));
        volRow.add(volValue);
        volRow.add(Box.createHorizontalGlue());
        content.add(volRow);
        content.add(Box.createVerticalStrut(8));

        // 音量滑块（0~100，拖动时实时写入 GameSettings 并刷新数值标签）
        JSlider slider = new JSlider(0, 100, GameSettings.getVolume());
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        slider.setFocusable(false);
        slider.setBackground(UIStyle.PANEL);
        slider.setForeground(UIStyle.TEXT);
        slider.setOpaque(true);
        slider.addChangeListener(e -> {
            int v = slider.getValue();
            GameSettings.setVolume(v);
            volValue.setText(String.valueOf(v));
        });
        content.add(slider);
        content.add(Box.createVerticalStrut(12));

        // 静音勾选
        JCheckBox muteBox = new JCheckBox("静音", GameSettings.isMuted());
        muteBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        muteBox.setFocusable(false);
        muteBox.setBackground(UIStyle.PANEL);
        muteBox.setForeground(UIStyle.TEXT);
        muteBox.setOpaque(true);
        muteBox.addActionListener(e -> GameSettings.setMuted(muteBox.isSelected()));
        content.add(muteBox);
        content.add(Box.createVerticalStrut(18));

        // 按钮行：继续游戏 / 再来一局 / 退出游戏
        Box btnRow = Box.createHorizontalBox();
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton resumeBtn = UIStyle.primary("继续游戏");
        JButton restartBtn = UIStyle.secondary("再来一局");
        JButton exitBtn = UIStyle.danger("退出游戏");
        btnRow.add(resumeBtn);
        btnRow.add(Box.createHorizontalStrut(8));
        btnRow.add(restartBtn);
        btnRow.add(Box.createHorizontalStrut(8));
        btnRow.add(exitBtn);
        content.add(btnRow);

        // 继续游戏：关菜单 + 恢复循环
        resumeBtn.addActionListener(e -> {
            dialog.dispose();
            panel.start();
        });

        // 再来一局：确认；YES 则关菜单 + 存档 + 关本窗 + 开新一局；NO 留在菜单（游戏仍暂停）
        restartBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(dialog,
                    "放弃当前进度并重新开始?", "确认",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c == JOptionPane.YES_OPTION) {
                dialog.dispose();
                if (!settled) {
                    saveCurrentRecord();
                }
                dispose();
                new GameWindow(currentUser, difficulty).setVisible(true);
            }
        });

        // 退出游戏：确认；YES 则关菜单 + 存档 + 弹结算(带"确定"键) + 回主菜单；NO 留在菜单
        exitBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(dialog,
                    "确定退出游戏?本局将结算存档", "确认",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c == JOptionPane.YES_OPTION) {
                dialog.dispose();
                if (!settled) {
                    saveCurrentRecord();
                }
                // 和死亡同款结算：显示分数/击杀/存活，带"确定"键
                String result = String.format("本局结算\n分数:%d  击杀:%d  存活:%ds",
                        controller.getScore(), controller.getKillCount(), controller.getElapsedSec());
                JOptionPane.showMessageDialog(this, result, "本局结算",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
                new MainFrame(currentUser).setVisible(true);
            }
        });

        // X 关闭 → 按"继续游戏"处理：恢复游戏循环（用户没选择离开）
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panel.start();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                settingsOpen = false;
            }
        });

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this); // 居中于游戏窗口
        dialog.setVisible(true);
    }
}
