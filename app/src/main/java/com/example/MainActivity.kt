package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.theme.MyApplicationTheme

/**
 * [MainActivity] 初始工程示例的主 Activity 入口。
 *
 * 用于测试基本的 Android Compose 启动流程与 Greeting 模板展示。
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Greeting(name = "Android", modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

/**
 * [Greeting] 示例文本渲染组件。
 *
 * @param name 问候对象名称
 * @param modifier 外部修饰符
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

/**
 * [GreetingPreview] 开发者预览函数。
 */
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}

