package com.game.ui;

import com.game.game.Difficulty;
import com.game.model.User;
import com.game.util.UIStyle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;

/**
 * 主菜单窗口。
 * 登录成功后展示，提供"开始游戏 / 排行榜 / 修改密码 / 用户管理(仅 admin) /
 * 游戏说明 / 设置 / 退出登录"等功能入口；记录当前登录用户，供后续功能取用。
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
                ? currentUser.getPhone() : currentUser.getNickname();
        setTitle("主菜单 - " + displayName);
        setSize(480, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
    }

    /**
     * 初始化界面组件：纵排大按钮。
     * "修改密码"对所有用户可见；"用户管理"仅 admin 可见。
     */
    private void initUI() {
        // 暗色窗体底
        getContentPane().setBackground(UIStyle.BG);

        // 按语义分配主色：开始游戏 / 用户管理 → 主色橙；排行榜等次操作 → 次色绿；退出 → 危险红
        JButton startButton = sizeMenu(UIStyle.primary("开始游戏"));
        JButton rankButton = sizeMenu(UIStyle.secondary("排行榜"));
        JButton changePwdButton = sizeMenu(UIStyle.secondary("修改密码"));
        JButton adminButton = sizeMenu(UIStyle.primary("用户管理"));
        JButton helpButton = sizeMenu(UIStyle.secondary("游戏说明"));
        JButton settingButton = sizeMenu(UIStyle.secondary("设置"));
        JButton logoutButton = sizeMenu(UIStyle.danger("退出登录"));

        boolean isAdmin = "admin".equals(currentUser.getRole());

        // 开始游戏：先弹难度选择（简单/困难），取消则不开始、留在主菜单
        startButton.addActionListener(
                e -> {
                    int choice = JOptionPane.showOptionDialog(
                            this,
                            "选择游戏难度",
                            "难度选择",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new Object[]{"简单", "困难"},
                            "简单");
                    if (choice == JOptionPane.CLOSED_OPTION) {
                        return; // 关闭对话框 = 取消，不开始
                    }
                    Difficulty diff = (choice == 1) ? Difficulty.HARD : Difficulty.EASY;
                    dispose();
                    new GameWindow(currentUser, diff).setVisible(true);
                });
        rankButton.addActionListener(
                e -> new LeaderboardFrame(currentUser).setVisible(true));
        changePwdButton.addActionListener(
                e -> new ChangePasswordDialog(this, currentUser).setVisible(true));
        // 仅 admin 才打开用户管理；MainFrame 仍开着（AdminFrame 独立 dispose）
        adminButton.addActionListener(
                e -> new AdminFrame(currentUser).setVisible(true));
        helpButton.addActionListener(e -> showHelp());
        settingButton.addActionListener(
                e -> JOptionPane.showMessageDialog(this, "设置功能开发中"));
        logoutButton.addActionListener(e -> doLogout());

        // 纵向排列，按钮间留白（透明面板，统一暗底）
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(22, 80, 22, 80));
        addMenuButton(panel, startButton);
        addMenuButton(panel, rankButton);
        addMenuButton(panel, changePwdButton);
        if (isAdmin) {
            addMenuButton(panel, adminButton);
        }
        addMenuButton(panel, helpButton);
        addMenuButton(panel, settingButton);
        addMenuButton(panel, logoutButton);

        add(panel);
    }

    /**
     * 把菜单按钮加入面板，并在下方留一段竖直空白。
     *
     * @param panel  面板
     * @param button 按钮
     */
    private void addMenuButton(JPanel panel, JButton button) {
        panel.add(button);
        panel.add(Box.createVerticalStrut(15));
    }

    /**
     * 统一菜单按钮尺寸：横向撑满、固定高度、居中对齐。
     *
     * @param button 已由 UIStyle 创建好的按钮
     * @return 同一按钮（便于链式调用）
     */
    private JButton sizeMenu(JButton button) {
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        return button;
    }

    /**
     * 弹出游戏说明：鼠标瞄准射击僵尸等玩法。
     */
    private void showHelp() {
        String help = "【游戏说明】\n"
                + "1. W/A/S/D 或方向键：移动角色躲避僵尸；\n"
                + "2. 移动鼠标：控制准星瞄准；\n"
                + "3. 单击左键：发射子弹击杀僵尸；\n"
                + "4. 击杀得分：普通僵尸 10 分、壮汉 25 分；\n"
                + "5. 僵尸撞到玩家会扣血（撞到后该僵尸消失），血量归零则本局结束；\n"
                + "6. 开始游戏时可选 简单 / 困难 难度，越往后僵尸刷得越密；\n"
                + "7. 右上角\"设置\"按钮：暂停游戏、调音量 / 静音、或退出本局。\n"
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
