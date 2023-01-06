package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.*

class SampleAlignContainer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface {
                    LayerContainer(modifier = Modifier.fillMaxSize()) {
                        Content()
                    }
                }
            }
        }
    }
}

@Composable
private fun Content() {
    val layer1 = layer1()
    val layer2 = layer2()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = {
                layer1.attach()
                layer2.attach()
            }
        ) {
            Text(text = "Attach")
        }
    }
}

/**
 * [rememberLayer]会在内存中保存Layer对象
 */
@Composable
private fun layer1(): Layer {
    return rememberLayer(
        onCreate = {
            it.isDebug = true
            it.setZIndex(1f)
            it.registerAttachCallback {
                logMsg { "onAttach" }
            }
            it.registerDetachCallback {
                logMsg { "onDetach" }
            }
        },
    ) {
        ColorBox(
            color = Color.Red,
            text = "Box1",
        )
    }
}

/**
 * [rememberLayerApi]每次显示的时候都会新建一个新的Layer对象
 */
@Composable
private fun layer2(): LayerApi {
    return rememberLayerApi(
        onCreate = {
            it.setPosition(Layer.Position.BottomCenter)
        }
    ) {
        ColorBox(
            color = Color.Red,
            text = "Box2",
        )
    }
}