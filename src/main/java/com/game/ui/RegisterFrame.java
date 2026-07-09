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
 * 注册窗口。
 * 收集用户名 / 密码 / 昵称，做基本合法性校验后调用
 * {@link UserDao#register(User)} 写库；注册成功后返回登录窗口，
 * 也可随时点"返回"放弃注册回到登录。
 */
public class RegisterFrame extends JFrame {

    /** 用户名输入框 */
    private final JTextField usernameField = new JTextField(15);
    /** 密码输入框 */
    private final JPasswordField passwordField = new JPasswordField(15);
    /** 昵称输入框 */
    private final JTextField nicknameField = new JTextField(15);
    /** 注册按钮 */
    private final JButton registerButton = new JButton("注册");
    /** 返回按钮 */
    private final JButton backButton = new JButton("返回");

    /**
     * 构造方法：初始化窗口标题、大小、布局与事件。
     */
    public RegisterFrame() {
        setTitle("注册");
        setSize(420, 360);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        initEvents();
    }

    /**
     * 初始化界面组件：顶部标题、中间表单、底部按钮区。
     */
    private void initUI() {
        // 顶部标题
        JLabel titleLabel = new JLabel("新用户注册", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        titleLabel.setBorder(new EmptyBorder(15, 0, 10, 0));

        // 中间表单：用户名 / 密码 / 昵称
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(10, 40, 10, 40));
        formPanel.add(new JLabel("用户名：", JLabel.RIGHT));
        formPanel.add(usernameField);
        formPanel.add(new JLabel("密  码：", JLabel.RIGHT));
        formPanel.add(passwordField);
        formPanel.add(new JLabel("昵  称：", JLabel.RIGHT));
        formPanel.add(nicknameField);

        // 底部按钮区
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);

        add(titleLabel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 绑定按钮事件，并把注册按钮设为回车默认按钮。
     */
    private void initEvents() {
        registerButton.addActionListener(e -> doRegister());
        backButton.addActionListener(e -> doBack());
        getRootPane().setDefaultButton(registerButton);
    }

    /**
     * 注册处理：校验用户名 / 密码合法性 -> 调 UserDao 注册 ->
     * 成功提示并回登录窗 / 失败提示"用户名可能已存在"。
     */
    private void doRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String nickname = nicknameField.getText().trim();
        // 合法性校验
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名不能为空",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.isEmpty() || password.length() < 6) {
            JOptionPane.showMessageDialog(this, "密码不能为空且长度不少于6位",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 昵称留空时默认使用用户名，避免数据库昵称列非空时报错
        if (nickname.isEmpty()) {
            nickname = username;
        }
        // 构造用户并写库
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        boolean ok;
        try {
            ok = new UserDao().register(user);
        } catch (Exception ex) {
            // 数据库连不上等异常：兜底成友好提示，不让界面崩
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查 MySQL 是否已启动",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (ok) {
            JOptionPane.showMessageDialog(this, "注册成功",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new LoginFrame().setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "注册失败(用户名可能已存在)",
                    "注册失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 返回处理：关闭本窗并回到登录窗口。
     */
    private void doBack() {
        dispose();
        new LoginFrame().setVisible(true);
    }
}
