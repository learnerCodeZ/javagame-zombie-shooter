-- ============================================================
-- 迁移 v1.3：登录标识 username → phone（11 位手机号）
-- 数据库 : MySQL 8.0 / game_db
-- 特点   : 幂等（已迁移则全部跳过，可重复执行不报错）
-- 日期   : 2026-07-11
-- ------------------------------------------------------------
-- 背景：注册 / 登录改为用 11 位手机号，不再用自由用户名。本脚本把
--   user.username 列重命名为 phone（VARCHAR(11)），按 id 给每行生成
--   合法且唯一的手机号（admin=00000000000，其余顺延），并把唯一索引
--   uk_username 改名为 uk_phone。
--
-- ★ 全新安装：直接执行 sql/schema.sql（已内置 phone 列与种子），无需本脚本。
-- ★ 增量升级：从老库（v1.2 及以前、列名仍为 username）升级时执行本脚本一次。
--   迁移完成后请勿再执行 migrate_v1.1（其内 WHERE username='admin' 引用旧列名）。
-- ============================================================

USE game_db;

-- 1) 幂等开关：user 表是否还存在旧列 username
SET @has_username = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'user'
      AND column_name  = 'username'
);

-- 2) 先把旧列里的值改写成 11 位手机号（按 id-1 生成，保证唯一），
--    这样下一步把列缩短到 VARCHAR(11) 时不会因旧数据超长而报错。
--    （仅在仍处于旧结构、即第 1 次迁移时执行；已迁移则 @has_username=0 跳过）
SET @s1 = IF(@has_username = 1,
    "UPDATE user SET username = CONCAT('138', LPAD(id - 1, 8, '0'))",
    'DO 0');
PREPARE s1 FROM @s1; EXECUTE s1; DEALLOCATE PREPARE s1;

-- 2.1) admin 账号手机号统一为 00000000000（与 schema.sql 种子一致，便于记忆）
SET @s1b = IF(@has_username = 1,
    "UPDATE user SET username = '00000000000' WHERE role = 'admin'",
    'DO 0');
PREPARE s1b FROM @s1b; EXECUTE s1b; DEALLOCATE PREPARE s1b;

-- 3) 重命名列：username → phone（VARCHAR(11) NOT NULL）；其上的唯一索引随列保留
SET @s2 = IF(@has_username = 1,
    "ALTER TABLE user CHANGE COLUMN username phone VARCHAR(11) NOT NULL COMMENT '登录手机号(11位)'",
    'DO 0');
PREPARE s2 FROM @s2; EXECUTE s2; DEALLOCATE PREPARE s2;

-- 4) 唯一索引 uk_username → uk_phone（若旧索引还在）
SET @has_uk_username = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name   = 'user'
      AND index_name   = 'uk_username'
);
SET @s3 = IF(@has_uk_username = 1,
    'ALTER TABLE user DROP INDEX uk_username, ADD UNIQUE KEY uk_phone (phone)',
    'DO 0');
PREPARE s3 FROM @s3; EXECUTE s3; DEALLOCATE PREPARE s3;

-- 5) 验证：迁移后 user 表的手机号 / 角色
SELECT id, phone, nickname, role FROM user ORDER BY id;
