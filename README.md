# Notify Ledger

一个本地优先的 Android 自动记账原型。它通过 Android 通知访问权限读取支付通知，解析消费金额、商户、来源 App，并写入手机本地 SQLite 数据库。

## 当前功能

- 通知监听：使用 `NotificationListenerService` 监听系统通知。
- 消费解析：从常见支付、付款、消费、扣款通知中提取金额。
- 去重入库：同一来源、同一分钟、同金额、同文本的通知不会重复写入。
- 本地账单：SQLite 保存最近账单，不上传任何数据。
- 首页展示：显示本月合计、权限状态、最近 100 条账单。
- 测试入口：可以写入一条测试账单，方便验证界面和数据库链路。

## 打开项目

1. 用 Android Studio 打开当前目录。
2. 等待 Gradle Sync 完成。
3. 连接 Android 手机或启动模拟器。
4. 运行 `app`。

当前工作环境没有 Java、Gradle 和 Android SDK，所以我无法在这里直接编译 APK；源码已经按标准 Android Gradle 项目组织。

## 使用方式

1. 安装并打开 App。
2. 点击“开启通知访问”。
3. 在系统设置里允许 `Notify Ledger 通知记账`。
4. 回到 App，状态显示“通知访问已开启”后，新的消费通知会自动尝试记账。

## 重要说明

这个 App 不读取微信、支付宝、银行 App 的私有数据库，也不抓包。它只处理系统通知里已经展示给你的文本，因此安全性和可维护性更好，但解析准确率取决于具体通知格式。

后续建议你先安装到自己的手机，收集几条真实通知文本，然后把样例加入 `NotificationParser` 的规则里。这样会比一次性猜所有支付 App 格式更稳。

## 主要文件

- `app/src/main/java/com/example/notifyledger/MainActivity.java`：首页、权限入口、账单列表。
- `app/src/main/java/com/example/notifyledger/NotificationLedgerService.java`：系统通知监听。
- `app/src/main/java/com/example/notifyledger/NotificationParser.java`：通知文本解析规则。
- `app/src/main/java/com/example/notifyledger/LedgerDatabase.java`：SQLite 存储。
