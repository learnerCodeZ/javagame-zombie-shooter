package com.game.ui;

import com.game.model.User;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * 主菜单窗口。
 * 登录成功后展示，提供"开始游戏 / 排行榜 / 游戏说明 / 设置 / 退出登录"
 * 五个功能入口；记录当前登录用户，后续阶段的实际功能在此处接入。
 */
public class MainFrame extends JFrame {

    /** 当前登录用户 */
    private final User currentUser;

    /**
     * 构造方法。
     *
     * @param user 当前登录用户，用于显示昵称及后续功能取用
     */
    public MainFrame(User user) {
        if (user == null) {
            throw new IllegalArgumentException("登录用户不能为空");
        }
        this.currentUser = user;
        String displayName = (currentUser.getNickname() == null || currentUser.getNickname().isEmpty())
                ? currentUser.getUsername() : currentUser.getNickname();
        setTitle("主菜单 - " + displayName);
        setSize(480, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
    }

    /**
     * 初始化界面组件：纵排五个大按钮。
     */
    private void initUI() {
        JButton startButton = createMenuButton("开始游戏");
        JButton rankButton = createMenuButton("排行榜");
        JButton helpButton = createMenuButton("游戏说明");
        JButton settingButton = createMenuButton("设置");
        JButton logoutButton = createMenuButton("退出登录");

        // 各按钮事件（排行榜、设置为占位，后续阶段实现）
        startButton.addActionListener(
                e -> {
                    dispose();
                    new GameWindow(currentUser).setVisible(true);
                });
        rankButton.addActionListener(
                e -> JOptionPane.showMessageDialog(this, "排行榜将在阶段⑤实现"));
        helpButton.addActionListener(e -> showHelp());
        settingButton.addActionListener(
                e -> JOptionPane.showMessageDialog(this, "设置功能开发中"));
        logoutButton.addActionListener(e -> doLogout());

        // 纵向排列，按钮间留白
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 80, 20, 80));
        panel.add(startButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(rankButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(helpButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(settingButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(logoutButton);

        add(panel);
    }

    /**
     * 创建主菜单大按钮：统一宽度、居中、字号放大。
     *
     * @param text 按钮文字
     * @return 配置好的按钮
     */
    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        return button;
    }

    /**
     * 弹出游戏说明：鼠标瞄准射击僵尸等玩法。
     */
    private void showHelp() {
        String help = "【游戏说明】\n"
                + "1. 移动鼠标：控制准星瞄准僵尸；\n"
                + "2. 单击左键：发射子弹击杀僵尸；\n"
                + "3. 击杀僵尸获得分数，连击有额外奖励；\n"
                + "4. 僵尸靠近底线或撞到玩家会扣除生命值；\n"
                + "5. 生命值归零则游戏结束，可重新开始。\n"
                + "祝你好运！";
        JOptionPane.showMessageDialog(this, help,
                "游戏说明", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 退出登录：关闭本窗并回到登录窗口。
     */
    private void doLogout() {
        dispose();
        new LoginFrame().setVisible(true);
    }
}
