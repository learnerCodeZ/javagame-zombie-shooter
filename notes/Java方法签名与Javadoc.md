# Java 方法签名(method signature)

> 一句话:**方法签名 = 方法名 + 参数列表(参数的类型、个数、顺序)**。它是编译器识别"这是哪个方法"的身份证。

## 一、什么算签名 / 什么不算

| 算签名的一部分 | **不**算 |
|---|---|
| 方法名 | 返回类型(`int` / `String` / `void` …) |
| 参数的**类型** | 参数的**名字**(`phone` vs `a`) |
| 参数的**个数** | 访问修饰符(`public` / `private`) |
| 参数的**顺序** | `static` 修饰符、抛出的异常 |

以本项目 [`UserDao.login`](../src/main/java/com/game/dao/UserDao.java) 为例:

```java
public User login(String phone, String password) { ... }
```

- 签名 = `login(String, String)`
- 返回类型 `User` **不算** → 就算改成 `void login(...)`,签名还是 `login(String, String)`
- 参数名 `phone`/`password` **不算** → 改成 `login(String a, String b)` 签名不变

## 二、为什么重要:决定"调用哪个方法"

编译器靠签名匹配调用。两个方法**同名但签名不同** = **重载(overload)**,合法;编译器按你传的参数类型/个数挑对应那个。

### 实例 ① 重载——本项目的两个 `GameController` 构造器

[`GameController`](../src/main/java/com/game/game/GameController.java):

```java
public GameController() {                     // 签名:GameController()
    this(Difficulty.EASY);                     // 委托给下面那个
}
public GameController(Difficulty difficulty) { // 签名:GameController(Difficulty)
    ...
}
```

- 同名 `GameController`,参数列表不同 → 两个不同签名 → 合法重载。
- `new GameController()` → 编译器看"没传参" → 匹配 `GameController()`。
- `new GameController(Difficulty.HARD)` → 传了一个 `Difficulty` → 匹配 `GameController(Difficulty)`。

[`Zombie`](../src/main/java/com/game/game/Zombie.java) 同理有两个构造器:`Zombie(double,double,double,double,double)`(普通)和 `Zombie(Type,double,double,double,double,double)`(带类型),靠签名区分——这就是**多态/重载**在项目里的真实落地。

### 实例 ② 改签名会"断"调用方——`RecordDao.mine`(刚踩过的真坑)

`mine` 原来签名是 `mine(int, String)`:

```java
public List<GameRecord> mine(int userId, String difficulty) { ... }   // 签名:mine(int, String)
```

后来(做"我的记录=全部战绩"那次)改成不分难度,签名变成 `mine(int)`:

```java
public List<GameRecord> mine(int userId) { ... }   // 签名:mine(int)
```

**签名变了 = 这成了一个不同的方法**。于是 [`TestDao`](../src/main/java/com/game/TestDao.java) 里原来的调用:

```java
recordDao.mine(loginUser.getId(), "EASY");   // 传两个参数 → 找 mine(int, String)
```

在新签名下**匹配不上**(已经没有 `mine(int, String)` 这个方法了)→ **编译报错**。所以改完签名,所有调用方都得跟着改:

```java
recordDao.mine(loginUser.getId());           // 改传一个参数 → 匹配 mine(int)
```

> **教训**:改方法签名(尤其改参数列表)是**破坏性改动**,会连累所有调用方。改之前先 `grep` 调用点,改完全量 `javac` 验证。这次就是靠编译器揪出 `TestDao` 那一处没同步。

## 三、反例:只改返回类型,不算新签名(不构成重载)

```java
int    foo(int x) { ... }
String foo(int x) { ... }   // ❌ 编译错:和上面签名相同(都是 foo(int)),重复方法
```

返回类型不在签名里,所以这俩编译器**分不开** → 报"method already defined"。要重载就得改**参数列表**,光改返回类型不行。

## 四、项目里更多签名一览(对照看)

| 方法 | 签名 | 说明 |
|---|---|---|
| `UserDao.login(String, String)` | `login(String, String)` | 手机号+密码 |
| `UserDao.findByPhone(String)` | `findByPhone(String)` | 查重 |
| `UserDao.changePassword(int, String, String)` | `changePassword(int, String, String)` | userId+旧密+新密 |
| `RecordDao.topN(int, String)` | `topN(int, String)` | 前 N 名+难度 |
| `RecordDao.mine(int)` | `mine(int)` | 某用户全部记录(改后) |
| `ResetRequestDao.approve(int, int)` | `approve(int, int)` | 申请ID+用户ID |
| `GameController()` / `GameController(Difficulty)` | 两个签名 | 重载构造器 |

## 五、小结

- **签名 = 方法名 + 参数列表(类型/个数/顺序)**;不含返回类型、参数名、修饰符。
- **重载(overload)** = 同名、签名不同;编译器按调用时传的参数匹配。
- **改签名 = 破坏性改动**,调用方要同步;改返回类型/参数名**不算**改签名,不影响调用方。
- 答辩一句话:「方法签名是方法名加参数列表,编译器靠它区分重载方法;我项目里 `GameController`/`Zombie` 的构造器重载就是靠不同签名实现的,改 `RecordDao.mine` 签名时也得同步所有调用方。」

---

*相关代码:[`UserDao`](../src/main/java/com/game/dao/UserDao.java)、[`RecordDao`](../src/main/java/com/game/dao/RecordDao.java)、[`GameController`](../src/main/java/com/game/game/GameController.java)、[`Zombie`](../src/main/java/com/game/game/Zombie.java)、[`TestDao`](../src/main/java/com/game/TestDao.java)。*

---

## 六、附:`@Override` / `@param` / `@return` / `@code` / `@link` 是什么 + 作用

这些 `@xxx` 经常在代码里出现,但**分两类**,别混。

### `@Override` —— 是「注解」(annotation),不是文档标签

- **是什么**:Java **注解**(annotation),写在方法**上方那行**(`@interface` 定义)。
- **作用**:声明"我重写了父类/接口的方法",**编译器会检查**——如果你没真正重写(方法名/签名写错),编译报错。等于让编译器帮你核对"这确实是 override"。
- **影响**:影响**编译**(是代码的一部分),不只是注释。

```java
@Override                       // 注解:声明重写;写错方法名/签名,编译器会报错
public void update() { ... }
```

### `@param` / `@return` / `@code` / `@link` —— 是 Javadoc「文档标签」

这四个都写在 `/** ... */` **文档注释里**,作用是**生成文档**(API 说明)时格式化输出。**不影响编译、不影响运行**,只给人看 / 给 `javadoc` 工具生成 HTML。

| 标签 | 写在哪 | 是什么 + 作用 | 例子 |
|---|---|---|---|
| `@param 名字 说明` | 方法注释 | **参数说明**:描述一个参数的含义 | `@param phone 手机号` |
| `@return 说明` | 方法注释 | **返回值说明**:描述返回什么 | `@return true 表示成功` |
| `{@code 代码}` | 行内 | **代码格式**:把内容当代码显示(等宽字体、不当 HTML 解析),用来包代码片段 | `使用 {@code List<GameObject>}` |
| `{@link 类#方法}` | 行内 | **超链接**:生成指向另一个类/方法的链接 | `见 {@link User#getPhone()}` |

例子(项目里 `UserDao.login` 的注释):

```java
/**
 * 登录:按手机号 + 密码校验。
 * @param phone    手机号          ← 参数说明
 * @param password 明文密码         ← 参数说明
 * @return 校验通过返回 User;失败返回 null   ← 返回值说明
 */
public User login(String phone, String password) { ... }
```

### 一句话记

- **`@Override`** = Java **注解**(方法上方、影响编译、= 重写声明,让编译器帮你核对)。
- **`@param / @return / @code / @link`** = Javadoc **文档标签**(`/** */` 里、只管生成文档,不影响运行);`@param`/`@return` 描述方法契约,`@code`/`@link` 是行内格式化标签。

> 两者都带 `@`,但**注解是给编译器看的,文档标签是给人/工具看的**。
