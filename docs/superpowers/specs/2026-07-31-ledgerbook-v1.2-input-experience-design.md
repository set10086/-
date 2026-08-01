# LedgerBook Lite V1.2 Input Experience Design

## Goal

修复 Android 15 边到边布局下顶部内容与状态栏重叠的问题，并将记账表单升级为适合手机操作的结构化选择体验。

## Scope

V1.2 保留现有 SQLite 数据结构、应用 ID 与固定侧载签名，不迁移或清空 V1.1 数据。改动集中在输入组件和记账表单展示层。

## System-bar handling

根布局监听 `WindowInsets`。顶部容器增加状态栏 inset，底部导航增加系统导航栏 inset。不得使用固定状态栏高度；横竖屏和刘海屏都使用系统回传值。

## Amount calculator

金额字段不再直接唤起系统键盘。点击金额或优惠金额后打开四列计算键盘，提供数字、小数点、退格、清空及加减乘除。表达式按乘除优先计算，使用 `BigDecimal`，结果四舍五入为两位小数。除零、非法表达式、非正数结果不得提交。

## Category picker

支出、收入分别使用预置的带图标分类网格；转账固定为账户转账。自定义分类通过文本输入补充。选择结果同时展示图标和名称。

## Account picker

账户选择器展示图标、账户名、账户类型和当前余额，并仅列出当前账本账户。转账必须选择不同的转出、转入账户。

## Date-time picker

使用 `NumberPicker` 提供年、月、日、小时、分钟滚轮。月份或年份变化时自动修正日期上限，覆盖闰年和大小月。

## Bookkeeper picker

内置本人、家人、伴侣、孩子，同时读取历史自定义记账人。新增自定义姓名后持久化到 SharedPreferences，后续直接可选。

## Error handling

所有组件在对话框内给出明确 Toast 或字段提示，不允许因为空值、除零、无账户、相同转账账户或非法时间崩溃。

## Testing

纯 Java 单元测试覆盖：运算优先级、小数、除零、分类映射、闰年天数、记账人去重。CI 必须依次通过单元测试、Android 编译、APK v2 签名验证和产物哈希生成。

## Release

版本号升级为 `1.2.0` / versionCode 3，产物名 `LedgerBook-Lite-v1.2.0.apk`。继续使用 V1.1 固定签名证书，可覆盖安装 V1.1。
