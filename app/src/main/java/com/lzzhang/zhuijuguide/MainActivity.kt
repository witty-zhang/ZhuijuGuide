package com.lzzhang.zhuijuguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzzhang.zhuijuguide.ui.GuideApp
import com.lzzhang.zhuijuguide.ui.GuideViewModel
import com.lzzhang.zhuijuguide.ui.LocalIsTv
import com.lzzhang.zhuijuguide.ui.isTelevision
import com.lzzhang.zhuijuguide.ui.theme.ZhuijuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 在进入 Compose 之前先判断设备形态，主题随之切换字号与明暗
        val isTv = isTelevision()

        setContent {
            val tv = remember { isTv }
            CompositionLocalProvider(LocalIsTv provides tv) {
                ZhuijuTheme(isTv = tv) {
                    val guideViewModel: GuideViewModel = viewModel(
                        factory = GuideViewModel.Factory(applicationContext),
                    )
                    GuideApp(viewModel = guideViewModel)
                }
            }
        }
    }
}
