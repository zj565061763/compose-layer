[![](https://jitpack.io/v/zj565061763/compose-layer.svg)](https://jitpack.io/#zj565061763/compose-layer)

# About

实现类似窗口的效果（Layer），支持设置目标，可以让Layer显示在目标的指定位置。

# Demo

![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/align_target.gif?raw=true)
![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/align_offset.gif?raw=true)
![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/drop_down.gif?raw=true)

# 普通Layer

```kotlin
AppTheme {
   // 添加layer容器
   LayerContainer {
      Content()
   }
}
```

```kotlin
/**
 * 创建Layer
 *
 * @param attach 是否添加Layer，true-添加；false-移除
 * @param onDetachRequest [LayerDetach]触发的移除回调
 * @param debug 是否调试模式，tag:FLayer
 * @param detachOnBackPress 按返回键是否移除Layer，true-移除；false-不移除；null-不处理返回键逻辑，默认true
 * @param detachOnTouchOutside 触摸非内容区域是否移除Layer，true-移除；false-不移除；null-不处理，事件会透过背景，默认false
 * @param backgroundColor 背景颜色
 * @param alignment 对齐容器位置
 * @param display Layer显示，通常用来做动画效果
 * @param content Layer内容
 */
@Composable
fun Layer(
   attach: Boolean,
   onDetachRequest: (LayerDetach) -> Unit,
   debug: Boolean = false,
   detachOnBackPress: Boolean? = true,
   detachOnTouchOutside: Boolean? = false,
   backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
   alignment: Alignment = Alignment.Center,
   display: LayerDisplay? = null,
   content: @Composable LayerContentScope.() -> Unit,
)
```

# 目标Layer

```kotlin
AppTheme {
   // 添加layer容器
   LayerContainer {
      Content()
   }
}
```

```kotlin
/**
 * 创建TargetLayer
 *
 * @param target 要对齐的目标
 * @param attach 是否添加Layer，true-添加；false-移除
 * @param onDetachRequest [LayerDetach]触发的移除回调
 * @param debug 是否调试模式，tag:FLayer
 * @param detachOnBackPress 按返回键是否移除Layer，true-移除；false-不移除；null-不处理返回键逻辑，默认值true
 * @param detachOnTouchOutside 触摸非内容区域是否移除Layer，true-移除；false-不移除；null-不处理，事件会透过背景，默认值false
 * @param backgroundColor 背景颜色
 * @param alignment 对齐目标位置
 * @param alignmentOffsetX 对齐目标X方向偏移量
 * @param alignmentOffsetY 对齐目标Y方向偏移量
 * @param smartAlignments 智能对齐目标位置（非响应式），null-关闭智能对齐；非null-开启智能对齐，如果是空列表则采用内置的对齐列表，默认关闭智能对齐。
 * 开启之后，如果默认的[alignment]导致内容溢出会使用[smartAlignments]提供的位置按顺序查找溢出最小的位置
 * @param clipBackgroundDirection （非响应式）裁切背景的方向[Directions]
 * @param display Layer显示，通常用来做动画效果
 * @param content Layer内容
 */
@Composable
fun TargetLayer(
   target: LayerTarget?,
   attach: Boolean,
   onDetachRequest: (LayerDetach) -> Unit,
   debug: Boolean = false,
   detachOnBackPress: Boolean? = true,
   detachOnTouchOutside: Boolean? = false,
   backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
   alignment: TargetAlignment = TargetAlignment.Center,
   alignmentOffsetX: TargetAlignmentOffset? = null,
   alignmentOffsetY: TargetAlignmentOffset? = null,
   smartAlignments: List<TargetAlignment>? = null,
   clipBackgroundDirection: Directions? = null,
   display: LayerDisplay? = null,
   content: @Composable LayerContentScope.() -> Unit,
)
```

### 要对齐的目标

```kotlin
@Immutable
sealed interface LayerTarget {
   /** 以[tag]为目标 */
   data class Tag(val tag: String?) : LayerTarget

   /** 以[offset]为目标 */
   data class Offset(val offset: IntOffset?) : LayerTarget
}
```

### 目标对齐位置

```kotlin
enum class TargetAlignment {
   /** 顶部开始方向对齐 */
   TopStart,
   /** 顶部中间对齐 */
   TopCenter,
   /** 顶部结束方向对齐 */
   TopEnd,
   /** 顶部对齐，不计算x坐标，默认x坐标为0 */
   Top,

   /** 底部开始方向对齐 */
   BottomStart,
   /** 底部中间对齐 */
   BottomCenter,
   /** 底部结束方向对齐 */
   BottomEnd,
   /** 底部对齐，不计算x坐标，默认x坐标为0 */
   Bottom,

   /** 开始方向顶部对齐 */
   StartTop,
   /** 开始方向中间对齐 */
   StartCenter,
   /** 开始方向底部对齐 */
   StartBottom,
   /** 开始方向对齐，不计算y坐标，默认y坐标为0 */
   Start,

   /** 结束方向顶部对齐 */
   EndTop,
   /** 结束方向中间对齐 */
   EndCenter,
   /** 结束方向底部对齐 */
   EndBottom,
   /** 结束方向对齐，不计算y坐标，默认y坐标为0 */
   End,

   /** 中间对齐 */
   Center,
}
```

### 目标对齐位置偏移量

```kotlin
@Immutable
sealed interface TargetAlignmentOffset {
   /** 按指定像素[value]偏移 */
   data class PX(val value: Int) : TargetAlignmentOffset

   /** 按偏移目标大小倍数[value]偏移，例如：1表示向正方向偏移1倍目标大小，-1表示向负方向偏移1倍目标大小 */
   data class Percent(val value: Float) : TargetAlignmentOffset
}
```