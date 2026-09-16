package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.theme.VisualBookmarksTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.factory(application as VisualBookmarksApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VisualBookmarksTheme {
                HomeScreen(viewModel = homeViewModel)
            }
        }
    }
}

