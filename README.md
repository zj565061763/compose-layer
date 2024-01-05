# Gradle

[![](https://jitpack.io/v/zj565061763/compose-layer.svg)](https://jitpack.io/#zj565061763/compose-layer)

# Demo

![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/align_target.gif?raw=true)
![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/align_offset.gif?raw=true)
![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/drop_down.gif?raw=true)
![](https://github.com/zj565061763/compose-layer/blob/dev/screenshots/overflow.gif?raw=true)

# 普通Layer

```kotlin
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
    // laye r内容
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
```