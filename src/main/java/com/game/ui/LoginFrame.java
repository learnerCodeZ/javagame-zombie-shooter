package com.game.ui;

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
        buttonPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(toRegisterButton);

        add(titleLabel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 绑定按钮事件，并把登录按钮设为回车默认按钮。
     */
    private void initEvents() {
        loginButton.addActionListener(e -> doLogin());
        toRegisterButton.addActionListener(e -> doRegister());
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
     * 跳转注册：释放本窗并打开注册窗口。
     * RegisterFrame 在返回 / 注册成功时会 new 新的 LoginFrame，
     * 故此处必须 dispose()，否则原登录窗会被隐藏却永不释放，造成泄漏。
     */
    private void doRegister() {
        dispose();
        new RegisterFrame().setVisible(true);
    }
}
