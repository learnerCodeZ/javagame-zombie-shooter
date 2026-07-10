package com.game.ui;

import com.game.dao.ResetRequestDao;
import com.game.dao.UserDao;
import com.game.model.PasswordResetRequest;
import com.game.model.User;
import com.game.util.UIStyle;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 管理员用户管理窗口（仅 admin 进入）。
 * 上方 JTable 列出所有用户(id / 用户名 / 昵称 / 角色 / 注册时间)；
 * 中间 JTable 列出待审核重置申请(用户名 / 昵称 / 申请时间)；
 * 按钮：通过 / 拒绝 / 删除用户 / 刷新 / 返回。操作后刷新对应表格。
 *
 * 说明：本窗 dispose 后只是关闭管理窗，调用方的 MainFrame 仍保持开着。
 */
public class AdminFrame extends JFrame {

    /** 日期格式化：展示注册时间 / 申请时间 */
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** 当前登录的管理员（仅用于校验身份） */
    private final User currentUser;
    private final UserDao userDao = new UserDao();
    private final ResetRequestDao resetDao = new ResetRequestDao();

    /** 用户表模型（不可编辑） */
    private final DefaultTableModel userModel = new DefaultTableModel(
            new Object[][]{}, new Object[]{"ID", "用户名", "昵称", "角色", "注册时间"}) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    /** 申请表模型（不可编辑） */
    private final DefaultTableModel requestModel = new DefaultTableModel(
            new Object[][]{}, new Object[]{"用户名", "昵称", "申请时间"}) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    /** 用户表组件 */
    private final JTable userTable = new JTable(userModel);
    /** 申请表组件 */
    private final JTable requestTable = new JTable(requestModel);

    /** 当前加载的用户列表（与用户表行一一对应） */
    private List<User> users;
    /** 当前加载的待审核申请列表（与申请表行一一对应） */
    private List<PasswordResetRequest> pending;

    /**
     * 构造方法。
     *
     * @param user 当前登录用户（应为 admin）
     */
    public AdminFrame(User user) {
        if (user == null) {
            throw new IllegalArgumentException("登录用户不能为空");
        }
        this.currentUser = user;
        setTitle("用户管理 - " + currentUser.getUsername());
        setSize(720, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        // 首次加载数据
        doRefresh();
    }

    /**
     * 初始化界面：上用户表、中申请表、下按钮区。
     */
    private void initUI() {
        // 暗色窗体底
        getContentPane().setBackground(UIStyle.BG);

        // —— 上方：所有用户表 ——
        JLabel userLabel = UIStyle.h2("所有用户");
        userLabel.setBorder(new EmptyBorder(8, 10, 4, 0));
        JScrollPane userScroll = new JScrollPane(userTable);
        // 暗色表格 + 暗色滚动面板（行高 / 表头 / 网格 / 选中色由 UIStyle 统一）
        UIStyle.table(userTable);
        UIStyle.scrollPane(userScroll);

        // —— 中间：待审核申请表 ——
        JLabel reqLabel = UIStyle.h2("待审核重置申请");
        reqLabel.setBorder(new EmptyBorder(8, 10, 4, 0));
        JScrollPane reqScroll = new JScrollPane(requestTable);
        UIStyle.table(requestTable);
        UIStyle.scrollPane(reqScroll);

        // 两张表纵向叠放，各占一半
        JPanel tablePanel = new JPanel(new java.awt.GridLayout(4, 1, 4, 4));
        UIStyle.apply(tablePanel);
        tablePanel.setBorder(new EmptyBorder(6, 10, 6, 10));
        tablePanel.add(userLabel);
        tablePanel.add(userScroll);
        tablePanel.add(reqLabel);
        tablePanel.add(reqScroll);

        // —— 下方：按钮区（按语义分配主色） ——
        // 通过 → 主色橙；拒绝 / 刷新 / 返回 → 次色绿；删除用户 → 危险红
        JButton approveButton = UIStyle.primary("通过");
        JButton rejectButton = UIStyle.secondary("拒绝");
        JButton deleteButton = UIStyle.danger("删除用户");
        JButton refreshButton = UIStyle.secondary("刷新");
        JButton backButton = UIStyle.secondary("返回");
        approveButton.addActionListener(e -> doApprove());
        rejectButton.addActionListener(e -> doReject());
        deleteButton.addActionListener(e -> doDeleteUser());
        refreshButton.addActionListener(e -> doRefresh());
        backButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        UIStyle.apply(buttonPanel);
        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(backButton);

        add(tablePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 加载所有用户到用户表。
     */
    private void loadUsers() {
        userModel.setRowCount(0);
        try {
            users = userDao.listAllUsers();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查 MySQL 是否已启动",
                    "错误", JOptionPane.ERROR_MESSAGE);
            users = java.util.Collections.emptyList();
        }
        for (User u : users) {
            userModel.addRow(new Object[]{
                    u.getId(),
                    u.getUsername(),
                    u.getNickname(),
                    u.getRole(),
                    u.getCreateTime() == null ? "" : DATE_FMT.format(u.getCreateTime())
            });
        }
    }

    /**
     * 加载待审核申请到申请表。
     */
    private void loadPending() {
        requestModel.setRowCount(0);
        try {
            pending = resetDao.listPending();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查 MySQL 是否已启动",
                    "错误", JOptionPane.ERROR_MESSAGE);
            pending = java.util.Collections.emptyList();
        }
        for (PasswordResetRequest r : pending) {
            requestModel.addRow(new Object[]{
                    r.getUsername(),
                    r.getNickname(),
                    r.getRequestTime() == null ? "" : DATE_FMT.format(r.getRequestTime())
            });
        }
    }

    /**
     * 通过选中的申请：重置该用户密码为 123456。操作后刷新。
     */
    private void doApprove() {
        PasswordResetRequest req = selectedRequest();
        if (req == null) {
            JOptionPane.showMessageDialog(this, "请先选中一条待审核申请",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean ok;
        try {
            ok = resetDao.approve(req.getId(), req.getUserId());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查 MySQL 是否已启动",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (ok) {
            JOptionPane.showMessageDialog(this,
                    "已通过，" + req.getUsername() + " 的密码已重置为 123456",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "该账号不可重置（admin），申请已置为拒绝",
                    "提示", JOptionPane.WARNING_MESSAGE);
        }
        doRefresh();
    }

    /**
     * 拒绝选中的申请。操作后刷新。
     */
    private void doReject() {
        PasswordResetRequest req = selectedRequest();
        if (req == null) {
            JOptionPane.showMessageDialog(this, "请先选中一条待审核申请",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean ok;
        try {
            ok = resetDao.reject(req.getId());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查 MySQL 是否已启动",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (ok) {
            JOptionPane.showMessageDialog(this, "已拒绝该申请",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "拒绝失败",
                    "提示", JOptionPane.WARNING_MESSAGE);
        }
        doRefresh();
    }

    /**
     * 删除选中的用户；admin 不可删。操作后刷新。
     */
    private void doDeleteUser() {
        User u = selectedUser();
        if (u == null) {
            JOptionPane.showMessageDialog(this, "请先选中一个用户",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 二次确认
        int choice = JOptionPane.showConfirmDialog(this,
                "确认删除用户 " + u.getUsername() + " ？\n（将一并删除其游戏记录与重置申请）",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        // admin 不可删（DAO 层也有保护，这里先给出友好提示）
        if ("admin".equals(u.getRole())) {
            JOptionPane.showMessageDialog(this, "管理员账号不可删除",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean ok;
        try {
            ok = userDao.deleteUser(u.getId());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查 MySQL 是否已启动",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (ok) {
            JOptionPane.showMessageDialog(this, "已删除用户 " + u.getUsername(),
                    "提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "删除失败（可能该用户不可删）",
                    "提示", JOptionPane.WARNING_MESSAGE);
        }
        doRefresh();
    }

    /**
     * 刷新两张表。
     */
    private void doRefresh() {
        loadUsers();
        loadPending();
    }

    /**
     * 取得申请表中当前选中行对应的申请对象；未选中返回 null。
     *
     * @return 选中的申请，未选中返回 null
     */
    private PasswordResetRequest selectedRequest() {
        int row = requestTable.getSelectedRow();
        if (row < 0 || pending == null || row >= pending.size()) {
            return null;
        }
        return pending.get(row);
    }

    /**
     * 取得用户表中当前选中行对应的用户对象；未选中返回 null。
     *
     * @return 选中的用户，未选中返回 null
     */
    private User selectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0 || users == null || row >= users.size()) {
            return null;
        }
        return users.get(row);
    }
}
