package com.game;

import com.game.dao.RecordDao;
import com.game.dao.UserDao;
import com.game.model.GameRecord;
import com.game.model.User;

import java.util.List;

/**
 * 阶段②数据层验收测试：在控制台跑通
 *   注册 -> 登录 -> 存战绩 -> 全局榜 -> 我的记录 -> 边界(错密码)。
 *
 * 运行前请确认：
 *   1) 已执行 sql/schema.sql，建好 game_db 库和表；
 *   2) db.properties 里的 password 已改成你本机 root 密码；
 *   3) MySQL 已启动。
 */
public class TestDao {

    public static void main(String[] args) {
        UserDao userDao = new UserDao();
        RecordDao recordDao = new RecordDao();

        // 1) 注册一个新用户（手机号带随机后缀，方便反复测试；138 + 8 位 = 11 位）
        String phone = "138" + String.format("%08d", System.currentTimeMillis() % 100000000);
        User newUser = new User();
        newUser.setPhone(phone);
        newUser.setPassword("123456");
        newUser.setNickname("测试玩家");
        boolean regOk = userDao.register(newUser);
        System.out.println("【注册】" + phone + " / 123456 -> "
                + (regOk ? "成功" : "失败(可能手机号已存在)"));

        // 2) 登录
        User loginUser = userDao.login(phone, "123456");
        System.out.println("【登录】" + (loginUser != null ? "成功 " + loginUser : "失败"));
        if (loginUser == null) {
            System.out.println("登录失败，后续步骤终止。");
            return;
        }

        // 3) 存两条战绩
        GameRecord r1 = new GameRecord();
        r1.setUserId(loginUser.getId());
        r1.setScore(150);
        r1.setKillCount(15);
        r1.setSurviveSec(120);
        GameRecord r2 = new GameRecord();
        r2.setUserId(loginUser.getId());
        r2.setScore(300);
        r2.setKillCount(30);
        r2.setSurviveSec(200);
        System.out.println("【存战绩】r1=" + recordDao.saveRecord(r1)
                + ", r2=" + recordDao.saveRecord(r2));

        // 4) 全局榜前 10
        System.out.println("【全局榜 Top10】");
        List<GameRecord> top = recordDao.topN(10, "EASY");
        for (int i = 0; i < top.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + top.get(i));
        }

        // 5) 我的记录
        System.out.println("【我的记录】");
        for (GameRecord r : recordDao.mine(loginUser.getId(), "EASY")) {
            System.out.println("  " + r);
        }

        // 6) 边界：错误密码登录应返回 null
        User badLogin = userDao.login(phone, "wrongpwd");
        System.out.println("【边界】错误密码登录应失败 -> "
                + (badLogin == null ? "符合预期(null)" : "异常(竟然登录成功)"));
    }
}
