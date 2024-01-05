# About

实现类似窗口的效果（Layer），支持设置目标，可以让Layer显示在目标的指定位置。

# Gradle

[![](https://jitpack.io/v/zj565061763/compose-layer.svg)](https://jitpack.io/#zj565061763/compose-layer)

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