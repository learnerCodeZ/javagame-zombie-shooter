package com.game.ui;

import com.game.dao.RecordDao;
import com.game.game.Difficulty;
import com.game.model.GameRecord;
import com.game.model.User;
import com.game.util.UIStyle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 排行榜窗口。
 * <p>按<strong>难度</strong>分两个榜:「简单榜」「困难榜」,各自只看该难度的战绩、
 * 一律按分数倒序。「我的记录」是在当前难度榜上叠加的"只看自己"过滤(可开关)。
 * <p>按钮:简单榜 / 困难榜(切难度 → 显示该难度全部玩家)· 我的记录(切「只看自己」)· 返回。
 * <p>作为独立窗叠加在主菜单之上,关闭后回到主菜单(不重建 MainFrame)。
 */
public class LeaderboardFrame extends JFrame {

    /** 当前登录用户(用于「我的记录」取昵称与 userId) */
    private final User currentUser;

    /** 表格模型,列固定为:排名 / 昵称 / 分数 / 击杀 / 存活(秒) / 时间 */
    private final DefaultTableModel tableModel;

    /** 时间格式化器 */
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** 当前查看的难度(默认简单) */
    private Difficulty currentDifficulty = Difficulty.EASY;
    /** 是否「我的记录」视图(true=只看自己 / false=该难度全部玩家) */
    private boolean mineMode = false;
    /** 顶部当前视图标签(文字+颜色随难度/范围变化) */
    private JLabel viewLabel;

    /**
     * 构造方法:初始化窗口并默认加载一次「简单榜」。
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

        // 列名常量;表格只读:重写 isCellEditable 永远返回 false
        String[] columns = {"排名", "昵称", "分数", "击杀", "存活(秒)", "时间"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        // 暗色表格 + 暗色滚动面板(行高 / 表头 / 网格 / 选中色由 UIStyle 统一)
        UIStyle.table(table);
        JScrollPane scrollPane = new JScrollPane(table);
        UIStyle.scrollPane(scrollPane);

        // 难度切换:简单榜→次色绿、困难榜→危险红(对齐游戏内 HUD 的难度配色);
        // 点难度按钮 = 看该难度的全部玩家(同时退出「我的记录」过滤)
        JButton easyButton = UIStyle.secondary("简单榜");
        JButton hardButton = UIStyle.danger("困难榜");
        // 「我的记录」:在当前难度榜上只看自己 → 主色橙(再次点击可退出)
        JButton mineButton = UIStyle.primary("我的记录");
        // 返回 → 次色绿
        JButton backButton = UIStyle.secondary("返回");

        easyButton.addActionListener(e -> { currentDifficulty = Difficulty.EASY; mineMode = false; reload(); });
        hardButton.addActionListener(e -> { currentDifficulty = Difficulty.HARD; mineMode = false; reload(); });
        mineButton.addActionListener(e -> { mineMode = !mineMode; reload(); });
        backButton.addActionListener(e -> dispose());

        // 顶部:第一行当前视图标签,第二行操作按钮(纵向 BoxLayout)
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        UIStyle.apply(topPanel);
        topPanel.setBorder(new EmptyBorder(8, 10, 4, 10));

        viewLabel = UIStyle.body("");
        viewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(viewLabel);
        topPanel.add(Box.createVerticalStrut(6));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        UIStyle.apply(btnRow);
        btnRow.add(easyButton);
        btnRow.add(hardButton);
        btnRow.add(mineButton);
        btnRow.add(backButton);
        topPanel.add(btnRow);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // 构造时默认加载「简单榜」
        reload();
    }

    /**
     * 按 {@link #currentDifficulty} 与 {@link #mineMode} 重新查询并刷新表格,
     * 同时更新顶部视图标签(文字+颜色)。
     */
    private void reload() {
        RecordDao dao = new RecordDao();
        List<GameRecord> list;
        // 「我的记录」昵称统一用当前用户的昵称;难度榜(全部玩家)用 JOIN 得到的每条昵称
        String mineNick = null;
        if (mineMode) {
            list = dao.mine(currentUser.getId(), currentDifficulty.name());
            mineNick = currentUser.getNickname();
            if (mineNick == null || mineNick.isEmpty()) {
                mineNick = currentUser.getPhone();
            }
        } else {
            list = dao.topN(50, currentDifficulty.name());
        }
        clearRows();
        int rank = 1;
        for (GameRecord r : list) {
            String nick = mineMode ? mineNick : r.getNickname();
            tableModel.addRow(new Object[]{
                    rank++,
                    nick,
                    r.getScore(),
                    r.getKillCount(),
                    r.getSurviveSec(),
                    formatTime(r.getRecordTime())
            });
        }
        updateViewLabel();
    }

    /**
     * 更新顶部视图标签:文字「当前:难度榜」(+「 · 我的记录」),颜色随难度(简单绿 / 困难红)。
     */
    private void updateViewLabel() {
        String suffix = mineMode ? " · 我的记录" : "";
        viewLabel.setText("当前:" + currentDifficulty.label + "榜" + suffix);
        viewLabel.setForeground(currentDifficulty == Difficulty.HARD
                ? new Color(235, 90, 90)
                : new Color(90, 200, 120));
    }

    /**
     * 清空表格所有行。
     */
    private void clearRows() {
        tableModel.setRowCount(0);
    }

    /**
     * 格式化时间,null 返回空串。
     *
     * @param date 记录时间
     * @return 格式化字符串
     */
    private String formatTime(java.util.Date date) {
        return date == null ? "" : timeFmt.format(date);
    }
}
