# 点名 · wear-os 手表班级点名 App

给班主任用的手表小工具：**跑操点名** + **收作业** + **历史统计**。
独立 App，不依赖手机；数据全存在手表本地，可一键导出 CSV 到电脑。

适配设备：华强北 DW99（Android 8.1，368×448 方屏）；理论兼容任何 Android 8.0+ 手表（圆屏也能跑，只是布局按方屏优化）。

## 功能

| 页面 | 干什么 |
| --- | --- |
| 出勤（第 1 页） | 3 列学生宫格。**快速模式**默认全员到齐，只点没来/请假/迟到的人；**逐个模式**从“未点”开始一个一个确认。点一下循环切换 到 → 请假 → 迟到 → 缺勤 → 回到默认。 |
| 收作业（第 2 页） | 每人一格，点一下切换「交 / 未交」；「全交」一键置绿，「全未交」二次确认。 |
| 统计（第 3 页） | 近 7 天缺勤次数排行、作业未交排行、导出 CSV，以及历史记录 / 名单管理 / 设置入口。 |
| 历史 | 按日期列出每天的出勤与作业情况，可进某天看“谁没到、谁没交”。 |
| 名单 | 增删改学生、上下调学号顺序、批量粘贴导入（支持换行/逗号/顿号/空格，自动去掉序号）。 |

## 快速开始

```powershell
cd C:\Users\Administrator\Downloads\person

# 编译 Debug APK（约 1–5 分钟，视机器内存）
.\gradlew.bat :app:assembleDebug

# 安装到手表（先开 USB 调试，adb devices 能看到设备）
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.juhao.rollcall/.MainActivity
```

产物：`app\build\outputs\apk\debug\app-debug.apk`

## 名单怎么换成你班的

1. 打开 `app\src\main\assets\roster.csv`，一行写一个名字（当前是 40 个占位「同学01…同学40」）；
2. 重新编译安装：首次启动会自动导入（只有第一次，之后不会再覆盖）；
3. 也可以在手表上：统计页 → 名单管理 → 批量导入，粘贴一段名字直接替换。

> 替换名单时，同名学生会沿用原 id，历史记录不会错位。

## 数据与导出

- 全部数据在手表 `DataStore`（应用私有目录），卸载 App 会清空；
- 导出 CSV：统计页 → 「导出 CSV」，文件写到手表
  `/sdcard/Android/data/com.juhao.rollcall/files/点名记录_<时间戳>.csv`；
- 用数据线把表连电脑后：
  ```powershell
  adb pull /sdcard/Android/data/com.juhao.rollcall/files/ .\export\
  ```
- CSV 是 `日期,姓名,出勤,作业` 四列，带 UTF-8 BOM，Excel 直接打开不乱码。

## 技术栈

- Kotlin 2.4 / JDK 21 / minSdk 26 / compileSdk 37、AGP 9.4.1、Gradle 9.8
- Jetpack Compose + **Wear Compose Material3 1.7**（`AppScaffold` / `HorizontalPagerScaffold` / `ScreenScaffold` / `TransformingLazyColumn`）
- Navigation3（`NavDisplay` + `entryProvider` + 侧滑返回）
- DataStore Preferences + kotlinx.serialization（每天一条 JSON）
- 零位图：图标用 Material Symbols 矢量，图形全部 Compose 画出来

## 目录结构

```
app/src/main/java/com/juhao/rollcall/
├── MainActivity.kt          入口，持有 RollCallController
├── App.kt                   主题 + 时间胶囊 + NavDisplay 路由
├── navigation/Key.kt        NavKey 定义
├── state/
│   ├── UiState.kt           唯一界面状态（页面只读它）
│   └── RollCallController.kt 数据流合成 + 所有用户操作
├── data/
│   ├── Models.kt            Student / DayRecord / Settings / 统计模型
│   ├── RollCallStore.kt     DataStore 落盘（名单、设置、按天记录）
│   ├── AttendanceLogic.kt   点击循环、解析、计数、周统计（纯函数）
│   ├── NameListParser.kt    名单文本解析
│   ├── SeedRoster.kt        内置名单（assets/roster.csv）
│   ├── CsvExporter.kt       CSV 生成/导出
│   └── DateFormats.kt       中文日期文案
├── theme/                   强调色派生的 Material3 ColorScheme + 状态色
├── ui/
│   ├── main/HomeScreen.kt   三页 HorizontalPager
│   ├── attendance/          出勤页
│   ├── homework/            收作业页
│   ├── stats/ history/      统计、历史、某天明细
│   ├── roster/              名单列表/编辑/批量导入
│   ├── settings/            设置
│   └── components/          自绘按钮/宫格/图例等共享组件
└── utils/ScreenUtils.kt     圆方屏、常亮、变形曲线
```

## 测试

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

覆盖：名单解析、点击循环与计数、周统计窗口、CSV 生成（都是纯函数，不依赖设备）。

## 已知限制

- 输入中文需要手表自带输入法；批量导入最省事的方式还是改 `roster.csv` 重装；
- 一天只有一条记录，没有“上午/下午”分场次；
- 历史保留最近 60 天在内存里展示，CSV 导出包含全部记录。
