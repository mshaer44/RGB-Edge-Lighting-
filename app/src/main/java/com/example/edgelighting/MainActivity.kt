package com.example.edgelighting

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgelighting.data.SettingsStore
import com.example.edgelighting.service.OverlayService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = SettingsStore(applicationContext)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00FF88),
                    secondary = Color(0xFF00F0FF),
                    background = Color(0xFF0B0D14),
                    surface = Color(0xFF141724),
                    onSurface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EdgeLightingDashboard(settingsStore)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeLightingDashboard(settingsStore: SettingsStore) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isEnabled by remember { mutableStateOf(true) }
    var thickness by remember { mutableFloatStateOf(6f) }
    var speed by remember { mutableFloatStateOf(1.2f) }
    var durationSec by remember { mutableFloatStateOf(3.5f) }
    var selectedStyle by remember { mutableStateOf("laser_comet") }

    val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RGB Edge Lighting",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "AMOLED Border Notification Engine",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = {
                    isEnabled = it
                    coroutineScope.launch { settingsStore.setMainEnabled(it) }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00FF88),
                    checkedTrackColor = Color(0xFF00FF88).copy(alpha = 0.4f)
                )
            )
        }

        // Permission Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161A29)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Required Permissions", fontWeight = FontWeight.SemiBold, color = Color.White)
                
                // Overlay Permission
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasOverlayPermission) Color(0xFF1E293B) else Color(0xFF2563EB)
                    )
                ) {
                    Icon(
                        if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasOverlayPermission) Color(0xFF10B981) else Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (hasOverlayPermission) "Overlay Permission Granted" else "Grant Display Over Other Apps")
                }

                // Notification Access
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(Modifier.width(8.dp))
                    Text("Configure Notification Listener Access")
                }
            }
        }

        // Live Test Button
        Button(
            onClick = {
                val intent = Intent(context, OverlayService::class.java).apply {
                    putExtra(OverlayService.EXTRA_PRIMARY_COLOR, 0xFF00FF88.toInt())
                    putExtra(OverlayService.EXTRA_SECONDARY_COLOR, 0xFF00F0FF.toInt())
                    putExtra(OverlayService.EXTRA_DURATION_MS, (durationSec * 1000).toLong())
                    putExtra(OverlayService.EXTRA_THICKNESS, thickness)
                    putExtra(OverlayService.EXTRA_SPEED, speed)
                    putExtra(OverlayService.EXTRA_ANIM_STYLE, selectedStyle)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text("Trigger Test Edge Glow", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        // Customization Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161A29)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Border Thickness: ${thickness.toInt()} px", color = Color.White, fontWeight = FontWeight.Medium)
                Slider(
                    value = thickness,
                    onValueChange = {
                        thickness = it
                        coroutineScope.launch { settingsStore.setThickness(it) }
                    },
                    valueRange = 2f..24f
                )

                Text("Animation Speed: ${String.format("%.1fx", speed)}", color = Color.White, fontWeight = FontWeight.Medium)
                Slider(
                    value = speed,
                    onValueChange = {
                        speed = it
                        coroutineScope.launch { settingsStore.setSpeed(it) }
                    },
                    valueRange = 0.5f..3.0f
                )

                Text("Duration: ${durationSec.toInt()}s", color = Color.White, fontWeight = FontWeight.Medium)
                Slider(
                    value = durationSec,
                    onValueChange = {
                        durationSec = it
                        coroutineScope.launch { settingsStore.setDuration((it * 1000).toLong()) }
                    },
                    valueRange = 1f..10f
                )
            }
        }
    }
}
