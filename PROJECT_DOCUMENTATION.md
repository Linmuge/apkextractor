# 牧歌App工具箱 (APKExtractor) 项目文档

## 项目概述

**牧歌App工具箱**（原 APKExtractor）是一个 Android 应用程序，用于快速查询、导出（提取）本地已安装的应用。项目采用 Kotlin 开发，使用 Material Design 3 设计语言，支持现代 Android 开发最佳实践。

| 项目属性 | 值 |
|---------|-----|
| 应用ID | `info.muge.appshare` |
| 当前版本 | 5.0.1 (versionCode: 364) |
| 最低SDK | API 24 (Android 7.0) |
| 目标SDK | API 36 |
| 编译SDK | API 36 |

---

## 项目结构

```
apkextractor/
├── app/                              # 主应用模块
│   ├── src/main/
│   │   ├── java/info/muge/appshare/
│   │   │   ├── activities/           # Activity 类
│   │   │   ├── fragments/            # Fragment 类
│   │   │   ├── adapters/             # 适配器类
│   │   │   ├── items/                # 数据模型类
│   │   │   ├── tasks/                # 异步任务类
│   │   │   ├── ui/                   # UI 组件类
│   │   │   ├── utils/                # 工具类
│   │   │   ├── base/                 # 基础类
│   │   │   ├── Global.kt             # 全局工具类
│   │   │   ├── Constants.kt          # 常量定义
│   │   │   └── MyApplication.kt      # 应用类
│   │   ├── res/                      # 资源文件
│   │   └── AndroidManifest.xml       # 应用清单
│   └── build.gradle.kts              # 应用构建脚本
├── build.gradle.kts                  # 项目级构建脚本
└── README.md                         # 项目说明
```

---

## 技术栈

### 核心框架

| 库名 | 版本 | 用途 |
|-----|------|-----|
| AndroidX AppCompat | 1.7.1 | 向后兼容支持 |
| Material Design | 1.13.0 | Material Design 3 组件 |
| RecyclerView | 1.4.0 | 列表显示 |
| ViewPager2 | 1.1.0 | 页面滑动 |
| ConstraintLayout | 2.2.1 | 灵活布局 |
| SwipeRefreshLayout | 1.2.0 | 下拉刷新 |

### 第三方库

| 库名 | 版本 | 用途 |
|-----|------|-----|
| BRV | 1.6.1 | RecyclerView 框架 |
| Pinyin4j | 2.5.1 | 中文拼音转换 |
| XXPermissions | 26.5 | 权限管理 |

### 构建配置

| 配置项 | 值 |
|-------|-----|
| Kotlin | 2.2.20 |
| Java | 21 |
| ViewBinding | 启用 |
| DataBinding | 启用 |

---

## 核心模块详解

### 1. 数据模型层 (`items/`)

#### AppItem.kt
应用数据模型，包含应用的所有信息：

- **属性**: 程序名、图标、包名、版本号、大小、安装来源等
- **排序功能**: 支持10种排序方式（名称、大小、更新日期、安装日期、包名等）
- **构造函数**:
  - `constructor(context, info)`: 从 PackageInfo 创建
  - `constructor(wrapper, flag_data, flag_obb)`: 创建副本用于导出

#### FileItem.kt
文件数据模型，表示单个导出文件

#### ImportItem.kt
导入项数据模型

### 2. 业务逻辑层 (`tasks/`)

#### ExportTask.kt - 导出任务
核心导出逻辑，继承自 `Thread`：

```kotlin
class ExportTask(
    private val context: Context,
    private val list: List<AppItem>,
    private var listener: ExportProgressListener?
) : Thread()
```

**功能**:
- 导出单个 APK 文件
- 导出 ZIP 压缩包（包含 Data/Obb 目录）
- 实时进度回调
- 支持中断和恢复
- 速度计算和显示

**导出流程**:
1. 初始化输出路径（内部存储/外部存储）
2. 遍历待导出应用列表
3. 对于每个应用：
   - 仅 APK: 直接复制文件
   - 包含 Data/Obb: 创建 ZIP 压缩包
4. 更新媒体库
5. 发送广播刷新列表

#### RefreshInstalledListTask.kt
刷新已安装应用列表的任务

#### SearchAppItemTask.kt
应用搜索任务

#### ImportTask.kt
导入应用任务

### 3. UI 层

#### activities/ - Activity 类

| Activity | 功能 |
|----------|------|
| `LaunchActivity` | 启动页 |
| `MainActivity` | 主界面，包含搜索、ViewPager2、菜单 |
| `AppDetailActivity` | 应用详情页 |
| `SettingActivity` | 设置页 |

**MainActivity.kt** 核心功能:
- Edge-to-Edge 全屏显示
- ViewPager2 + TabLayout 页面切换
- SearchView 搜索功能
- 多选模式管理
- 返回键处理

#### fragments/ - Fragment 类

**AppFragment.kt** - 应用列表页核心逻辑:
- RecyclerView 展示应用列表
- SwipeRefreshLayout 下拉刷新
- 多选操作
- 搜索模式切换
- 滚动监听（隐藏/显示操作卡片）
- BroadcastReceiver 监听应用安装/卸载

#### adapters/ - 适配器类

| 适配器 | 功能 |
|--------|------|
| `AppListAdapter` | 应用列表适配器，使用 BRV 框架 |
| `MyPagerAdapter` | ViewPager 适配器 |
| `MyViewPager2Adapter` | ViewPager2 适配器 |

#### ui/ - UI 组件类

| 组件 | 功能 |
|------|------|
| `UpdateDialog` | 更新对话框（Material3 风格） |
| `ExportingDialog` | 导出进度对话框 |
| `ImportingDialog` | 导入进度对话框 |
| `DataObbDialog` | Data/Obb 选择对话框 |
| `AppItemSortConfigDialog` | 应用排序配置对话框 |
| `ExportRuleDialog` | 导出规则对话框 |
| `ToastManager` | Toast 管理 |
| `ProgressDialog` | 进度对话框 |

### 4. 工具类层 (`utils/`)

| 工具类 | 功能 |
|--------|------|
| `FileUtil` | 文件操作工具 |
| `StorageUtil` | 存储工具 |
| `SPUtil` | SharedPreferences 工具 |
| `PermissionExts` | 权限扩展函数 |
| `ThemeUtil` | 主题工具（夜间模式、动态颜色） |
| `EnvironmentUtil` | 环境工具 |
| `OutputUtil` | 输出路径工具 |
| `DocumentFileUtil` | DocumentFile 工具 |
| `ZipFileUtil` | ZIP 文件工具 |

### 5. 基础类层 (`base/`)

| 基础类 | 功能 |
|--------|------|
| `BaseActivity<VB>` | Activity 基类，支持 ViewBinding |
| `BaseFragment<VB>` | Fragment 基类，支持 ViewBinding |
| `MainChildFragment` | 主页面 Fragment 接口 |

---

## 全局对象

### Global.kt
全局工具对象，提供以下功能：

| 方法 | 功能 |
|------|------|
| `checkAndExportCertainAppItemsToSetPathWithoutShare()` | 检查并导出应用 |
| `shareCertainAppsByItems()` | 分享应用 |
| `showCheckingDuplicationDialogAndStartImporting()` | 显示查重对话框并启动导入 |
| `shareCertainFiles()` | 分享文件 |
| `shareThisApp()` | 分享本应用 |

**全局变量**:
- `handler`: 主线程 Handler
- `app_list`: 已安装应用列表（线程安全）
- `item_list`: 导出目录文件列表（线程安全）

### Constants.kt
常量定义，包含：

| 类型 | 常量 |
|------|------|
| SharedPreferences 键 | 保存路径、文件名格式、压缩级别、视图模式等 |
| 默认值 | 各种配置的默认值 |
| 文件名格式变量 | `?A` 序号、`?N` 应用名、`?P` 包名等 |
| 排序配置 | 0-10 种排序方式 |
| 分享模式 | 直接分享、提取后分享 |

---

## 应用功能特性

### 核心功能

1. **应用导出**
   - 导出 APK 文件
   - 导出 ZIP 压缩包（含 Data/Obb）
   - 自定义保存路径
   - 自定义文件名格式
   - 重复文件检测
   - 导出进度显示

2. **应用管理**
   - 查看已安装应用列表
   - 按多种方式排序
   - 搜索应用
   - 查看应用详情
   - 复制包名

3. **视图模式**
   - 列表模式
   - 网格模式
   - 切换排序方式

4. **设置选项**
   - 夜间模式（跟随系统/日间/夜间）
   - 动态颜色（Android 12+）
   - 语言切换（中文/英文）
   - 导出路径配置
   - 文件名格式配置
   - ZIP 压缩级别

### 权限说明

| 权限 | 用途 |
|------|------|
| `QUERY_ALL_PACKAGES` | 查询所有已安装应用 |
| `GET_INSTALLED_APPS` | 获取已安装应用列表 |

---

## 架构设计

### 分层架构

```
┌─────────────────────────────────────┐
│           UI Layer                  │
│  (Activities, Fragments, Dialogs)   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Business Logic              │
│     (Tasks, Adapters, Global)       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          Data Layer                 │
│    (Items: AppItem, FileItem)       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          Utils Layer                │
│  (FileUtil, StorageUtil, SPUtil...) │
└─────────────────────────────────────┘
```

### 设计模式

| 模式 | 应用场景 |
|------|----------|
| 单例模式 | Global、Constants |
| 工厂模式 | 适配器创建 |
| 观察者模式 | 广播接收器、进度回调 |
| 构建器模式 | AlertDialog、MaterialDialog |
| 模板方法模式 | BaseActivity、BaseFragment |

---

## 最近的代码变更

根据 Git 状态，以下文件有未提交的修改：

| 文件 | 变更内容 |
|------|----------|
| `app/build.gradle.kts` | 依赖版本更新 |
| `UpdateDialog.kt` | 更新对话框修改 |
| `dialog_update.xml` | 更新对话框布局 |
| `styles.xml` | 样式适配 Material3 |
| `build.gradle.kts` | 项目构建配置 |
| `bg_update_dialog_md3.xml` | 新增更新对话框背景 |

### 最近的提交历史

- `b803be5` - 更新依赖版本，修改应用名称为"牧歌App工具箱"
- `4204796` - 优化 ViewBinding 反射并完善混淆规则
- `054ffbf` - 适配 Material3，优化导出规则对话框界面
- `9830a1e` - 重构：使用 M3 风格重构设置页面并新增 SwitchView 组件
- `8ec4e6e` - 重构并适配 Material 3 UI

---

## 构建配置

### build.gradle.kts 关键配置

```kotlin
android {
    compileSdk = 36
    defaultConfig {
        applicationId = "info.muge.appshare"
        minSdk = 24
        targetSdk = 36
        versionCode = 364
        versionName = "5.0.1"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}
```

### ProGuard 规则

项目包含完整的 ProGuard 混淆规则，保留：
- ViewBinding 生成的类
- 泛型和反射相关类
- 自定义 View

---

## 国际化支持

### 支持语言
- 跟随系统
- 简体中文
- English

### 语言设置
通过 `Constants.PREFERENCE_LANGUAGE` 配置，值为：
- `LANGUAGE_FOLLOW_SYSTEM = 0`
- `LANGUAGE_CHINESE = 1`
- `LANGUAGE_ENGLISH = 2`

---

## 主题系统

### 夜间模式
支持三种模式：
- 跟随系统 (`MODE_NIGHT_FOLLOW_SYSTEM`)
- 日间模式 (`MODE_NIGHT_NO`)
- 夜间模式 (`MODE_NIGHT_YES`)

### 动态颜色
支持 Android 12+ 动态主题色彩，通过 `ThemeUtil` 管理。

---

## 待办事项（根据代码分析）

以下功能在代码中被注释或未完全实现：

1. **分享功能** (`Global.kt:262`): `shareCertainAppsByItems` 方法当前仅显示 Toast
2. **导入分享** (`Global.kt:366`): `shareImportItems` 方法当前仅显示 Toast
3. **批量导出** (`AppFragment.kt:256`): 导出按钮点击事件被注释掉

---

## 开发注意事项

### 权限处理
使用 XXPermissions 库处理运行时权限，特别是 Android 10+ 的存储访问权限。

### 线程安全
- `Global.app_list` 使用 `Collections.synchronizedList`
- 导出任务使用独立的 Thread
- UI 更新通过 `Global.handler` 切换到主线程

### 边到边显示
MainActivity 使用 `WindowCompat.setDecorFitsSystemWindows(window, false)` 实现全屏显示。

### 内存管理
- ViewPager2 的 TabLayoutMediator 在 onDestroy 时正确 detach
- 任务支持中断（`setInterrupted()`）
- 异步任务引用在 Fragment 生命周期中正确管理

---

## 项目维护者

项目源自 [appshare](https://github.com/ghmxr/appshare)，由牧歌进行二次开发和维护。

---

## 许可证

请查看项目根目录的 LICENSE 文件获取详细信息。
