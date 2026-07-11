package com.game.ui;

import com.game.dao.ResetRequestDao;
import com.game.dao.UserDao;
import com.game.model.User;
import com.game.util.UIStyle;

import javax.swing.BoxLayout;
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
 * 登录窗口。
 * 提供手机号 / 密码输入，调用 {@link UserDao#login(String, String)} 完成登录校验；
 * 登录成功后关闭本窗并打开 {@link MainFrame}，同时提供跳转到
 * {@link RegisterFrame} 的入口。
 */
public class LoginFrame extends JFrame {

    /** 手机号输入框 */
    private final JTextField phoneField = UIStyle.phoneField();
    /** 密码输入框 */
    private final JPasswordField passwordField = UIStyle.passwordField();
    /** 登录按钮 */
    private final JButton loginButton = UIStyle.primary("登录");
    /** 跳转注册按钮 */
    private final JButton toRegisterButton = UIStyle.secondary("去注册");
    /** 忘记密码按钮（提交重置申请，等管理员审核） */
    private final JButton resetButton = UIStyle.secondary("忘记密码?");

    /**
     * 构造方法：初始化窗口标题、大小、布局与事件。
     */
    public LoginFrame() {
        setTitle("登录");
        setSize(420, 340);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
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
        JLabel titleLabel = UIStyle.title("打僵尸射击游戏");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setBorder(new EmptyBorder(20, 0, 14, 0));

        // 中间表单：手机号 / 密码（透明面板，标签右对齐）
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 12));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(10, 40, 14, 40));
        JLabel phoneLabel = UIStyle.body("手机号：");
        phoneLabel.setHorizontalAlignment(JLabel.RIGHT);
        JLabel pwdLabel = UIStyle.body("密  码：");
        pwdLabel.setHorizontalAlignment(JLabel.RIGHT);
        formPanel.add(phoneLabel);
        formPanel.add(phoneField);
        formPanel.add(pwdLabel);
        formPanel.add(passwordField);

        // 底部按钮区（透明）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(0, 0, 4, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(toRegisterButton);

        // "忘记密码?" 单独一行居中
        JPanel resetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        resetPanel.setOpaque(false);
        resetPanel.setBorder(new EmptyBorder(0, 0, 18, 0));
        resetPanel.add(resetButton);

        // 南区纵向叠放按钮区 + 忘记密码链接
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.setOpaque(false);
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
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (phone.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入手机号和密码",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!phone.matches("\\d{11}")) {
            JOptionPane.showMessageDialog(this, "手机号格式不正确（应为 11 位手机号）",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        User user;
        try {
            user = new UserDao().login(phone, password);
        } catch (Exception ex) {
            // 数据库连不上等异常：DBUtil 已抛 RuntimeException，这里兜底成友好提示
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查 MySQL 是否已启动",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (user == null) {
            JOptionPane.showMessageDialog(this, "手机号或密码错误",
                    "登录失败", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            return;
        }
        dispose();
        new MainFrame(user).setVisible(true);
    }

    /**
     * 忘记密码处理：弹框输入手机号 -> 调 ResetRequestDao.requestReset 提交申请。
     * 手机号格式不对 -> 提示"手机号格式不正确"；手机号不存在 -> 提示"手机号不存在"；
     * 已有 pending -> 提示"已有待审核申请"；成功提交 -> 提示"已提交，等管理员审核"。
     * DB 异常 try/catch 友好提示。
     */
    private void doResetRequest() {
        String phone = JOptionPane.showInputDialog(this, "请输入您的手机号：",
                "忘记密码", JOptionPane.QUESTION_MESSAGE);
        if (phone == null) {
            // 用户点了取消
            return;
        }
        phone = phone.trim();
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "手机号不能为空",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!phone.matches("\\d{11}")) {
            JOptionPane.showMessageDialog(this, "手机号格式不正确（应为 11 位手机号）",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            UserDao userDao = new UserDao();
            ResetRequestDao resetDao = new ResetRequestDao();
            // 先用 findByPhone 区分"手机号不存在"，再调 requestReset
            if (!userDao.findByPhone(phone)) {
                JOptionPane.showMessageDialog(this, "手机号不存在",
                        "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean ok = resetDao.requestReset(phone);
            if (ok) {
                JOptionPane.showMessageDialog(this, "已提交，等管理员审核",
                        "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // 手机号存在但返回 false，说明该用户已有 pending 申请
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
