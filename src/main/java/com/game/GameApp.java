package com.game;

import com.game.ui.LoginFrame;
import com.game.util.UIStyle;

import javax.swing.SwingUtilities;

/**
 * 程序入口。
 * 阶段②：运行 com.game.TestDao 验证数据层。
 * 阶段③起：设置系统外观，并用 SwingUtilities 打开登录界面。
 */
public class GameApp {

    public static void main(String[] args) {
        // 最先初始化全局暗色主题（现代游戏风：橙/绿主色），覆盖原系统外观设置
        UIStyle.initGlobalTheme();

        System.out.println("=== 打僵尸射击游戏 ===");
        System.out.println("阶段③：启动 Swing 登录界面。");

        // 在事件分派线程上创建并显示登录窗口
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
