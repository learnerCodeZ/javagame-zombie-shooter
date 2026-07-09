-- ============================================================
-- 迁移脚本: 为"用户管理与账号安全"功能升级数据库 (无损, 可重复执行)
-- 作用: 给 user 表加 role 列; 新建 password_reset_request 表
-- 日期: 2026-07-09
-- ============================================================
USE game_db;

-- 1) user 表加 role 列(若已存在则跳过, 保证可重复执行)
SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = 'game_db' AND table_name = 'user' AND column_name = 'role');
SET @ddl := IF(@col = 0,
               'ALTER TABLE user ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT ''user'' COMMENT ''角色 admin/user''',
               'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) admin 账号置为管理员角色(其余保持默认 user)
UPDATE user SET role = 'admin' WHERE username = 'admin';

-- 3) 新建密码重置申请表(若不存在)
CREATE TABLE IF NOT EXISTS password_reset_request (
    id           INT      NOT NULL AUTO_INCREMENT,
    user_id      INT      NOT NULL,
    status       VARCHAR(10) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    request_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handle_time  DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_reset_user (user_id),
    KEY idx_reset_status (status),
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置申请表';

-- 4) 验证
SELECT username, role FROM user;
SHOW TABLES;
