package com.game.ui;

import com.game.dao.UserDao;
import com.game.model.User;
import com.game.util.UIStyle;

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
import java.awt.GridLayout;

/**
 * 注册窗口。
 * 收集手机号 / 密码 / 昵称，做基本合法性校验后调用
 * {@link UserDao#register(User)} 写库；注册成功后返回登录窗口，
 * 也可随时点"返回"放弃注册回到登录。
 */
public class RegisterFrame extends JFrame {

    /** 手机号输入框 */
    private final JTextField phoneField = UIStyle.phoneField();
    /** 密码输入框 */
    private final JPasswordField passwordField = UIStyle.passwordField();
    /** 昵称输入框 */
    private final JTextField nicknameField = UIStyle.field();
    /** 注册按钮 */
    private final JButton registerButton = UIStyle.primary("注册");
    /** 返回按钮 */
    private final JButton backButton = UIStyle.secondary("返回");

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
        // 暗色窗体底
        getContentPane().setBackground(UIStyle.BG);

        // 顶部标题（橙色标题）
        JLabel titleLabel = UIStyle.title("新用户注册");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setBorder(new EmptyBorder(15, 0, 10, 0));

        // 中间表单：手机号 / 密码 / 昵称（透明面板，标签右对齐）
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 12));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(10, 40, 14, 40));
        JLabel phoneLabel = UIStyle.body("手机号：");
        phoneLabel.setHorizontalAlignment(JLabel.RIGHT);
        JLabel pwdLabel = UIStyle.body("密  码：");
        pwdLabel.setHorizontalAlignment(JLabel.RIGHT);
        JLabel nickLabel = UIStyle.body("昵  称：");
        nickLabel.setHorizontalAlignment(JLabel.RIGHT);
        formPanel.add(phoneLabel);
        formPanel.add(phoneField);
        formPanel.add(pwdLabel);
        formPanel.add(passwordField);
        formPanel.add(nickLabel);
        formPanel.add(nicknameField);

        // 底部按钮区（透明）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
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
     * 注册处理：校验手机号 / 密码 / 昵称合法性 -> 调 UserDao 注册 ->
     * 成功提示并回登录窗 / 失败提示"手机号可能已存在"。
     */
    private void doRegister() {
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String nickname = nicknameField.getText().trim();
        // 合法性校验：手机号必须是 11 位数字
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "手机号不能为空",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!phone.matches("\\d{11}")) {
            JOptionPane.showMessageDialog(this, "请输入正确的 11 位手机号",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.isEmpty() || password.length() < 6) {
            JOptionPane.showMessageDialog(this, "密码不能为空且长度不少于6位",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nickname.length() > 50) {
            JOptionPane.showMessageDialog(this, "昵称不能超过 50 个字符",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 昵称留空时默认 "玩家" + 手机号后 4 位，避免界面显示裸手机号
        if (nickname.isEmpty()) {
            nickname = "玩家" + phone.substring(phone.length() - 4);
        }
        // 构造用户并写库
        User user = new User();
        user.setPhone(phone);
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
            JOptionPane.showMessageDialog(this, "注册失败(手机号可能已存在)",
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
