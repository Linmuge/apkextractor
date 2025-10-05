# AppShare UI设计规范文档

> 基于 Material Design 3 设计语言的完整UI规范
> 
> 生成日期：2025-10-05

---

## 目录

1. [设计原则](#设计原则)
2. [颜色系统](#颜色系统)
3. [文本样式](#文本样式)
4. [间距与尺寸](#间距与尺寸)
5. [组件规范](#组件规范)
6. [布局模式](#布局模式)
7. [可复用资源](#可复用资源)

---

## 1. 设计原则

### 1.1 核心原则

- **Material Design 3 优先**：严格遵循 MD3 设计语言
- **主题颜色属性**：永不创建新的颜色资源，仅使用主题颜色属性
- **屏幕适配基准**：所有UI组件按 **375dp 屏幕宽度**设计基准进行适配
- **ViewBinding + Kotlin**：使用 ViewBinding 和 Kotlin 协程
- **无障碍支持**：合并相关UI元素为单一焦点，添加 contentDescription

### 1.2 禁止事项

❌ 不创建新的颜色资源文件  
❌ 不使用 `android:fitsSystemWindows="true"`（使用 WindowInsets 代替）  
❌ 不使用硬编码颜色值  
❌ 不使用 ViewModel（除非特殊说明）

---

## 2. 颜色系统

### 2.1 主题颜色属性

所有颜色必须使用主题属性，不得硬编码：

#### 主色系

| 属性 | 用途 | 示例场景 |
|------|------|----------|
| `?attr/colorPrimary` | 主要品牌色 | 按钮、强调文本、选中状态 |
| `?attr/colorOnPrimary` | 主色上的文本/图标 | 主色按钮上的文字 |
| `?attr/colorPrimaryContainer` | 主色容器 | 选中卡片背景 |
| `?attr/colorOnPrimaryContainer` | 主色容器上的内容 | 选中卡片上的文字 |

#### 表面色系

| 属性 | 用途 | 示例场景 |
|------|------|----------|
| `?attr/colorSurface` | 页面背景色 | Activity/Fragment 背景 |
| `?attr/colorSurfaceContainer` | **卡片容器背景色** | MaterialCardView 背景（最常用） |
| `?attr/colorSurfaceContainerLow` | 低层级容器 | 次要卡片 |
| `?attr/colorSurfaceContainerHigh` | 高层级容器 | 悬浮卡片 |
| `?attr/colorOnSurface` | 表面上的主要文本 | 标题、正文 |
| `?attr/colorOnSurfaceVariant` | 表面上的次要文本 | 副标题、说明文字 |

#### 其他色系

| 属性 | 用途 |
|------|------|
| `?attr/colorSecondary` | 次要强调色 |
| `?attr/colorError` | 错误状态 |
| `?attr/colorOutline` | 边框线 |
| `?attr/colorOutlineVariant` | 次要边框线 |

### 2.2 颜色使用规则

```xml
<!-- ✅ 正确：使用主题属性 -->
<TextView
    android:textColor="?attr/colorOnSurface" />

<MaterialCardView
    app:cardBackgroundColor="?attr/colorSurfaceContainer" />

<!-- ❌ 错误：硬编码颜色 -->
<TextView
    android:textColor="#333333" />
```

### 2.3 Kotlin 代码中获取主题颜色

```kotlin
// 扩展属性方式（推荐）
val Context.colorPrimary: Int @ColorInt get() = getThemeColor(R.attr.colorPrimary)
val Context.colorOnPrimary: Int @ColorInt get() = getThemeColor(R.attr.colorOnPrimary)
val Context.colorSurface: Int @ColorInt get() = getThemeColor(R.attr.colorSurface)
val Context.colorOnSurface: Int @ColorInt get() = getThemeColor(R.attr.colorOnSurface)
val Context.colorSurfaceContainer: Int @ColorInt get() = getThemeColor(R.attr.colorSurfaceContainer)
val Context.colorOnSurfaceVariant: Int @ColorInt get() = getThemeColor(R.attr.colorOnSurfaceVariant)
val Context.colorPrimaryContainer: Int @ColorInt get() = getThemeColor(R.attr.colorPrimaryContainer)
val Context.colorOnPrimaryContainer: Int @ColorInt get() = getThemeColor(R.attr.colorOnPrimaryContainer)

// 使用示例
cardView.setCardBackgroundColor(context.colorSurfaceContainer)
textView.setTextColor(context.colorOnSurface)
```

---

## 3. 文本样式

### 3.1 Material Design 3 文本样式

使用 MD3 预定义的文本样式，不要自定义 textSize：

| 样式属性 | 用途 | 文本颜色 | 加粗 |
|----------|------|----------|------|
| `?attr/textAppearanceDisplayLarge` | 超大标题 | `colorOnSurface` | - |
| `?attr/textAppearanceHeadlineLarge` | 大标题 | `colorOnSurface` | - |
| `?attr/textAppearanceHeadlineMedium` | 中标题 | `colorOnSurface` | - |
| `?attr/textAppearanceHeadlineSmall` | 小标题 | `colorOnSurface` | ✓ |
| `?attr/textAppearanceTitleLarge` | 大标题文本 | `colorPrimary` | ✓ |
| `?attr/textAppearanceTitleMedium` | **标题文本（常用）** | `colorPrimary` | ✓ |
| `?attr/textAppearanceTitleSmall` | 小标题文本 | `colorPrimary` | ✓ |
| `?attr/textAppearanceBodyLarge` | 大正文 | `colorOnSurface` | - |
| `?attr/textAppearanceBodyMedium` | **正文（常用）** | `colorOnSurface` | - |
| `?attr/textAppearanceBodySmall` | **副文本（常用）** | `colorOnSurfaceVariant` | - |
| `?attr/textAppearanceLabelLarge` | 大标签 | - | - |
| `?attr/textAppearanceLabelMedium` | 中标签 | - | - |
| `?attr/textAppearanceLabelSmall` | 小标签 | - | - |

### 3.2 文本样式使用示例

```xml
<!-- 卡片标题 -->
<TextView
    android:textAppearance="?attr/textAppearanceTitleMedium"
    android:textColor="?attr/colorPrimary"
    android:textStyle="bold" />

<!-- 主要内容文本 -->
<TextView
    android:textAppearance="?attr/textAppearanceBodyMedium"
    android:textColor="?attr/colorOnSurface" />

<!-- 次要说明文本 -->
<TextView
    android:textAppearance="?attr/textAppearanceBodySmall"
    android:textColor="?attr/colorOnSurfaceVariant" />
```

### 3.3 特殊文本样式

```xml
<!-- 加粗文本（fontWeight 600） -->
<style name="bold600" parent="Widget.AppCompat.TextView">
    <item name="android:textStyle">bold</item>
    <item name="android:fontWeight" tools:targetApi="o">600</item>
</style>

<!-- Tab 选中文本 -->
<style name="tabSelectText" parent="Widget.AppCompat.TextView">
    <item name="android:textSize">16sp</item>
    <item name="android:textStyle">bold</item>
</style>

<!-- Tab 未选中文本 -->
<style name="tabUnSelectText" parent="Widget.AppCompat.TextView">
    <item name="android:textSize">14sp</item>
    <item name="android:textStyle">normal</item>
</style>
```

---

## 4. 间距与尺寸

### 4.1 标准间距值

项目使用 **8dp 栅格系统**：

| 尺寸名称 | 数值 | 用途 |
|----------|------|------|
| `@dimen/dp_8` | 8dp | 小间距、垂直间距 |
| `@dimen/dp_12` | 12dp | 中等间距 |
| `@dimen/dp_16` | 16dp | **标准水平内边距（最常用）** |
| `activity_horizontal_margin` | 16dp | Activity 水平边距 |
| `activity_vertical_margin` | 16dp | Activity 垂直边距 |

### 4.2 间距使用规范

#### 卡片内边距

```xml
<!-- 标准卡片内边距：16dp 水平，垂直根据内容调整 -->
<LinearLayout
    android:paddingHorizontal="16dp"
    android:paddingTop="16dp"
    android:paddingBottom="12dp">
```

#### 垂直间距

- **卡片之间**：8dp
- **组件之间**：8dp
- **段落之间**：12dp
- **区块之间**：16dp

#### 水平间距

- **页面边距**：16dp（标准）
- **卡片内边距**：16dp
- **图标与文本**：8-12dp

### 4.3 圆角半径

| 圆角大小 | 用途 | 样式名称 |
|----------|------|----------|
| 4dp | 小组件 | `shapeImage4dp` |
| 8dp | 小卡片 | `shapeImage8dp` |
| 10dp | 应用图标 | `shapeImage10dp` |
| 12dp | 中等卡片 | `shapeImage12dp` |
| **16dp** | **标准卡片（最常用）** | `shapeImage16dp` |
| 18dp | 大卡片 | `shapeImage18dp` |
| 28dp | Expressive 风格 | `shapeImage28dp` |
| 50% | 圆形 | `shapeImageCircle` |

### 4.4 图标尺寸

| 尺寸 | 用途 |
|------|------|
| 20dp | 小图标（按钮内） |
| 24dp | **标准图标（最常用）** |
| 40dp | 可点击图标区域 |
| 44dp | 应用图标（横向列表） |
| 48dp | 最小触摸目标 |

---

## 5. 组件规范

### 5.1 MaterialCardView（卡片容器）

#### 标准卡片样式

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    style="@style/Widget.Material3.CardView.Filled"
    app:cardCornerRadius="16dp"
    app:cardElevation="0dp"
    app:strokeWidth="0dp"
    app:cardBackgroundColor="?attr/colorSurfaceContainer"
    android:clickable="true"
    android:focusable="true"
    android:foreground="@drawable/ripple_16dp_surfacecontainer">
```

#### 关键属性说明

- **圆角**：`app:cardCornerRadius="16dp"`（标准）
- **背景色**：`app:cardBackgroundColor="?attr/colorSurfaceContainer"`
- **阴影**：`app:cardElevation="0dp"`（MD3 扁平化设计）
- **描边**：`app:strokeWidth="0dp"`（无描边）
- **水波纹**：`android:foreground="@drawable/ripple_16dp_surfacecontainer"`

#### 选中状态卡片

```kotlin
// 选中状态
(root as MaterialCardView).apply {
    strokeColor = colorPrimary
    strokeWidth = 2.dp
    setCardBackgroundColor(ColorStateList.valueOf(colorPrimaryContainer))
}

// 未选中状态
(root as MaterialCardView).apply {
    strokeColor = colorOutlineVariant
    strokeWidth = 1.dp
    setCardBackgroundColor(ColorStateList.valueOf(colorSurfaceContainerLow))
}
```

### 5.2 MaterialButton（按钮）

#### 主要按钮（Filled Button）

```xml
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="确定"
    android:minHeight="48dp"
    style="@style/Widget.Material3.Button" />
```

#### 次要按钮（Tonal Button）

```xml
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="取消"
    style="@style/Widget.Material3.Button.TonalButton" />
```

#### 轮廓按钮（Outlined Button）

```xml
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="更多"
    style="@style/Widget.Material3.Button.OutlinedButton" />
```

### 5.3 MaterialToolbar（顶部栏）

```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/toolbar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="?attr/colorSurface"
    android:elevation="0dp"
    app:contentInsetStartWithNavigation="0dp">
    
    <!-- 返回按钮 -->
    <ImageView
        android:id="@+id/ivBack"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:src="@drawable/ic_back"
        android:contentDescription="返回"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:padding="8dp"
        app:tint="?attr/colorOnSurface" />
</com.google.android.material.appbar.MaterialToolbar>
```

### 5.4 标题栏（TitleView）

使用统一的 `titleview.xml`：

```xml
<include
    android:id="@+id/titleView"
    layout="@layout/titleview" />
```

Kotlin 初始化：

```kotlin
binding.titleView.init(activity, "页面标题", fitsystem = false)
    .more(moreText = "更多")
    .click { view ->
        // 点击事件
    }
```

### 5.5 Chip（筛选标签）

```xml
<com.google.android.material.chip.Chip
    android:id="@+id/chip"
    style="@style/Widget.Material3.Chip.Filter"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="标签"
    android:checkable="true"
    app:checkedIconVisible="false" />
```

### 5.6 MaterialSwitch（开关）

```xml
<com.google.android.material.materialswitch.MaterialSwitch
    android:id="@+id/switch"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="开关选项" />
```

---

## 6. 布局模式

### 6.1 列表项布局（应用列表）

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    style="@style/Widget.Material3.CardView.Filled"
    app:cardBackgroundColor="?colorSurfaceContainer">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="14dp"
        android:paddingVertical="10dp">
        
        <!-- 应用图标 -->
        <com.google.android.material.imageview.ShapeableImageView
            android:id="@+id/ivIcon"
            android:layout_width="48dp"
            android:layout_height="48dp"
            app:shapeAppearance="@style/shapeImage12dp" />
        
        <!-- 应用名称 -->
        <TextView
            android:id="@+id/tvName"
            android:textAppearance="?attr/textAppearanceBodyMedium"
            android:textColor="?attr/colorOnSurface"
            android:textStyle="bold" />
        
        <!-- 应用描述 -->
        <TextView
            android:id="@+id/tvDesc"
            android:textAppearance="?attr/textAppearanceBodySmall"
            android:textColor="?attr/colorOnSurfaceVariant" />
    </androidx.constraintlayout.widget.ConstraintLayout>
</com.google.android.material.card.MaterialCardView>
```

### 6.2 设置项布局

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:background="@drawable/ripple_16dp_surfacecontainer"
    android:padding="16dp">
    
    <!-- 图标 -->
    <ImageView
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:src="@drawable/ic_setting"
        app:tint="?attr/colorOnSurfaceVariant" />
    
    <!-- 文本区域 -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginStart="12dp"
        android:orientation="vertical">
        
        <TextView
            android:textAppearance="?attr/textAppearanceBodyMedium"
            android:textColor="?attr/colorOnSurface"
            android:textStyle="bold" />
        
        <TextView
            android:textAppearance="?attr/textAppearanceBodySmall"
            android:textColor="?attr/colorOnSurfaceVariant" />
    </LinearLayout>
    
    <!-- 右侧控件（开关/箭头） -->
    <com.google.android.material.materialswitch.MaterialSwitch
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />
</LinearLayout>
```

### 6.3 RecyclerView 布局管理

```kotlin
// 网格布局（根据屏幕宽度自适应）
recyclerView.gridLayout(requireActivity(), dividerWidth = 8).setup {
    // ...
}

// 瀑布流布局
recyclerView.staggeredLayout(requireActivity(), dividerWidth = 8).setup {
    // ...
}

// 线性布局
recyclerView.linear(RecyclerView.VERTICAL).divider {
    setDivider(8, true)
    startVisible = true
    endVisible = true
}.setup {
    // ...
}
```

---

## 7. 可复用资源

### 7.1 Ripple 水波纹背景

| 资源名称 | 圆角 | 背景色 | 用途 |
|----------|------|--------|------|
| `ripple_16dp_surfacecontainer` | 16dp | colorSurfaceContainer | **标准卡片（最常用）** |
| `ripple_12dp_surfacecontainer` | 12dp | colorSurfaceContainer | 中等卡片 |
| `ripple_8dp_surface_container` | 8dp | colorSurfaceContainer | 小卡片 |
| `ripple_circle_surface_container` | 圆形 | colorSurfaceContainer | 圆形按钮 |
| `ripple_16dp_surface` | 16dp | colorSurface | 表面色卡片 |
| `ripple_md3` | 16dp | 透明 | MD3 标准水波纹 |

### 7.2 Shape 背景

| 资源名称 | 圆角 | 背景色 |
|----------|------|--------|
| `bg_16dp` | 16dp | titleViewBgColor |
| `bg_12dp` | 12dp | titleViewBgColor |
| `bg_10dp` | 10dp | titleViewBgColor |
| `bg_8dp` | 8dp | titleViewBgColor |

### 7.3 图片圆角样式

```xml
<!-- 圆形图片（头像） -->
<style name="shapeImageCircle">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">50%</item>
    <item name="android:scaleType">centerCrop</item>
</style>

<!-- 应用图标（10dp圆角） -->
<style name="shapeImage10dp">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">10dp</item>
</style>

<!-- 标准卡片图片（16dp圆角） -->
<style name="shapeImage16dp">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">16dp</item>
</style>

<!-- Expressive 风格（28dp圆角） -->
<style name="shapeImage28dp">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">28dp</item>
</style>
```

使用示例：

```xml
<com.google.android.material.imageview.ShapeableImageView
    android:layout_width="48dp"
    android:layout_height="48dp"
    app:shapeAppearance="@style/shapeImage12dp"
    android:scaleType="centerCrop" />
```

---

## 附录：快速参考

### 最常用的组合

#### 标准卡片

```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.Material3.CardView.Filled"
    app:cardCornerRadius="16dp"
    app:cardElevation="0dp"
    app:cardBackgroundColor="?attr/colorSurfaceContainer"
    android:foreground="@drawable/ripple_16dp_surfacecontainer">
    
    <LinearLayout
        android:paddingHorizontal="16dp"
        android:paddingVertical="12dp">
        <!-- 内容 -->
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

#### 标题 + 正文 + 副文本

```xml
<!-- 标题 -->
<TextView
    android:textAppearance="?attr/textAppearanceTitleMedium"
    android:textColor="?attr/colorPrimary"
    android:textStyle="bold" />

<!-- 正文 -->
<TextView
    android:textAppearance="?attr/textAppearanceBodyMedium"
    android:textColor="?attr/colorOnSurface" />

<!-- 副文本 -->
<TextView
    android:textAppearance="?attr/textAppearanceBodySmall"
    android:textColor="?attr/colorOnSurfaceVariant" />
```

#### 图标 + 文本

```xml
<ImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    app:tint="?attr/colorOnSurfaceVariant" />

<TextView
    android:layout_marginStart="8dp"
    android:textAppearance="?attr/textAppearanceBodyMedium"
    android:textColor="?attr/colorOnSurface" />
```

---

**文档版本**：v1.0  
**最后更新**：2025-10-05  
**维护者**：AppShare 开发团队

