package com.characterfinder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.characterfinder.app.navigation.AppNavHost
import com.characterfinder.app.ui.theme.CharacterFinderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CharacterFinderTheme {
                AppNavHost()
            }
        }
    }
}
