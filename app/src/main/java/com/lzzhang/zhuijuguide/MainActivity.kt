package com.lzzhang.zhuijuguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzzhang.zhuijuguide.ui.GuideApp
import com.lzzhang.zhuijuguide.ui.GuideViewModel
import com.lzzhang.zhuijuguide.ui.theme.ZhuijuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZhuijuTheme {
                val guideViewModel: GuideViewModel = viewModel(
                    factory = GuideViewModel.Factory(applicationContext),
                )
                GuideApp(viewModel = guideViewModel)
            }
        }
    }
}
