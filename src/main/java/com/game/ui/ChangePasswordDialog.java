package com.game.ui;

import com.game.dao.UserDao;
import com.game.model.User;
import com.game.util.UIStyle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;

/**
 * 修改密码对话框（模态）。
 * 输入旧密码 + 新密码 + 确认新密码，校验非空、两次新密码一致、新密码长度 ≥ 6 后，
 * 调用 {@link UserDao#changePassword(int, String, String)}。
 * 成功则提示并关闭；旧密码错误则提示"原密码错误"。DB 异常 try/catch 友好提示。
 */
public class ChangePasswordDialog extends JDialog {

    /** 当前登录用户 */
    private final User currentUser;
    /** 旧密码输入框 */
    private final JPasswordField oldPwdField = UIStyle.passwordField();
    /** 新密码输入框 */
    private final JPasswordField newPwdField = UIStyle.passwordField();
    /** 确认新密码输入框 */
    private final JPasswordField confirmPwdField = UIStyle.passwordField();

    /**
     * 构造方法：模态对话框，依附于主菜单窗。
     *
     * @param owner 主菜单窗
     * @param user  当前登录用户
     */
    public ChangePasswordDialog(Frame owner, User user) {
        super(owner, "修改密码", true);
        if (user == null) {
            throw new IllegalArgumentException("登录用户不能为空");
        }
        this.currentUser = user;
        setSize(400, 260);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    /**
     * 初始化界面：顶部标题、中间表单（三行密码框）、底部按钮区。
     */
    private void initUI() {
        // 内容面板 PANEL 底（卡片效果）
        getContentPane().setBackground(UIStyle.PANEL);

        // 顶部标题
        JLabel titleLabel = UIStyle.h2("修改密码");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setBorder(new EmptyBorder(12, 0, 10, 0));

        // 中间表单：三行密码框（透明面板，标签右对齐）
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 12));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(10, 30, 10, 30));
        JLabel oldLabel = UIStyle.body("旧密码：");
        oldLabel.setHorizontalAlignment(JLabel.RIGHT);
        JLabel newLabel = UIStyle.body("新密码：");
        newLabel.setHorizontalAlignment(JLabel.RIGHT);
        JLabel confirmLabel = UIStyle.body("确认新密码：");
        confirmLabel.setHorizontalAlignment(JLabel.RIGHT);
        formPanel.add(oldLabel);
        formPanel.add(oldPwdField);
        formPanel.add(newLabel);
        formPanel.add(newPwdField);
        formPanel.add(confirmLabel);
        formPanel.add(confirmPwdField);

        // 底部按钮区（透明）
        JButton okButton = UIStyle.primary("确认");
        JButton cancelButton = UIStyle.secondary("取消");
        okButton.addActionListener(e -> doSubmit());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        getRootPane().setDefaultButton(okButton);

        add(titleLabel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 提交处理：校验非空 + 两次新密码一致 + 新密码长度 ≥ 6 -> 调 DAO 修改 ->
     * 成功提示并关闭 / 旧密码错误提示。
     */
    private void doSubmit() {
        String oldPwd = new String(oldPwdField.getPassword());
        String newPwd = new String(newPwdField.getPassword());
        String confirmPwd = new String(confirmPwdField.getPassword());
        // 非空校验
        if (oldPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "三项密码都不能为空",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 两次新密码一致
        if (!newPwd.equals(confirmPwd)) {
            JOptionPane.showMessageDialog(this, "两次输入的新密码不一致",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 新密码长度 ≥ 6
        if (newPwd.length() < 6) {
            JOptionPane.showMessageDialog(this, "新密码长度不能少于6位",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 调 DAO 修改
        boolean ok;
        try {
            ok = new UserDao().changePassword(currentUser.getId(), oldPwd, newPwd);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查 MySQL 是否已启动",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (ok) {
            JOptionPane.showMessageDialog(this, "密码修改成功",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "原密码错误",
                    "修改失败", JOptionPane.ERROR_MESSAGE);
        }
    }
}
