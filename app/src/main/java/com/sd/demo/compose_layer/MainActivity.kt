package com.sd.demo.compose_layer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Content(this)
                }
            }
        }
    }
}

@Composable
private fun Content(
    activity: Activity,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(onClick = {
            activity.startActivity(Intent(activity, SampleAlignContainer::class.java))
        }) {
            Text("Align container")
        }

        Button(onClick = {
            activity.startActivity(Intent(activity, SampleAlignTarget::class.java))
        }) {
            Text("Align target")
        }

        Button(onClick = {
            activity.startActivity(Intent(activity, SampleAlignTargetList::class.java))
        }) {
            Text("Align target list")
        }
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("FLayer-demo", block())
}