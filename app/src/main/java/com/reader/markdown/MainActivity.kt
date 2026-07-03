package com.reader.markdown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reader.markdown.ui.screens.FileBrowserScreen
import com.reader.markdown.ui.screens.MarkdownViewerScreen
import com.reader.markdown.ui.theme.MarkdownReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarkdownReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "browser") {
                        composable("browser") {
                            FileBrowserScreen(
                                onFileSelected = { filePath ->
                                    navController.navigate("viewer/${java.net.URLEncoder.encode(filePath, "UTF-8")}")
                                }
                            )
                        }
                        composable("viewer/{filePath}") { backStackEntry ->
                            val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                            val filePath = java.net.URLDecoder.decode(encodedPath, "UTF-8")
                            MarkdownViewerScreen(
                                filePath = filePath,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}