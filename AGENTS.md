# compose-layer

Jetpack Compose 浮层容器库，支持对齐容器的普通浮层（`Layer` / `layer`）与对齐目标元素或坐标的目标浮层（`TargetLayer` / `targetLayer`）。

## 模块结构

| 模块 | 包名 | 职责 |
|---|---|---|
| `:lib` | `com.sd.lib.compose.layer` | 核心库，提供浮层容器、生命周期管理、目标定位、智能选位、溢出修正、背景裁切与过渡动画 |
| `:app` | `com.sd.demo.compose_layer` | 示例应用，按特性拆分为独立的 `Sample*` Activity |

- Maven 发布信息（坐标 `io.github.zj565061763.android:compose-layer` 与版本号）配置在 `lib/gradle.properties`。

## 构建与验证命令

- 编译库与示例应用：`./gradlew :lib:assembleRelease :app:assembleDebug`
- 运行本地单元测试：`./gradlew testDebugUnitTest`
- 运行 Lint 检查：`./gradlew lintDebug`

## 核心源码职责

源码目录：`lib/src/main/java/com/sd/lib/compose/layer/`

| 文件 | 职责 |
|---|---|
| `Api.kt` | 公开入口：`LayerContainer`、`Layer` / `layer`、`TargetLayer` / `targetLayer`、`Modifier.layerTag` 及内部 `CompositionLocal` |
| `LayerTag.kt` | `LayerTagElement` 与 `LayerTagNode` 实现，负责 `Modifier.layerTag` 节点的容器绑定、唯一性校验与按需窗口矩形上报 |
| `LayerContainer.kt` | 容器双接口拆分（`ContainerForComposable` / `ContainerForLayer`）、`LayoutTrackNode` 按需矩形追踪与挂载列表管理 |
| `Layer.kt` | `LayerState`、`LayerLifecycleState`、`LayerDetach` 定义及基类 `LayerImpl` 的生命周期、过渡状态、返回键与外部触摸控制 |
| `NormalLayer.kt` | `NormalLayerImpl` 实现，按 `Alignment` 对齐容器并根据方位匹配默认过渡动画 |
| `TargetLayer.kt` | `TargetLayerImpl` 实现，通过单树 `Layout` / `LookaheadScope` 完成目标对齐、智能选位、溢出修正与绘制期背景裁切 |
| `FixOverflow.kt` | `fixOverFlow` 溢出尺寸裁剪算法，在可内收边越界时循环缩减宽高并保护固定锚定边 |
| `Aligner.kt` | 纯坐标计算引擎，根据目标、容器与内容尺寸及 `LayoutDirection` 计算相对坐标与四边溢出量 |
| `TargetAlignment.kt` | 17 种目标对齐位置枚举，以及 `Tag` 目标与 `Offset` 目标的默认 `LayerTransition` 映射 |
| `TargetAlignmentOffset.kt` | `PX`、`DP`、`Target`（目标尺寸倍数）偏移量及 `relativeAlignment` 相对对齐方向换算 |
| `SmartAliments.kt` | 智能对齐候选列表 `SmartAliments` 与单项配置 `SmartAliment` |
| `Directions.kt` | 位掩码方向定义（`Top`、`Bottom`、`Start`、`End`），支持 `+` 组合与 RTL 方向转换 |
| `LayerTransition.kt` | 进出场动画封装，内置无动画、淡入淡出、四向滑动与四角锚点缩放 |

## 内部机制与开发约束

- `LayerContainerImpl` 同时实现 `ContainerForComposable`（供 `LayerContainer` 与 `Modifier.layerTag` 注册 `LayoutTrackNode`）和 `ContainerForLayer`（供 `LayerImpl` 挂载与按需订阅窗口矩形），通过两个独立的 `staticCompositionLocalOf` 隔离暴露范围；无 `TargetLayer` 订阅时容器与 `tag` 节点不注册 `registerOnLayoutRectChanged` 监听。
- 同一 `LayerContainer` 内已放置节点的非空 `tag` 必须唯一，不同已放置节点复用同一 `tag` 会触发校验失败。
- `LocalInspectionMode` 预览模式下，未包裹 `LayerContainer` 时 `Modifier.layerTag` 与 `Layer` 均跳过容器绑定以避免预览报错；包裹 `LayerContainer` 且 `attach = true` 时同步挂载、上报目标坐标并直接静态渲染以支持预览。
- `LayerImpl` 生命周期按 `Initial -> Ready -> Attached -> Detaching -> Ready` 流转，内容与背景各自持有独立的 `MutableTransitionState`，调用 `detach()` 时进入 `Detaching` 并将两者的 `targetState` 置为 `false`，待两者退场动画均进入 `isIdle` 后从容器移除并回到 `Ready`；`Detaching` 退场中途再次调用 `attach()` 会切回 `Attached` 并将 `targetState` 恢复为 `true` 平滑反转动画。
- `detachOnBackPress` 与 `detachOnTouchOutside` 仅在 `isVisibleState == true` 时生效，`true` 回调 `onDetachRequest`，`false` 消费事件但不请求移除，`null` 不注册拦截以透传事件；多个 `Layer` 同时挂载时，容器按 `zIndex`（相同 `zIndex` 后挂载者优先）由最顶层需拦截返回键的 `Layer` 处理返回事件，且 `HandleBackPress()` 先于各浮层 `Content()` 组合以允许浮层内容内部的 `BackHandler` 优先响应。
- `ContentBox` 内置空 `pointerInput` 消费内容区点击事件，防止点击穿透到下方的外部触摸拦截层。
- `ContentBox` 默认使用 `CapturedAnimatedVisibility` 执行内容动画（`captureOnDetach = true`），退场开始时立即移除子内容组合并基于捕获帧播放退场动画；设为 `false` 时使用 `AnimatedVisibility`。
- 容器与 `Tag` 目标均通过窗口矩形（`LayoutCoordinates.positionInWindow().round()` 与 `RelativeLayoutBounds.boundsInWindow`）采集坐标，因此 `LayerTarget.Offset` 传入的坐标也必须基于 Window 坐标系（如 `localToWindow` 转换结果）。
- `TargetLayer` 无需 `SubcomposeLayout`：未启用 `smartAlignments` 时通过 `FixedTargetBox` 预计算最大允许尺寸并单遍测量放置 `ContentBox`；启用 `smartAlignments` 时通过 `LookaheadScope` + `SmartTargetLayoutNode`（`ApproachLayoutModifierNode`）基于 Lookahead 自然尺寸智能选位并在仍溢出时裁剪尺寸，且未显式指定 `transition` 时首次进入前通过 `SmartProbeBox` 配合 `movableContentOf` 探测自然尺寸以确定默认智能动画。
- `TargetLayer` 在容器或目标未就绪（`!isReady`）时复用上一帧缓存的 `_placedContentRect`，保证目标消失或卸载时退场动画仍在原位播放。
- `fixOverFlow` 在可随尺寸内收的边越界时循环缩减宽高并重算对齐坐标，水平或垂直居中对齐且仅单侧溢出时按双倍溢出量缩减对应维度，维持居中对齐关系。
- `clipBackgroundDirection` 表示裁切掉内容对应外侧方向的背景遮罩（如 `Directions.Top` 裁掉内容上方的遮罩，使背景从内容顶部向下延伸），在 `BackgroundBox` 绘制阶段通过 `drawWithContent` + `clipRect` 完成裁切，支持通过 `+` 组合多个方向并在 RTL 下自动互换 `Start` 与 `End`。
- 未显式指定 `transition` 时，`TargetLayer` 若命中 `SmartAliment` 则优先使用该项的动画，否则 `LayerTarget.Offset` 在四角对齐时使用锚点缩放动画（`offsetTransition`），其余场景使用滑动或淡入淡出动画（`defaultTransition`）。
- `transition` 存储在普通属性而非 Compose `State` 中，属于非响应式配置，运行期变更不会主动触发重组。
- `SmartAliments` 与 `SmartAliment` 为既有公开 API 命名（历史拼写），修改时必须保留原类名以确保兼容性。
- 无返回值的 `Layer` / `TargetLayer` 与返回 `LayerState` 的 `layer` / `targetLayer` 参数签名必须保持同步。
- 库内调试日志在 `debug = true` 时统一使用 `FLayer` 作为 Logcat tag，`:app` 示例日志使用 `FLayer-demo`。

## 示例与验证场景对照

| Activity | 验证场景 |
|---|---|
| `SampleAlignContainer` | 普通 `layer` 容器对齐及 `LayerState.isVisibleState` / `lifecycleState` 状态监听 |
| `SampleAlignTarget` | `targetLayer` 17 种 `TargetAlignment` 对齐滚动目标及目标动态显隐 |
| `SampleDropDown` | 下拉菜单对齐 `Tag` 目标与 `Directions.Top` 裁切上方背景遮罩 |
| `SampleTargetOffset` | `PX` / `Target` 偏移量、组合方向背景裁切与 LTR/RTL 布局方向切换 |
| `SampleFixOverflow` | 长列表超出容器边界时的溢出尺寸裁剪与自定义缩放动画 |
| `SampleZIndex` | 多个 `Layer` 同时挂载时的 `zIndex` 层级覆盖顺序 |
| `SampleListMenu` | 列表点击窗口坐标 `LayerTarget.Offset`、`SmartAliments.Default` 智能选位与 `relativeAlignment` 相对偏移 |

## 单元测试对照

测试目录：`lib/src/test/java/com/sd/lib/compose/layer/`

| 测试类 | 覆盖范围 |
|---|---|
| `AlignerTest` | 17 种 `Aligner.Position` 在 LTR/RTL 下的相对坐标计算及 `sourceOverflow` 溢出统计 |
| `TargetAlignmentOffsetTest` | `PX`、`DP`、`Target` 像素换算及 `relativeAlignment` 在 X/Y 轴的正负向与 `downgrade` 降级 |
| `DirectionsTest` | 单方向位标志、`+` 组合值相等与幂等性及 `rtl()` 左右方向互换 |
| `TargetLayerFixOverflowTest` | `fixOverFlow` 四边内收裁剪、居中单双侧溢出修正及固定锚定边越界保护 |
| `TargetLayerLayoutTest` | `calculateMaxAllowedSize` 预计算最大允许尺寸与 `calculateClipBackgroundBounds` 绘制期背景裁切边界（含 LTR/RTL 与越界收敛） |
| `TargetAlignmentTransitionTest` | `TargetAlignment.offsetTransition` 在 LTR/RTL 下的四角缩放锚点映射 |
