package com.example.mdpandroid.ui

import android.Manifest
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.mdpandroid.ui.theme.MDPAndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MDPAndroidTheme {
                val navController = rememberNavController()
                MainActivityContent(navController)
            }
        }
    }
}

@Composable
fun MainActivityContent(navController: NavHostController) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val bluetoothAdapter = bluetoothManager?.adapter ?: run {
        Toast.makeText(context, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show()
        return
    }

    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter.isEnabled) }

    // Bluetooth enable launcher
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // You can handle the result of enabling Bluetooth here if necessary
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(context, "Bluetooth is required for this app", Toast.LENGTH_LONG).show()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms[Manifest.permission.BLUETOOTH_SCAN] == true &&
                    perms[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                    perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        } else {
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }

        // If permissions are granted, prompt to enable Bluetooth
        if (canEnableBluetooth && !isBluetoothEnabled) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else if (!canEnableBluetooth) {
            Toast.makeText(context, "Bluetooth permissions are required", Toast.LENGTH_LONG).show()
        }
    }

    // Request permissions and enable Bluetooth on first launch
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }

        // Check Bluetooth status after permissions are granted
        withContext(Dispatchers.IO) {
            isBluetoothEnabled = bluetoothAdapter.isEnabled
            if (!isBluetoothEnabled) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }

    // Render the main UI of the app
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AppNavHost(navController = navController)
    }
}
