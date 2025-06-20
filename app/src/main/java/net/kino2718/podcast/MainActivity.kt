package net.kino2718.podcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.kino2718.podcast.ui.theme.PodCastTheme
import net.kino2718.podcast.ui.NavItem
import net.kino2718.podcast.ui.home.HomeScreen
import net.kino2718.podcast.ui.search.SearchScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PodCastTheme {
                var current by rememberSaveable { mutableStateOf(NavItem.HOME) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavItem.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = current == item,
                                    onClick = { current = item },
                                    icon = {
                                        androidx.compose.material3.Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (current) {
                        NavItem.HOME -> HomeScreen(Modifier.padding(innerPadding))
                        NavItem.SEARCH -> SearchScreen(Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    PodCastTheme {
        HomeScreen()
    }
}