package com.game.ui;

import com.game.dao.ResetRequestDao;
import com.game.dao.UserDao;
import com.game.model.User;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * 登录窗口。
 * 提供用户名 / 密码输入，调用 {@link UserDao#login(String, String)} 完成登录校验；
 * 登录成功后关闭本窗并打开 {@link MainFrame}，同时提供跳转到
 * {@link RegisterFrame} 的入口。
 */
public class LoginFrame extends JFrame {

    /** 用户名输入框 */
    private final JTextField usernameField = new JTextField(15);
    /** 密码输入框 */
    private final JPasswordField passwordField = new JPasswordField(15);
    /** 登录按钮 */
    private final JButton loginButton = new JButton("登录");
    /** 跳转注册按钮 */
    private final JButton toRegisterButton = new JButton("去注册");
    /** 忘记密码按钮（提交重置申请，等管理员审核） */
    private final JButton resetButton = new JButton("忘记密码?");

    /**
     * 构造方法：初始化窗口标题、大小、布局与事件。
     */
    public LoginFrame() {
        setTitle("登录");
        setSize(420, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
        initEvents();
    }

    /**
     * 初始化界面组件：顶部标题、中间表单、底部按钮区。
     */
    private void initUI() {
        // 顶部标题
        JLabel titleLabel = new JLabel("打僵尸射击游戏", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        titleLabel.setBorder(new EmptyBorder(15, 0, 10, 0));

        // 中间表单：用户名 / 密码
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(10, 40, 10, 40));
        formPanel.add(new JLabel("用户名：", JLabel.RIGHT));
        formPanel.add(usernameField);
        formPanel.add(new JLabel("密  码：", JLabel.RIGHT));
        formPanel.add(passwordField);

        // 底部按钮区
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBorder(new EmptyBorder(0, 0, 5, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(toRegisterButton);

        // "忘记密码?" 单独一行居中，作为链接式入口
        JPanel resetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        resetPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        resetButton.setBorderPainted(false);
        resetButton.setContentAreaFilled(false);
        resetButton.setForeground(new java.awt.Color(0, 102, 204));
        resetButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        resetButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        resetPanel.add(resetButton);

        // 南区纵向叠放按钮区 + 忘记密码链接
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new javax.swing.BoxLayout(southPanel, javax.swing.BoxLayout.Y_AXIS));
        southPanel.add(buttonPanel);
        southPanel.add(resetPanel);

        add(titleLabel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * 绑定按钮事件，并把登录按钮设为回车默认按钮。
     */
    private void initEvents() {
        loginButton.addActionListener(e -> doLogin());
        toRegisterButton.addActionListener(e -> doRegister());
        resetButton.addActionListener(e -> doResetRequest());
        getRootPane().setDefaultButton(loginButton);
    }

    /**
     * 登录处理：校验非空 -> 调 UserDao 登录 -> 成功进主菜单 / 失败提示并清空密码。
     */
    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名和密码",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        User user;
        try {
            user = new UserDao().login(username, password);
        } catch (Exception ex) {
            // 数据库连不上等异常：DBUtil 已抛 RuntimeException，这里兜底成友好提示
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查 MySQL 是否已启动",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (user == null) {
            JOptionPane.showMessageDialog(this, "用户名或密码错误",
                    "登录失败", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            return;
        }
        dispose();
        new MainFrame(user).setVisible(true);
    }

    /**
     * 忘记密码处理：弹框输入用户名 -> 调 ResetRequestDao.requestReset 提交申请。
     * 用户名不存在 -> 提示"用户名不存在"；已有 pending -> 提示"已有待审核申请"；
     * 成功提交 -> 提示"已提交，等管理员审核"。DB 异常 try/catch 友好提示。
     */
    private void doResetRequest() {
        String username = JOptionPane.showInputDialog(this, "请输入您的用户名：",
                "忘记密码", JOptionPane.QUESTION_MESSAGE);
        if (username == null) {
            // 用户点了取消
            return;
        }
        username = username.trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名不能为空",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            UserDao userDao = new UserDao();
            ResetRequestDao resetDao = new ResetRequestDao();
            // 先用 findByName 区分"用户名不存在"，再调 requestReset
            if (!userDao.findByName(username)) {
                JOptionPane.showMessageDialog(this, "用户名不存在",
                        "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean ok = resetDao.requestReset(username);
            if (ok) {
                JOptionPane.showMessageDialog(this, "已提交，等管理员审核",
                        "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // 用户名存在但返回 false，说明该用户已有 pending 申请
                JOptionPane.showMessageDialog(this, "已有待审核申请",
                        "提示", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查 MySQL 是否已启动",
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 跳转注册：释放本窗并打开注册窗口。
     * RegisterFrame 在返回 / 注册成功时会 new 新的 LoginFrame，
     * 故此处必须 dispose()，否则原登录窗会被隐藏却永不释放，造成泄漏。
     */
    private void doRegister() {
        dispose();
        new RegisterFrame().setVisible(true);
    }
}
