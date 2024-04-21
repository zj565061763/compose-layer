[![](https://jitpack.io/v/zj565061763/compose-layer.svg)](https://jitpack.io/#zj565061763/compose-layer)

# About

实现类似窗口的效果（Layer），支持设置目标，可以让Layer显示在目标的指定位置。

# Demo

![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/align_target.gif?raw=true)
![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/align_offset.gif?raw=true)
![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/drop_down.gif?raw=true)
![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/overflow.gif?raw=true)

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
 @Composable
private fun Content() {
    // 创建一个普通的Layer
    val layer = rememberLayer(
        onCreate = {
            // 设置在容器中的位置
            it.setPosition(Layer.Position.StartCenter)
            // 设置触摸背景消失
            it.setCanceledOnTouchBackground(true)
            it.registerAttachCallback {
                // layer 添加回调
            }
            it.registerDetachCallback {
                // layer 移除回调
            }
        },
        display = {
            // 设置layer显示隐藏动画，默认为透明度变化
            DisplaySlideUpDown()
        },
    ) {
        // layer内容
        ColorBox(
            color = Color.Red,
            text = "Box",
        )
    }

    LaunchedEffect(layer) {
        // 显示layer
        layer.attach()

        // 移除layer
        layer.detach()
    }
}
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
@Composable
private fun layer(): TargetLayer {
    // 创建跟踪目标的Layer
    return rememberTargetLayer(
        onCreate = {
            // 设置目标
            it.setTarget("hello")
            /** 设置对齐目标的位置 */
            it.setPosition(Layer.Position.Center)
            // 设置背景透明
            it.setBackgroundColor(Color.Transparent)
            // 不处理返回事件
            it.setCanceledOnBackPressed(null)
            // 不处理触摸背景事件
            it.setCanceledOnTouchBackground(null)
        }
    ) {
        // 设置要显示的内容
        ColorBox(
            color = Color.Red,
            text = "Box",
        )
    }
}
```

```kotlin
Button(
    onClick = { layer.attach() },
    // 将当前Button设置为目标
    modifier = Modifier.layerTarget("button")
) {
    Text("Click")
}
```

# 显示位置

```kotlin
enum class Position {
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

# 普通Layer接口

```kotlin
interface Layer {
    /**
     * 是否调试模式，tag:FLayer
     */
    var isDebug: Boolean

    /**
     * 当前Layer是否可见
     */
    val isVisibleState: Boolean

    /**
     * 位置
     */
    val positionState: Position

    /**
     * 背景颜色
     */
    val backgroundColorState: Color

    /**
     * 按返回键是否取消[detach]，null表示不处理返回键逻辑，默认true表示按返回键触发[detach]
     */
    val isCanceledOnBackPressedState: Boolean?

    /**
     * 触摸背景是否取消[detach]，null表示不处理，事件会透过背景，默认false表示触摸背景不会[detach]
     */
    val isCanceledOnTouchBackgroundState: Boolean?

    /**
     * 对齐的位置，默认[Position.Center]
     */
    fun setPosition(position: Position)

    /**
     * 背景颜色
     */
    fun setBackgroundColor(color: Color)

    /**
     * 按返回键是否取消[detach]，null表示不处理返回键逻辑，默认true表示按返回键触发[detach]
     */
    fun setCanceledOnBackPressed(value: Boolean?)

    /**
     * 触摸背景是否取消[detach]，null表示不处理，事件会透过背景，默认false表示触摸背景不会[detach]
     */
    fun setCanceledOnTouchBackground(value: Boolean?)

    /**
     * 是否裁剪内容区域，默认true
     */
    fun setClipToBounds(clipToBounds: Boolean)

    /**
     * 注册[attach]回调
     */
    fun registerAttachCallback(callback: (Layer) -> Unit)

    /**
     * 取消注册
     */
    fun unregisterAttachCallback(callback: (Layer) -> Unit)

    /**
     * 注册[detach]回调
     */
    fun registerDetachCallback(callback: (Layer) -> Unit)

    /**
     * 取消注册
     */
    fun unregisterDetachCallback(callback: (Layer) -> Unit)

    /**
     * 添加到容器
     */
    fun attach()

    /**
     * 从容器上移除
     */
    fun detach()
}
```

# 目标Layer接口

```kotlin
interface TargetLayer : Layer {
    /**
     * 设置目标
     */
    fun setTarget(target: String?)

    /**
     * 设置目标坐标
     */
    fun setTarget(offset: IntOffset?)

    /**
     * 设置目标X方向偏移量
     */
    fun setTargetOffsetX(offset: TargetOffset?)

    /**
     * 设置目标Y方向偏移量
     */
    fun setTargetOffsetY(offset: TargetOffset?)

    /**
     * 设置是否修复溢出，默认true
     */
    fun setFixOverflow(fixOverflow: Boolean)

    /**
     * 设置是否查找最佳的显示位置，默认false
     */
    fun setFindBestPosition(findBestPosition: Boolean)

    /**
     * 设置要裁切背景的方向[Directions]
     */
    fun setClipBackgroundDirection(direction: Directions?)
}

sealed interface TargetOffset {
    /**
     * 偏移指定像素
     */
    data class PX(val value: Int) : TargetOffset

    /**
     * 偏移目标大小的倍数，例如：1表示向正方向偏移1倍目标的大小，-1表示向负方向偏移1倍目标的大小
     */
    data class Percent(val value: Float) : TargetOffset
}
```