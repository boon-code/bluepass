package org.booncode.bluepass4

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.booncode.bluepass4.service.BlueService
import org.booncode.bluepass4.ui.theme.BluePass4Theme
import java.util.regex.Pattern
import org.booncode.bluepass4.R

class MainActivity : ComponentActivity() {
    var _manager: BluetoothManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        setContent {
            BluePass4Theme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    MainScreen(_manager?.adapter)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkSmsPermissions()
        checkBluetoothScanPermissions()
        checkBluetoothEnabled {
            Log.d(TAG, "Bluetooth adapter is ready")
        }
    }

    private fun checkSmsPermissions() {
        val perm = Manifest.permission.RECEIVE_SMS

        if (!isGranted(perm)) {
            val permissionRequest =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) {
                        Log.d(TAG, "SMS_RECEIVE permission was granted")
                    } else {
                        Log.w(TAG, "SMS_RECEIVE permission was not granted")
                        Toast.makeText(
                            this,
                            "SMS_RECEIVE permission not granted",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            permissionRequest.launch(perm)
        }
    }

    private fun checkBluetoothScanPermissions() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (!perms.all { isGranted(it) }) {
            val permissionRequest =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { it ->
                    var allGranted = true
                    for (i in it) {
                        if (i.value) {
                            Log.d(TAG, "Permission ${i.key} granted")
                        } else {
                            Log.w(TAG, "Permission ${i.key} denied")
                            allGranted = false
                        }
                    }
                    if (!allGranted) {
                        Toast.makeText(
                            this,
                            "Bluetooth scan not possible: Permissions have been denied",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            permissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun isGranted(permission: String): Boolean {
        return this.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkBluetoothEnabled(onBtAdapterReady: () -> Unit) {
        val adapter = _manager!!.adapter
        if (adapter == null) {
            Toast.makeText(this, "No bluetooth adapter is available", Toast.LENGTH_LONG).show()
        } else if (!adapter.isEnabled) {
            val request =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    when (it.resultCode) {
                        RESULT_OK -> {
                            onBtAdapterReady()
                        }
                        else -> {
                            // User did not enable Bluetooth or an error occurred
                            Log.d(TAG, "BT not enabled")
                            Toast.makeText(
                                this,
                                R.string.error_bt_adapter_not_enabled,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            finish()
                        }
                    }
                }
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            request.launch(enableIntent)
        } else {
            onBtAdapterReady()
        }
    }

    companion object {
        internal const val TAG = "MainActivity"
    }
}

@Composable
fun MainScreen(adapter: BluetoothAdapter?) {
    val context = LocalContext.current
    val btDevice = MyDataStore(context).btDeviceParams.collectAsState(initial = BtDeviceParams())
    val dialogOpen = remember { mutableStateOf(false) }

    if (dialogOpen.value) {
        ChooseBluetoothDeviceDialog(
            adapter = adapter,
            onDismissRequest = {
                dialogOpen.value = false
            }
        )
    } else {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier
                    .padding(all = 8.dp)
                    .border(1.dp, MaterialTheme.colors.primary)
                    .padding(all = 8.dp)
                    .fillMaxWidth()
            ) {
                MessageFilterSettings()
            }

            Column(
                modifier = Modifier
                    .padding(all = 8.dp)
                    .border(1.dp, MaterialTheme.colors.primary)
                    .padding(all = 8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.ui_main_current_bt_device_name).format(
                        btDevice.value.name ?: "<not set>"
                    )
                )
                Text(
                    text = stringResource(R.string.ui_main_current_bt_device_address).format(
                        btDevice.value.address ?: "<not set>"
                    )
                )
                Button(
                    onClick = { dialogOpen.value = true },
                    modifier = Modifier
                        .align(alignment = Alignment.CenterHorizontally)
                        .padding(vertical = 4.dp)
                ) {
                    Text(text = stringResource(R.string.ui_main_bt_scan_button))
                }
            }
        }
    }
}

@Composable
fun ChooseBluetoothDeviceDialog(
    adapter: BluetoothAdapter?,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        BluetoothDeviceListDialogView(
            adapter = adapter,
            onSelected = { onDismissRequest() },
            onCancel = onDismissRequest
        )
    }
}

@Composable
fun FakeSms() {
    val context = LocalContext.current
    val number = remember { mutableStateOf("123456") }
    Column {
        OutlinedTextField(
            value = number.value,
            onValueChange = { number.value = it }
        )
        Button(onClick = {
            val intent = Intent(context, BlueService::class.java)
            intent.putExtra(BlueService.INTENT_COMMAND, BlueService.CMD_PUSH_CODE)
            intent.putExtra(BlueService.INTENT_CODE, number.value)

            context.startForegroundService(intent)
        }) {
            Text(text = "Send fake message")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageFilterSettingsPreview() {
    BluePass4Theme {
        MessageFilterSettings()
    }
}

@Composable
fun MessageFilterSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ds = MyDataStore(context)
    val filterText = ds.msgFilterText.collectAsState(initial = MsgFilterText())
    val saveFilter: (MsgFilterText) -> Unit = {
        scope.launch {
            MyDataStore(context).run {
                setMsgFilterParams(it.sender_regex ?: "", it.message_regex ?: "")
                Toast.makeText(
                    context,
                    "Updated filter settings ${it.sender_regex}, ${it.message_regex}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column {
        MessageFilterView(
            filterText.value,
            onSave = saveFilter,
        )
    }
}

@Composable
fun MessageFilterView(
    msgFilterText: MsgFilterText,
    onSave: (MsgFilterText) -> Unit,
) {
    val sender = remember { mutableStateOf(msgFilterText.sender_regex ?: "") }
    val message = remember { mutableStateOf(msgFilterText.message_regex ?: "") }
    val isSaveEnabled = remember { mutableStateOf(false) }
    val tryMessagePattern = remember { mutableStateOf<Pattern?>(null) }
    val checkSaveEnabled = {
        val new_sender_pattern = sender.value
        val new_message_pattern = message.value
        val rx_sender = MyDataStore.tryCompilePattern(new_sender_pattern)
        tryMessagePattern.value = MyDataStore.tryCompilePattern(new_message_pattern)
        val patternValid = listOf(rx_sender, tryMessagePattern.value).all { it != null }
        val contentChanged =
            (msgFilterText.sender_regex != new_sender_pattern) || (msgFilterText.message_regex != new_message_pattern)
        isSaveEnabled.value = contentChanged && patternValid
    }
    val testMessage = remember { mutableStateOf("") }
    val parsedResult = remember { mutableStateOf("<no match>") }

    LaunchedEffect(key1 = msgFilterText.message_regex) {
        message.value = msgFilterText.message_regex ?: ""
    }
    LaunchedEffect(key1 = msgFilterText.sender_regex) {
        sender.value = msgFilterText.sender_regex ?: ""
    }
    LaunchedEffect(key1 = msgFilterText) {
        checkSaveEnabled()
    }
    LaunchedEffect(testMessage.value, message.value) {
        val pat = MyDataStore.tryCompilePattern(message.value)
        parsedResult.value = if (pat != null) {
            val m = pat.matcher(testMessage.value)
            if (!m.matches()) {
                "regular expression doesn't match"
            } else if (m.groupCount() >= 1) {
                val number = try {
                    m.group(1)
                } catch (e: Exception) {
                    null
                }
                if (number != null) {
                    "matched: ${number}"
                } else {
                    ""
                }
            } else {
                "invalid number of groups: ${m.groupCount()}"
            }
        } else {
            "invalid regular expression"
        }
    }
    Column {
        OutlinedTextField(
            value = sender.value,
            onValueChange = {
                sender.value = it
                checkSaveEnabled()
            },
            label = {
                Text(text = stringResource(R.string.ui_main_new_sender_regex_label))
            }
        )
        Text(
            text = stringResource(R.string.ui_main_new_sender_regex_desc),
            style = MaterialTheme.typography.overline,
            modifier = Modifier
                .padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))
        OutlinedTextField(
            value = message.value,
            onValueChange = {
                message.value = it
                checkSaveEnabled()
            },
            label = {
                Text(text = stringResource(R.string.ui_main_new_message_regex_label))
            }
        )
        Text(
            text = stringResource(R.string.ui_main_new_message_regex_desc),
            style = MaterialTheme.typography.overline,
            modifier = Modifier
                .padding(horizontal = 8.dp)
        )
        Column(modifier = Modifier.padding(all = 8.dp)) {
            OutlinedTextField(
                value = testMessage.value,
                onValueChange = { testMessage.value = it },
                label = {
                    Text(text = stringResource(R.string.ui_main_test_message_regex_label))
                }
            )
            Text(
                text = stringResource(R.string.ui_main_test_message_parse_result).format(
                    parsedResult.value
                )
            )
        }
        Spacer(modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onSave(MsgFilterText(sender.value, message.value)) },
                enabled = isSaveEnabled.value,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(text = stringResource(R.string.ui_main_save_filters_button))
            }
            Button(
                onClick = {
                    sender.value = msgFilterText.sender_regex ?: ""
                    message.value = msgFilterText.message_regex ?: ""
                    isSaveEnabled.value = false
                },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(text = stringResource(R.string.ui_main_reset_filters_button))
            }
        }
    }
}

@Composable
fun BluetoothDeviceListDialogView(
    adapter: BluetoothAdapter?,
    onSelected: (BtDeviceParams) -> Unit,
    onCancel: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(all = 16.dp)) {
            if (adapter != null) {
                BluetoothDeviceList(adapter = adapter, onSelected = onSelected)
            } else {
                Text(text = stringResource(R.string.ui_bt_dialog_no_adapter))
            }
            Button(onClick = onCancel) {
                Text(text = stringResource(R.string.ui_bt_dialog_cancel_button))
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    adapter: BluetoothAdapter,
    onSelected: (BtDeviceParams) -> Unit,
) {
    val discovering = remember { mutableStateOf(false) }
    val devList = remember { mutableStateListOf<BtDeviceParams>() }
    val bondedDevices = adapter.bondedDevices.map {
        BtDeviceParams(it.address, it.name)
    }
    val cancelDiscover = {
        discovering.value = false
        adapter.cancelDiscovery()
    }

    BluetoothBroadcasts(
        onDiscoverDone = {
            discovering.value = false
        },
        onNewDevice = { dev ->
            devList.add(BtDeviceParams(address = dev.address, name = dev.name))
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // UI
    Column {
        BtScanStatus(
            scanning = discovering.value,
            onRequestScan = {
                adapter.startDiscovery()
                discovering.value = true
            },
            onRequestCancel = {
                cancelDiscover()
            }
        )
        BluetoothDeviceView(
            onSelected = { dev: BtDeviceParams ->
                cancelDiscover()
                onSelected(dev)
                scope.launch {
                    MyDataStore(context).run {
                        setBtDeviceParams(dev.address!!, dev.name!!)

                        Toast.makeText(
                            context,
                            context.getString(R.string.ui_bt_dialog_set_device_toast).format(
                                dev.name,
                                dev.address
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            bondedDevices = bondedDevices,
            scannedDevices = devList
        )
    }
}

@Composable
fun BtScanStatus(scanning: Boolean, onRequestScan: () -> Unit, onRequestCancel: () -> Unit) {
    Column {
        if (scanning) {
            Text(text = "scanning ...")
            Button(onClick = onRequestCancel) {
                Text(text = "Cancel")
            }
        } else {
            Text(text = "scanning done")
            Button(onClick = onRequestScan) {
                Text(text = "Scan now")
            }
        }
    }
}

@Composable
fun BluetoothDeviceView(
    onSelected: (BtDeviceParams) -> Unit,
    bondedDevices: List<BtDeviceParams>,
    scannedDevices: List<BtDeviceParams>
) {
    Column {
        Text(text = "Bonded:")
        for (dev in bondedDevices) {
            BtItemView(dev = dev, onSelected = onSelected)
        }
        Text(text = "Scanned:")
        for (dev in scannedDevices) {
            BtItemView(dev = dev, onSelected = onSelected)
        }
    }
}

@Composable
fun BtItemView(dev: BtDeviceParams, onSelected: (BtDeviceParams) -> Unit) {
    Button(onClick = { onSelected(dev) }) {
        Row {
            Spacer(Modifier.width(8.dp))
            Column {
                Text(text = "name: ${dev.name}")
                Text(text = "address: ${dev.address}")
            }
        }
    }
}

@Composable
fun BluetoothBroadcasts(
    onDiscoverDone: () -> Unit,
    onNewDevice: (BluetoothDevice) -> Unit,
) {
    val context = LocalContext.current
    SystemBroadcastReceiver(
        systemAction = BluetoothDevice.ACTION_FOUND,
        onReceive = {
            Log.i("BluetoothBroadcasts", "Received a device")
            val dev = it?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if ((dev != null) && (dev.bondState != BluetoothDevice.BOND_BONDED)) {
                onNewDevice(dev)
            }
        }
    )
    SystemBroadcastReceiver(
        systemAction = BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
        onReceive = {
            Log.i("BluetoothBroadcasts", "Discover done")
            onDiscoverDone()
        }
    )
}

// Taken from https://developer.android.com/jetpack/compose/interop/interop-apis
@Composable
fun SystemBroadcastReceiver(
    systemAction: String,
    onReceive: (intent: Intent?) -> Unit
) {
    val context = LocalContext.current

    // HACK: Safely use the latest onSystemEvent lambda passed to the function
    val currentOnReceive by rememberUpdatedState(onReceive)

    DisposableEffect(context, systemAction) {
        val intentFilter = IntentFilter(systemAction)
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                onReceive(intent)
            }
        }

        context.registerReceiver(broadcastReceiver, intentFilter)

        // When the effect leaves the Composition, remove the callback
        onDispose {
            context.unregisterReceiver(broadcastReceiver)
        }
    }
}

