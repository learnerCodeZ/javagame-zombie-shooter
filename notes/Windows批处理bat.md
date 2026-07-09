# 学习笔记：Windows 批处理脚本（.bat）

> 写给：刚用 `run.bat` 跑通游戏、想搞懂 bat 文件怎么写、有没有固定套路的你
> 贯穿例子：本项目 `D:\MYCODE\Game\run.bat`（已验证可用）
> 日期：2026-07-09

---

## 一、什么是 .bat

`.bat`（batch）= **Windows 批处理脚本**，本质是一串 **CMD 命令**按顺序写进一个文本文件，**双击或在终端运行时，CMD 会逐行解释执行**。

- 就像 Linux 的 `.sh`（shell 脚本），只是 Windows 版、用 CMD 语法。
- 文件就是普通文本，改后缀为 `.bat`（或 `.cmd`）即可。
- 双击 = 用 CMD 跑；也可在终端里敲文件名跑。

---

## 二、一个 bat 的基本骨架（固定套路）

```bat
@echo off
chcp 65001 >nul
cd /d "%~dp0"

rem ====== 你的逻辑 ======
echo Hello
pause
```

这四行几乎是**每个 bat 的固定开头**，含义：

| 开头行 | 作用 |
|---|---|
| `@echo off` | **不回显**命令本身（默认 CMD 会把每条命令打印一遍，很乱；`@` 连这一行自己也不显示） |
| `chcp 65001 >nul` | 把控制台切到 **UTF-8** 编码（让中文不乱码）；`>nul` 把切换提示丢掉不显示 |
| `cd /d "%~dp0"` | 切到 **bat 自己所在的目录**（`%~dp0` = bat 文件的盘符+路径），这样不管从哪运行，相对路径都对 |
| `pause` | 结尾"按任意键继续"，**防止窗口一闪而过**看不到输出 |

> 记住这骨架，新写 bat 先抄这四行，再往中间填逻辑。

---

## 三、最常用的命令（速查表）

| 命令 | 作用 | 示例 |
|---|---|---|
| `echo 文字` | 打印文字 | `echo 编译完成` |
| `echo.` | 打印空行 | `echo.` |
| `rem ...` 或 `:: ...` | 注释 | `rem 这是注释` |
| `cd /d 路径` | 切目录（`/d` 连盘符一起切） | `cd /d d:\work` |
| `mkdir` / `md` | 建文件夹 | `mkdir out` |
| `del 文件` | 删文件 | `del *.tmp` |
| `if` | 条件判断 | `if not exist out mkdir out` |
| `pause` | 暂停等按键 | `pause` |
| `exit /b 1` | 退出脚本（`/b` 只退脚本不关 CMD），返回码 1 | `exit /b 1` |
| `>nul` | 丢弃输出（黑洞） | `chcp 65001 >nul` |
| `&` `&&` `\|\|` | 命令连接符 | `A && B`（A 成功才跑 B） |

---

## 四、变量

**定义和读取**：
```bat
set NAME=打僵尸           :: 定义
echo %NAME%               :: 读取（用 % 包起来）
```
> 注意：`set NAME=打僵尸` 等号**前后不能有空格**，否则变量名会带上空格。

**用户输入**：
```bat
set /p AGE=请输入年龄:    :: /p = prompt，把用户输入存进 AGE
echo 你的年龄是 %AGE%
```

**算术**：
```bat
set /a SUM=3+4            :: /a = arithmetic，SUM=7
```

---

## 五、延迟扩展（最重要也最易错的概念）⭐

**问题**：CMD 解析一行/一个括号块时，`%VAR%` **在解析时就替换好了**。所以下面这段**不会**如你预期工作：

```bat
set COUNT=0
for %%f in (1 2 3) do (
    set /a COUNT+=1
    echo 第 %COUNT% 次       :: ❌ 永远打印"第 0 次"！
)
```
因为 `%COUNT%` 在进入 for 块时就被替换成 `0` 了，循环里改了也没用。

**解法：延迟扩展**。开启后用 `!VAR!`（感叹号）读取，它会在**每次循环执行时**才取最新值：
```bat
setlocal enabledelayedexpansion
set COUNT=0
for %%f in (1 2 3) do (
    set /a COUNT+=1
    echo 第 !COUNT! 次       :: ✅ 正确打印 1 2 3
)
```

> 一句话：**在 `for` / `if` 的括号块里改了变量又想立刻读到新值，必须 `setlocal enabledelayedexpansion` + `!VAR!`。**

我们的 `run.bat` 拼接文件列表时就用了这个套路。

---

## 六、条件判断 `if`

```bat
if exist out\GameApp.class echo 已编译        :: 文件存在?
if not exist out mkdir out                    :: 不存在就建
if errorlevel 1 ( echo 失败 & exit /b 1 )     :: 上一条命令退出码 >=1?
if "%MODE%"=="debug" echo 调试模式            :: 字符串相等?
if "%MODE%" neq "release" echo 非正式版       :: 不等?
```

- `errorlevel N` 为真表示**退出码 ≥ N**（不是等于）。
- 字符串比较建议**两边都加引号** `"%X%"=="y"`，防变量为空时语法出错。

---

## 七、循环 `for`

bat 的 `for` 有几种变体（注意循环变量是 **两个百分号 `%%`**，命令行里直接用是一个 `%`）：

| 形式 | 作用 | 示例 |
|---|---|---|
| `for %%f in (*.txt) do ...` | 遍历当前目录匹配的文件 | `for %%f in (*.java) do echo %%f` |
| `for /r "dir" %%f in (*.java) do ...` | **递归**遍历某目录下所有匹配文件 | `run.bat` 用它收集所有 .java |
| `for /l %%i in (1,1,10) do ...` | 数字循环(起,步长,终) | 打印 1..10 |
| `for /f "tokens=1,2" %%a in (file.txt) do ...` | 逐行解析文件/命令输出 | 按列取数据 |

> `%%f` 是循环变量，代表"当前这一项"（一个文件路径/一个数字）。

---

## 八、参数与路径（写通用 bat 必备）

| 写法 | 含义 |
|---|---|
| `%0` | bat 脚本自己的路径 |
| `%1` `%2` … | 第 1、2…个**命令行参数** |
| `%~dp0` | bat 所在的**盘符+目录**（带结尾 `\`），最常用，让脚本"在哪都能跑" |
| `%~f0` | bat 的完整绝对路径 |
| `%~n0` | bat 的文件名（不含扩展名） |

例：`cd /d "%~dp0"` = 切到脚本自己所在文件夹。

---

## 九、退出码

```bat
javac ... %SRCS%
if errorlevel 1 (
    echo 编译失败
    exit /b 1          :: 退出脚本，返回码 1
)
echo 成功
exit /b 0              :: 返回码 0（成功）
```
- `exit /b N`：只结束**脚本**，返回码 N。
- 别的程序能用 `%errorlevel%` 或 `if errorlevel` 读到这个码，做"成功/失败"判断。

---

## 十、中文与编码（少踩坑）

- bat 文件里要有中文，且控制台要正常显示：开头加 `chcp 65001`（切 UTF-8），并把 bat 文件**存成 UTF-8**。
- 老系统（Win7/中文 CMD 默认 GBK）：要么 bat 存成 **ANSI(GBK)**、不加 chcp；要么存 UTF-8 + chcp 65001。**两边要配对**，否则乱码。
- bat 自己的 `echo` 中文 + 调用的程序输出中文，都可能踩这个坑。我们的 `run.bat` 干脆 echo 用英文，程序输出靠 `-Dfile.encoding=UTF-8` + chcp 65001 解决。

---

## 十一、逐行拆解我们的 `run.bat`（实战版）

```bat
@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out

echo [1/2] Compiling all sources...
setlocal enabledelayedexpansion
set "SRCS="
for /r "src\main\java" %%f in (*.java) do set "SRCS=!SRCS! %%f"
javac -encoding UTF-8 -d out -cp "lib\*" %SRCS%
if errorlevel 1 (
    echo.
    echo Compile FAILED - see errors above.
    pause
    exit /b 1
)

echo [2/2] Starting game (close the window to exit)...
java -Dfile.encoding=UTF-8 -cp "out;lib\*;src\main\resources" com.game.GameApp
pause
```

逐行翻译：

| 行 | 干啥 |
|---|---|
| `@echo off` | 不回显命令 |
| `chcp 65001 >nul` | 控制台切 UTF-8，丢弃提示 |
| `cd /d "%~dp0"` | 切到 run.bat 所在的 `D:\MYCODE\Game` |
| `if not exist out mkdir out` | 没有 `out` 文件夹就建（放编译产物） |
| `echo [1/2] ...` | 打印进度 |
| `setlocal enabledelayedexpansion` | 开延迟扩展（下面 for 里要改 SRCS） |
| `set "SRCS="` | 清空 SRCS 变量 |
| `for /r "src\main\java" %%f in (*.java) do set "SRCS=!SRCS! %%f"` | **递归**找所有 `.java`，一个个用 `!SRCS!`（延迟扩展）拼成一长串路径 |
| `javac ... %SRCS%` | 用拼好的文件列表编译（`%SRCS%` 在循环外，普通读取即可） |
| `if errorlevel 1 ( ... )` | javac 失败（退出码≥1）→ 提示 + pause + `exit /b 1` |
| `echo [2/2] ...` | 打印进度 |
| `java -Dfile.encoding=UTF-8 -cp "out;lib\*;src\main\resources" com.game.GameApp` | 运行：classpath 含 `out`(编译产物)+`lib\*`(驱动jar)+`src\main\resources`(db.properties)，主类 `com.game.GameApp` |
| `pause` | 跑完留窗，看输出 |

> 这就是一个**"编译 + 运行 Java 项目"的完整 bat 套路**：固定开头 → 准备目录 → 编译（收集文件用 for+延迟扩展）→ 判错 → 运行 → pause。

---

## 十二、常见坑 & 调试技巧

| 坑 | 解法 |
|---|---|
| 窗口一闪而过 | 结尾加 `pause` |
| for/if 里变量不更新 | `setlocal enabledelayedexpansion` + `!VAR!` |
| 变量值带尾随空格 | `set` 等号前后**不留空格**，或用 `set "VAR=value"`（带引号最稳） |
| 中文乱码 | `chcp 65001` + 文件存 UTF-8 |
| 路径含空格报错 | 路径**加引号** `"C:\my folder"` |
| 从别的目录跑，相对路径错 | 开头 `cd /d "%~dp0"` |
| 调试看不到哪行出错 | 临时把 `@echo off` 改成 `@echo on`，能看见每条命令 |
| 双引号、`&` `<` `>` 等特殊字符 | 用 `^` 转义，如 `^&` |

**调试万能法**：在终端里**手动**跑 bat（不要双击），这样窗口不关，能看完整输出和报错。

---

## 十三、一句话总结

> **.bat 是一串 CMD 命令的文本，CMD 逐行执行。固定套路：`@echo off` + `chcp 65001` + `cd /d "%~dp0"` 开头，逻辑写中间，`pause` 结尾。最易错的是 for/if 里读变量要用延迟扩展 `!VAR!`。**

把 `run.bat` 当模板，以后写 Java/其它项目的"一键编译运行"脚本，照着改路径和主类就行。
