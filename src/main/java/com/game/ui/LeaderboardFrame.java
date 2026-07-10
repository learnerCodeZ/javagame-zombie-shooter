package com.game.ui;

import com.game.dao.RecordDao;
import com.game.model.GameRecord;
import com.game.model.User;
import com.game.util.UIStyle;

import javax.swing.JButton;
import javax.swing.JFrame;
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
 * 排行榜窗口。
 * 展示全局战绩 Top50 或当前用户的个人记录，由顶部按钮切换。
 * 作为独立窗叠加在主菜单之上，关闭后回到主菜单（不重建 MainFrame）。
 */
public class LeaderboardFrame extends JFrame {

    /** 当前登录用户（用于「我的记录」取昵称与 userId） */
    private final User currentUser;

    /** 表格模型，列固定为：排名 / 昵称 / 分数 / 击杀 / 存活(秒) / 时间 */
    private final DefaultTableModel tableModel;

    /** 时间格式化器 */
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 构造方法：初始化窗口并默认加载一次全局榜。
     *
     * @param currentUser 当前登录用户
     */
    public LeaderboardFrame(User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("登录用户不能为空");
        }
        this.currentUser = currentUser;

        setTitle("排行榜");
        setSize(720, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // 暗色窗体底
        getContentPane().setBackground(UIStyle.BG);

        // 列名常量；表格只读：重写 isCellEditable 永远返回 false
        String[] columns = {"排名", "昵称", "分数", "击杀", "存活(秒)", "时间"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        // 暗色表格 + 暗色滚动面板（行高 / 表头 / 网格 / 选中色由 UIStyle 统一）
        UIStyle.table(table);
        JScrollPane scrollPane = new JScrollPane(table);
        UIStyle.scrollPane(scrollPane);

        // 顶部按钮：全局榜 / 我的记录 → 主色橙；返回 → 次色绿
        JButton globalButton = UIStyle.primary("全局榜");
        JButton mineButton = UIStyle.primary("我的记录");
        JButton backButton = UIStyle.secondary("返回");

        globalButton.addActionListener(e -> loadGlobal());
        mineButton.addActionListener(e -> loadMine());
        backButton.addActionListener(e -> dispose());

        // 顶部操作条：暗色面板 + 统一留白
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        UIStyle.apply(topPanel);
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        topPanel.add(globalButton);
        topPanel.add(mineButton);
        topPanel.add(backButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // 构造时默认加载全局榜
        loadGlobal();
    }

    /**
     * 加载全局排行榜：取分数前 50 名，排名列填 1..N，昵称取 JOIN 得到的昵称。
     */
    private void loadGlobal() {
        List<GameRecord> list = new RecordDao().topN(50);
        clearRows();
        int rank = 1;
        for (GameRecord r : list) {
            tableModel.addRow(new Object[]{
                    rank++,
                    r.getNickname(),
                    r.getScore(),
                    r.getKillCount(),
                    r.getSurviveSec(),
                    formatTime(r.getRecordTime())
            });
        }
    }

    /**
     * 加载当前用户的个人记录：昵称统一用 currentUser 的昵称。
     */
    private void loadMine() {
        List<GameRecord> list = new RecordDao().mine(currentUser.getId());
        // 昵称为空时回退到用户名，与主菜单标题行为一致
        String nick = currentUser.getNickname();
        if (nick == null || nick.isEmpty()) {
            nick = currentUser.getUsername();
        }
        clearRows();
        int rank = 1;
        for (GameRecord r : list) {
            tableModel.addRow(new Object[]{
                    rank++,
                    nick,
                    r.getScore(),
                    r.getKillCount(),
                    r.getSurviveSec(),
                    formatTime(r.getRecordTime())
            });
        }
    }

    /**
     * 清空表格所有行。
     */
    private void clearRows() {
        tableModel.setRowCount(0);
    }

    /**
     * 格式化时间，null 返回空串。
     *
     * @param date 记录时间
     * @return 格式化字符串
     */
    private String formatTime(java.util.Date date) {
        return date == null ? "" : timeFmt.format(date);
    }
}
