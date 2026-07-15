package com.game.util;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 全局 UI 主题工具类（现代游戏风暗色主题）。
 * <p>统一所有 Swing 界面外观：暗色底 + 橙/绿主色，贴合"橙猫打绿僵尸"的题材。
 * <p>颜色与字体常量全公开，方便各界面直接引用；同时提供按钮 / 输入框 / 标签 /
 * 表格 / 滚动面板等工厂方法，保证全站样式一致。
 * <p>使用流程：{@link com.game.GameApp#main} 启动时先调一次
 * {@link #initGlobalTheme()}，之后各界面按需调用 {@link #primary(String)} 等方法。
 */
public final class UIStyle {

    private UIStyle() {
        // 工具类，不实例化
    }

    // ============================ 配色（锁定） ============================

    /** 窗体底色（最深一档，做背景留白） */
    public static final Color BG = new Color(0x16181f);
    /** 卡片 / 面板底色 */
    public static final Color PANEL = new Color(0x232732);
    /** 输入框 / 表格行底色 */
    public static final Color FIELD = new Color(0x2c313c);
    /** 边框色（输入框 / 表格网格 / 滚动面板边框） */
    public static final Color BORDER = new Color(0x3a4150);
    /** 主色橙：主按钮 / 标题（橙猫） */
    public static final Color PRIMARY = new Color(0xff8a1e);
    /** 主色橙 hover（锁定） */
    public static final Color PRIMARY_HOVER = new Color(0xffa84d);
    /** 次色绿：次按钮 / 强调（绿僵尸） */
    public static final Color SECONDARY = new Color(0x3dd578);
    /** 次色绿 hover */
    public static final Color SECONDARY_HOVER = SECONDARY.brighter();
    /** 危险红：删除 / 退出 */
    public static final Color DANGER = new Color(0xef4f4f);
    /** 危险红 hover */
    public static final Color DANGER_HOVER = DANGER.brighter();
    /** 主文字色 */
    public static final Color TEXT = new Color(0xf1f2f6);
    /** 次要文字色 */
    public static final Color MUTED = new Color(0x9aa1ad);
    /** 选中色：PRIMARY 叠在暗底上的近似半透明橙（实色，避免 JTable 重绘残影） */
    public static final Color SELECTION = new Color(0x8b592e);

    // ============================ 字体（微软雅黑） ============================

    /** 标题（20 bold） */
    public static final Font TITLE = new Font("微软雅黑", Font.BOLD, 20);
    /** 二级标题（16 bold） */
    public static final Font H2 = new Font("微软雅黑", Font.BOLD, 16);
    /** 正文（14） */
    public static final Font BODY = new Font("微软雅黑", Font.PLAIN, 14);
    /** 按钮文字（15 bold） */
    public static final Font BUTTON = new Font("微软雅黑", Font.BOLD, 15);
    /** 小字（12） */
    public static final Font SMALL = new Font("微软雅黑", Font.PLAIN, 12);
    /** 表头字体（14 bold，内部用） */
    private static final Font HEADER_FONT = new Font("微软雅黑", Font.BOLD, 14);

    /** 圆角半径：按钮 */
    private static final int ARC_BUTTON = 12;
    /** 圆角半径：输入框 */
    private static final int ARC_FIELD = 8;

    // ============================ 全局主题 ============================

    /**
     * 初始化全局暗色主题。在 {@code GameApp.main} 最前面调一次。
     * <p>选用跨平台 Look&amp;Feel（Metal），确保 {@code UIManager.put} 的暗色取值
     * 被 JOptionPane / JTable 等组件稳定采纳；随后统一写入暗色默认值。
     */
    public static void initGlobalTheme() {
        try {
            // 跨平台 L&F 最稳定地遵循下面的 UIManager 颜色覆盖
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ---- 通用底色 / 文字 ----
        UIManager.put("Panel.background", PANEL);
        UIManager.put("Panel.foreground", TEXT);
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("OptionPane.foreground", TEXT);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", BUTTON);
        UIManager.put("Label.background", PANEL);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", BODY);

        // ---- 按钮（JOptionPane 等内部的 L&F 按钮，靠此自动暗色） ----
        UIManager.put("Button.background", PANEL);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.select", PANEL.brighter());
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("Button.font", BUTTON);

        // ---- 文本输入 ----
        UIManager.put("TextField.background", FIELD);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextField.selectionBackground", SELECTION);
        UIManager.put("TextField.selectionForeground", TEXT);
        UIManager.put("TextField.font", BODY);
        UIManager.put("PasswordField.background", FIELD);
        UIManager.put("PasswordField.foreground", TEXT);
        UIManager.put("PasswordField.caretForeground", TEXT);
        UIManager.put("PasswordField.selectionBackground", SELECTION);
        UIManager.put("PasswordField.selectionForeground", TEXT);
        UIManager.put("PasswordField.font", BODY);

        // ---- 表格 ----
        UIManager.put("Table.background", FIELD);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.font", BODY);
        UIManager.put("Table.rowHeight", 24);
        UIManager.put("TableHeader.background", PANEL);
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("TableHeader.font", HEADER_FONT);
        UIManager.put("TableHeader.cellBorder", new EmptyBorder(6, 8, 6, 8));

        // ---- 滚动面板 / 滚动条 ----
        UIManager.put("ScrollPane.background", FIELD);
        UIManager.put("ScrollPane.foreground", TEXT);
        UIManager.put("Viewport.background", FIELD);
        UIManager.put("ScrollBar.background", FIELD);
        UIManager.put("ScrollBar.foreground", BORDER);
        UIManager.put("ScrollBar.track", FIELD);
        UIManager.put("ScrollBar.thumb", BORDER);
        UIManager.put("ScrollBar.thumbHighlight", BORDER.brighter());
        UIManager.put("ScrollBar.thumbDarkShadow", BG);
        UIManager.put("ScrollBar.width", 12);

        // ---- 复选框 / 单选框文字 ----
        UIManager.put("CheckBox.background", PANEL);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("CheckBox.font", BODY);
        UIManager.put("RadioButton.background", PANEL);
        UIManager.put("RadioButton.foreground", TEXT);
        UIManager.put("RadioButton.font", BODY);

        // ---- 组合框 ----
        UIManager.put("ComboBox.background", FIELD);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", SELECTION);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("ComboBox.font", BODY);
    }

    // ============================ 按钮 ============================

    /**
     * 创建主操作按钮（橙底白字，扁平圆角，hover 变亮）。
     *
     * @param text 按钮文字
     * @return 配置好的主按钮
     */
    public static JButton primary(String text) {
        return new GameButton(text, PRIMARY, PRIMARY_HOVER);
    }

    /**
     * 创建次操作按钮（绿底白字，扁平圆角，hover 变亮）。
     *
     * @param text 按钮文字
     * @return 配置好的次按钮
     */
    public static JButton secondary(String text) {
        return new GameButton(text, SECONDARY, SECONDARY_HOVER);
    }

    /**
     * 创建危险操作按钮（红底白字，扁平圆角，hover 变亮），用于删除 / 退出。
     *
     * @param text 按钮文字
     * @return 配置好的危险按钮
     */
    public static JButton danger(String text) {
        return new GameButton(text, DANGER, DANGER_HOVER);
    }

    /**
     * 扁平圆角按钮：自定义 {@code fillRoundRect} 画底色 + hover 换色 + 白字 + 按钮字体。
     */
    private static final class GameButton extends JButton {
        private static final long serialVersionUID = 1L;//目的：消除编译器警告,作用：序列化版本号

        /** 常态底色 */
        private final Color normalColor;
        /** 鼠标悬停底色 */
        private final Color hoverColor;
        /** 当前是否处于悬停态 */
        private boolean hover = false;

        /**
         * 构造方法。
         *
         * @param text        按钮文字
         * @param normalColor 常态底色
         * @param hoverColor  悬停底色
         */
        GameButton(String text, Color normalColor, Color hoverColor) {
            super(text);
            this.normalColor = normalColor;
            this.hoverColor = hoverColor;
            setFont(BUTTON);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // 内边距留白，按钮更饱满
            setBorder(new EmptyBorder(9, 22, 9, 22));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? hoverColor : normalColor);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        ARC_BUTTON, ARC_BUTTON);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    // ============================ 面板 ============================

    /**
     * 把面板套成卡片样式：不透明 + PANEL 底色。
     *
     * @param panel 面板
     */
    public static void apply(JPanel panel) {
        panel.setOpaque(true);
        panel.setBackground(PANEL);
    }

    // ============================ 输入框 ============================

    /**
     * 创建暗色圆角文本框（FIELD 底 + BORDER 圆角边框 + TEXT 字 + BODY 字体）。
     *
     * @return 配置好的文本框
     */
    public static JTextField field() {
        return new GameField();
    }

    /**
     * 创建手机号输入框：在 {@link #field()} 样式基础上，用 {@link DocumentFilter} 限制
     * <b>只能输入数字、最多 11 位</b>（输满 11 位即无法继续输入，粘贴超长/非数字会被丢弃）。
     *
     * @return 配置好的手机号输入框（11 位纯数字）
     */
    public static JTextField phoneField() {
        JTextField f = field();
        if (f.getDocument() instanceof PlainDocument doc) {
            doc.setDocumentFilter(new DocumentFilter() {
                /** 插入后总长度是否 ≤ 11 且新增文本全是数字（空串放行，用于删除）。 */
                private boolean acceptable(int curLen, int removedLen, String inserted) {
                    if (inserted == null || inserted.isEmpty()) {
                        return true;
                    }
                    if (!inserted.matches("\\d*")) {
                        return false;
                    }
                    return curLen - removedLen + inserted.length() <= 11;
                }

                @Override
                public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                        throws BadLocationException {
                    if (acceptable(fb.getDocument().getLength(), 0, text)) {
                        super.insertString(fb, offset, text, attr);
                    }
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attr)
                        throws BadLocationException {
                    if (acceptable(fb.getDocument().getLength(), length, text)) {
                        super.replace(fb, offset, length, text, attr);
                    }
                }
            });
        }
        return f;
    }

    /**
     * 创建暗色圆角密码框（样式同 {@link #field()}）。
     *
     * @return 配置好的密码框
     */
    public static JPasswordField passwordField() {
        return new GamePasswordField();
    }

    /**
     * 给文本框统一套暗色圆角样式：圆角底 + 圆角边框 + 内边距 + 光标 / 选中色。
     *
     * @param f 文本框（含密码框）
     */
    private static void styleField(JTextField f) {
        f.setBackground(FIELD);
        f.setForeground(TEXT);
        f.setFont(BODY);
        f.setCaretColor(TEXT);
        f.setSelectionColor(SELECTION);
        f.setSelectedTextColor(TEXT);
        f.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
    }

    /**
     * 圆角暗色文本框：opaque=false 后自行 fillRoundRect 画底色。
     */
    private static class GameField extends JTextField {
        private static final long serialVersionUID = 1L;

        GameField() {
            setOpaque(false);
            styleField(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        ARC_FIELD, ARC_FIELD);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * 圆角暗色密码框（与 {@link GameField} 共用样式与绘制逻辑）。
     */
    private static class GamePasswordField extends JPasswordField {
        private static final long serialVersionUID = 1L;

        GamePasswordField() {
            setOpaque(false);
            styleField(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        ARC_FIELD, ARC_FIELD);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    // ============================ 标签 ============================

    /**
     * 标题标签（TITLE 字体 + PRIMARY 橙）。
     *
     * @param text 文本
     * @return 标题标签
     */
    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE);
        label.setForeground(PRIMARY);
        return label;
    }

    /**
     * 二级标题标签（H2 字体 + TEXT 字）。
     *
     * @param text 文本
     * @return 二级标题标签
     */
    public static JLabel h2(String text) {
        JLabel label = new JLabel(text);
        label.setFont(H2);
        label.setForeground(TEXT);
        return label;
    }

    /**
     * 正文标签（BODY 字体 + TEXT 字）。
     *
     * @param text 文本
     * @return 正文标签
     */
    public static JLabel body(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY);
        label.setForeground(TEXT);
        return label;
    }

    /**
     * 次要文字标签（SMALL 字体 + MUTED 字）。
     *
     * @param text 文本
     * @return 次要文字标签
     */
    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SMALL);
        label.setForeground(MUTED);
        return label;
    }

    // ============================ 表格 / 滚动面板 ============================

    /**
     * 把表格套成暗色风格：FIELD 行底 + TEXT 字 + BORDER 网格 + 暗底表头 + 不可拖动表头 + 行高 24。
     *
     * @param t 表格
     */
    public static void table(JTable t) {
        t.setBackground(FIELD);
        t.setForeground(TEXT);
        t.setSelectionBackground(SELECTION);
        t.setSelectionForeground(TEXT);
        t.setGridColor(BORDER);
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setRowHeight(24);
        t.setFont(BODY);
        t.setFillsViewportHeight(true);

        JTableHeader header = t.getTableHeader();
        header.setBackground(PANEL);
        header.setForeground(TEXT);
        header.setFont(HEADER_FONT);
        // 表头不可拖动（可仍允许调整列宽）
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        // 表头不透明，确保 PANEL 底色生效
        header.setOpaque(true);
    }

    /**
     * 把滚动面板套成暗色风格：暗色 viewport + BORDER 边框 + 暗色滚动条。
     *
     * @param s 滚动面板
     */
    public static void scrollPane(JScrollPane s) {
        s.setOpaque(true);
        s.setBackground(FIELD);
        s.setBorder(new LineBorder(BORDER, 1));
        s.getViewport().setOpaque(true);
        s.getViewport().setBackground(FIELD);

        styleScrollBar(s.getVerticalScrollBar());
        styleScrollBar(s.getHorizontalScrollBar());

        // 表头一角也统一暗色
        Component corner = s.getColumnHeader();
        if (corner instanceof JComponent jc) {
            jc.setBackground(PANEL);
            jc.setOpaque(true);
        }
    }

    /**
     * 暗色化单根滚动条。
     *
     * @param bar 滚动条
     */
    private static void styleScrollBar(JScrollBar bar) {
        if (bar == null) {
            return;
        }
        bar.setBackground(FIELD);
        bar.setForeground(BORDER);
        bar.setOpaque(true);
    }
}
