# laundry-app 优化路线图

## 项目现状（v1.0）
- 技术栈：Kotlin + Jetpack Compose + Material3，minSdk 26 / target 34
- 包名：com.sion.laundry
- 构建：GitHub Actions（.github/workflows/build.yml），Gradle 8.7 + AGP 8.5.2 + Kotlin 1.9.24
- 产物：Actions → Artifacts → laundry-app-debug → app-debug.apk
- 存储：SharedPreferences + JSON（key: laundry_store / items）
- 提醒：WorkManager 周期任务（1天），Notifier.check() 每天最多通知一次
- 计数原理：存 startEpochDay（上次换洗日期），daysWorn = 今天 - startEpochDay，自动累加
- 代码结构：全部逻辑集中在 app/src/main/java/com/sion/laundry/MainActivity.kt
  （内含 Item / Store / Notifier / CheckWorker / MainActivity / App / ItemCard / ItemDialog）

## 已知问题
1. 只支持按天计数，鞋子/外套更适合按穿着次数
2. WorkManager 有漂移，无法指定「每天几点提醒」
3. 通知栏不能直接点「已换洗」，必须进 App
4. 归零后无历史记录，无法统计
5. 无导出/导入备份，换手机数据丢失
6. UI 简陋：系统默认图标、无深色模式、无分类图标、无统计页
7. 单文件架构，功能增多后难维护

## v1.1 界面与体验（只改 MainActivity.kt）
- [ ] 分类图标（内衣/袜子/上衣/裤子/鞋子/其他）
- [ ] 环形进度显示剩余天数
- [ ] 深色模式跟随系统
- [ ] 顶部分类筛选 chips + 搜索框
- [ ] 到期物品置顶 + 「今天有 N 件该洗」横幅
- [ ] 「全部一键换洗」
- [ ] 卡片左滑换洗 / 右滑删除
- [ ] 自定义分类管理（增删分类及其默认阈值）
- [ ] 空状态引导

## v1.2 提醒与历史（改 MainActivity.kt + AndroidManifest.xml）
- [ ] AlarmManager 精确闹钟，支持设定每日提醒时间（默认 20:00）
- [ ] 通知栏「已换洗」ActionButton，不打开 App 即可归零
- [ ] 「稍后提醒」推迟 1 天
- [ ] 阈值前 1 天黄色预警
- [ ] 计次模式：按天 / 按穿着次数 二选一
- [ ] 换洗历史记录（每次归零存一条），历史列表页
- [ ] 需要的权限：SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM、RECEIVE_BOOT_COMPLETED

## v2.0 架构与数据（拆分多文件 + app/build.gradle 加依赖）
- [ ] 拆分：data/ (Item, Room entities, DAO, Repository)、notify/、ui/ (screens, components, theme)
- [ ] SharedPreferences 迁移到 Room 数据库（含数据迁移逻辑）
- [ ] 底部导航：衣柜 / 统计 / 设置
- [ ] 统计页：本月换洗次数、勤/懒排行、日历热力图、月度柱状图
- [ ] 导出 JSON 到手机存储 / 导入恢复
- [ ] 桌面小部件 AppWidget
- [ ] 可选：衣物拍照、洗衣批次（先丢洗衣机→晾干后确认）

## 修改与发布流程
1. 仓库里点开目标文件 → 铅笔图标编辑 → 粘贴新代码 → Commit changes
2. Actions 自动触发 Build APK，等绿勾（3~5 分钟）
3. 进入该次运行 → 底部 Artifacts → 下载 laundry-app-debug（zip）
4. 解压得 app-debug.apk → 覆盖安装（包名不变，数据不丢）
5. 编译失败：看 Actions 红色日志最后 20 行定位

## 注意事项
- 改代码时勿改 applicationId，否则视为新 App，旧数据不可见
- MainActivity.kt 首行 @file:OptIn(...) 必须保留
- 版本号建议每次递增 versionCode / versionName（在 app/build.gradle）
- Actions 里的 cache / Node.js 警告可忽略，不影响产物
