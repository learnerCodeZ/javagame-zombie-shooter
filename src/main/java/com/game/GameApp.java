package com.game;

import com.game.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 程序入口。
 * 阶段②：运行 com.game.TestDao 验证数据层。
 * 阶段③起：设置系统外观，并用 SwingUtilities 打开登录界面。
 */
public class GameApp {

    public static void main(String[] args) {
        // 设置系统原生外观，让窗口风格跟随当前操作系统
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("=== 打僵尸射击游戏 ===");
        System.out.println("阶段③：启动 Swing 登录界面。");

        // 在事件分派线程上创建并显示登录窗口
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
