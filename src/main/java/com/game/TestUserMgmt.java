package com.game;

import com.game.dao.ResetRequestDao;
import com.game.dao.UserDao;
import com.game.model.PasswordResetRequest;
import com.game.model.User;

import java.util.List;

/**
 * 阶段扩展模块验收测试（无界面）：用户管理与账号安全。
 * 连真库 game_db（密码 123456）跑通：
 *   a) 注册临时用户 -> login 返回该用户且 role=user；
 *   b) changePassword -> 返回 true；用新密码能登录、旧密码不能；
 *   c) requestReset -> true；hasPending=true；listPending 含该用户；重复申请不新增；
 *   d) approve -> true；密码被重置为 123456，用 123456 能登录；
 *   e) deleteUser -> 该用户从 listAllUsers 消失；
 *   f) deleteUser(admin id=1) -> admin 仍不可删。
 *
 * 末尾打印总结；临时用户最后 deleteUser 清理，不残留垃圾；全程 try/catch 不崩。
 *
 * 运行前请确认：MySQL 已启动、game_db 已建表（含 role 列与 password_reset_request 表）。
 */
public class TestUserMgmt {

    /** 通过项计数 */
    private static int passed = 0;
    /** 失败项计数 */
    private static int failed = 0;

    public static void main(String[] args) {
        UserDao userDao = new UserDao();
        ResetRequestDao resetDao = new ResetRequestDao();
        // 临时用户 id，用于最后清理
        Integer userId = null;
        // 临时手机号：138 + 8 位随机后缀 = 11 位
        String phone = "138" + String.format("%08d", System.currentTimeMillis() % 100000000);

        try {
            System.out.println("===== 用户管理与账号安全 测试开始 =====");
            System.out.println("临时手机号：" + phone);

            // ---------- a) 注册 -> 登录 -> role=user ----------
            User regUser = new User();
            regUser.setPhone(phone);
            regUser.setPassword("123456");
            regUser.setNickname("临时测试");
            boolean regOk = userDao.register(regUser);
            System.out.println("[a1] register -> " + regOk);
            check("a) register 临时用户成功", regOk);

            User loginUser = userDao.login(phone, "123456");
            String loginRole = (loginUser == null) ? null : loginUser.getRole();
            System.out.println("[a2] login -> " + loginUser + " role=" + loginRole);
            check("a) login 返回用户且 role=user",
                    loginUser != null && "user".equals(loginRole));
            if (loginUser != null) {
                userId = loginUser.getId();
            }

            // ---------- b) changePassword ----------
            boolean b1 = userId != null && userDao.changePassword(userId, "123456", "newpwd");
            System.out.println("[b1] changePassword(123456 -> newpwd) -> " + b1);
            check("b) changePassword 返回 true", b1);

            User loginNew = (userId == null) ? null : userDao.login(phone, "newpwd");
            User loginOld = (userId == null) ? null : userDao.login(phone, "123456");
            System.out.println("[b2] login(newpwd) -> " + (loginNew != null)
                    + " ; login(123456) -> " + (loginOld != null));
            check("b) 用新密码 newpwd 能登录", loginNew != null);
            check("b) 用旧密码 123456 不能登录", loginOld == null);

            // ---------- c) requestReset / hasPending / listPending / 去重 ----------
            boolean c1 = resetDao.requestReset(phone);
            System.out.println("[c1] requestReset -> " + c1);
            check("c) requestReset 返回 true", c1);

            boolean c2 = userId != null && resetDao.hasPending(userId);
            System.out.println("[c2] hasPending -> " + c2);
            check("c) hasPending=true", c2);

            int count1 = countPendingFor(resetDao, userId);
            System.out.println("[c3] 该用户 pending 数量 -> " + count1);
            check("c) listPending 含该用户(数量==1)", count1 == 1);

            boolean c4 = resetDao.requestReset(phone);
            int count2 = countPendingFor(resetDao, userId);
            System.out.println("[c4] 再次 requestReset -> " + c4 + " ; pending 数量 -> " + count2);
            check("c) 重复申请不新增(hasPending 仍 1)", count2 == 1);

            int requestId = findPendingRequestId(resetDao, userId);
            System.out.println("[c5] 该用户 pending requestId -> " + requestId);

            // ---------- d) approve ----------
            boolean d1 = userId != null && requestId > 0
                    && resetDao.approve(requestId, userId);
            System.out.println("[d1] approve -> " + d1);
            check("d) approve 返回 true", d1);

            User loginReset = (userId == null) ? null : userDao.login(phone, "123456");
            System.out.println("[d2] login(123456 重置后) -> " + (loginReset != null));
            check("d) 用 123456 能登录(密码已重置)", loginReset != null);

            // ---------- e) deleteUser(临时用户) ----------
            boolean e1 = userId != null && userDao.deleteUser(userId);
            System.out.println("[e1] deleteUser(tmp) -> " + e1);
            check("e) deleteUser 返回 true", e1);

            boolean existsAfter = userId != null && containsUser(userDao, userId);
            System.out.println("[e2] listAllUsers 是否还含该用户 -> " + existsAfter);
            check("e) 用户已从 listAllUsers 消失", !existsAfter);

            // ---------- f) deleteUser(admin id=1) 不可删 ----------
            boolean f1 = userDao.deleteUser(1);
            boolean adminExists = containsUser(userDao, 1);
            System.out.println("[f1] deleteUser(admin id=1) -> " + f1
                    + " ; admin 是否仍在 -> " + adminExists);
            check("f) admin 不可删(返回 false)", !f1);
            check("f) admin 仍在 listAllUsers", adminExists);

        } catch (Exception ex) {
            System.out.println("测试发生异常：" + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // 清理：确保不残留垃圾（即使中途失败 / e 已删过，再删返回 false 亦无妨）
            if (userId != null) {
                try {
                    boolean cleaned = userDao.deleteUser(userId);
                    System.out.println("[清理] deleteUser(userId=" + userId + ") -> " + cleaned);
                } catch (Exception ex) {
                    System.out.println("[清理] 异常：" + ex.getMessage());
                }
            }
            System.out.println("===== 总结：通过 " + passed + " 项，失败 " + failed + " 项 =====");
            System.out.println(failed == 0 ? "全部通过" : "存在失败项");
        }
    }

    /**
     * 断言并累计通过 / 失败计数，打印单条结论。
     *
     * @param name 断言项名称
     * @param cond 条件
     */
    private static void check(String name, boolean cond) {
        if (cond) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.out.println("  [FAIL] " + name);
        }
    }

    /**
     * 统计某用户当前 pending 申请数量（用 listPending 过滤该 userId）。
     *
     * @param dao    重置申请 DAO
     * @param userId 用户ID
     * @return 该用户 pending 数量；userId 为 null 返回 -1
     */
    private static int countPendingFor(ResetRequestDao dao, Integer userId) {
        if (userId == null) {
            return -1;
        }
        int n = 0;
        for (PasswordResetRequest r : dao.listPending()) {
            if (r.getUserId() == userId) {
                n++;
            }
        }
        return n;
    }

    /**
     * 找出某用户当前 pending 申请的 requestId。
     *
     * @param dao    重置申请 DAO
     * @param userId 用户ID
     * @return requestId；不存在 / userId 为 null 返回 -1
     */
    private static int findPendingRequestId(ResetRequestDao dao, Integer userId) {
        if (userId == null) {
            return -1;
        }
        for (PasswordResetRequest r : dao.listPending()) {
            if (r.getUserId() == userId) {
                return r.getId();
            }
        }
        return -1;
    }

    /**
     * listAllUsers 中是否包含指定 id 的用户。
     *
     * @param dao    用户 DAO
     * @param userId 用户ID
     * @return true 表示存在
     */
    private static boolean containsUser(UserDao dao, int userId) {
        List<User> all = dao.listAllUsers();
        for (User u : all) {
            if (u.getId() == userId) {
                return true;
            }
        }
        return false;
    }
}
