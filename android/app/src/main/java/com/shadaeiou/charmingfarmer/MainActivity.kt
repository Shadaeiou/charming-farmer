package com.shadaeiou.charmingfarmer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shadaeiou.charmingfarmer.data.DownloadResult
import com.shadaeiou.charmingfarmer.data.Updater
import com.shadaeiou.charmingfarmer.ui.BirdwatchingScreen
import com.shadaeiou.charmingfarmer.ui.BreweryScreen
import com.shadaeiou.charmingfarmer.ui.FarmerTheme
import com.shadaeiou.charmingfarmer.ui.FishingScreen
import com.shadaeiou.charmingfarmer.ui.HomeScreen
import com.shadaeiou.charmingfarmer.ui.MalthouseScreen
import com.shadaeiou.charmingfarmer.ui.SettingsScreen
import com.shadaeiou.charmingfarmer.ui.WorldMapScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val KEY_LAST_DEST_META = "last_dest"

class MainActivity : ComponentActivity() {

    // Lint flags this on FragmentActivity hosts that pre-date fragment 1.3.0; we
    // extend ComponentActivity directly and never use Fragments, so the bug it
    // warns about can't apply here.
    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
        handleAutoUpdateIntent(intent)
        setContent {
            FarmerTheme {
                Surface(modifier = Modifier.fillMaxSize()) { Root() }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        // Android 13+ requires a runtime grant for POST_NOTIFICATIONS or FCM
        // pushes are silently dropped before they ever reach PushService.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAutoUpdateIntent(intent)
    }

    private fun handleAutoUpdateIntent(intent: Intent?) {
        if (intent == null) return
        if (!intent.getBooleanExtra(EXTRA_AUTO_UPDATE, false)) return
        intent.removeExtra(EXTRA_AUTO_UPDATE)
        startAutoUpdateFlow()
    }

    private fun startAutoUpdateFlow() {
        val appContext = applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, "Checking for update...", Toast.LENGTH_SHORT).show()
            }
            val updater = Updater(appContext)
            val info = runCatching {
                updater.checkForUpdate(BuildConfig.VERSION_CODE)
            }.getOrNull()

            if (info == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "You're already on the latest build.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    appContext,
                    "Downloading v${info.versionName}...",
                    Toast.LENGTH_SHORT,
                ).show()
            }

            val id = updater.startDownload(info)
            when (val result = updater.awaitDownload(id)) {
                DownloadResult.Success -> updater.launchInstall(id)
                is DownloadResult.Failure -> withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "Update failed: ${result.reason}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    companion object {
        const val EXTRA_AUTO_UPDATE = "auto_update"
    }
}

@Composable
private fun Root() {
    val context = LocalContext.current
    // Last-visited screen used to live in its own SharedPreferences blob;
    // it now rides along in system_meta so storage is fully unified.
    val db = remember { com.shadaeiou.charmingfarmer.data.room.AppDatabase.get(context.applicationContext) }
    // World map is the new home base for fresh installs. Existing players
    // resume on whichever screen they left.
    val startDest = db.systemMeta().get(KEY_LAST_DEST_META) ?: "map"

    fun saveLastDest(dest: String) {
        db.systemMeta().put(KEY_LAST_DEST_META, dest)
    }

    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDest) {
        composable("home") {
            saveLastDest("home")
            HomeScreen(
                onOpenSettings = { nav.navigate("settings") },
                onOpenMap = { nav.navigate("map") },
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenMap = { nav.navigate("map") },
            )
        }
        composable("map") {
            saveLastDest("map")
            WorldMapScreen(
                onNavigate = { route ->
                    saveLastDest(route)
                    nav.navigate(route)
                },
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable("pond") {
            saveLastDest("pond")
            FishingScreen(
                onBack = { nav.popBackStack() },
                onOpenMap = { nav.navigate("map") },
            )
        }
        composable("birds") {
            saveLastDest("birds")
            BirdwatchingScreen(
                onBack = { nav.popBackStack() },
                onOpenMap = { nav.navigate("map") },
            )
        }
        composable("malthouse") {
            saveLastDest("malthouse")
            MalthouseScreen(
                onBack = { nav.popBackStack() },
                onOpenMap = { nav.navigate("map") },
            )
        }
        composable("brewery") {
            saveLastDest("brewery")
            BreweryScreen(
                onBack = { nav.popBackStack() },
                onOpenMap = { nav.navigate("map") },
            )
        }
    }
}
