package org.booncode.bluepass4

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.booncode.bluepass4.service.BlueService
import org.booncode.bluepass4.ui.theme.BluePass4Theme
import java.util.regex.Pattern
import androidx.compose.animation.core.animateValue

class MainActivity : ComponentActivity() {
    private var _manager: BluetoothManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        setContent {
            BluePass4Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    color = MaterialTheme.colors.background,
                    contentColor = MaterialTheme.colors.onBackground
                ) {
                    MainScreen(_manager?.adapter)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestPermissionsIfNecessary()
        checkBluetoothEnabled {
            Log.d(TAG, "Bluetooth adapter is ready")
        }
    }

    private fun requestPermissionsIfNecessary() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECEIVE_SMS
        )
        if (!perms.all { isGranted(it) }) {
            val permissionRequest =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
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
                            getString(R.string.main_ui_missing_permissions_toast),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            permissionRequest.launch(perms)
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
                            ).show()
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

enum class MainDialog {
    CHOOSE_BLUETOOTH_DEVICE,
    FILTER_SETTINGS,
    CLOSED,
}

@Composable
fun MainScreen(adapter: BluetoothAdapter?) {
    val openDialog = remember { mutableStateOf(MainDialog.CLOSED) }

    when (openDialog.value) {
        MainDialog.CHOOSE_BLUETOOTH_DEVICE -> {
            ChooseBluetoothDeviceDialog(
                adapter = adapter,
                onDismissRequest = {
                    openDialog.value = MainDialog.CLOSED
                }
            )
        }
        MainDialog.FILTER_SETTINGS -> {
            MessageFilterSettingsDialog(onDismissRequest = {
                openDialog.value = MainDialog.CLOSED
            })
        }
        MainDialog.CLOSED -> {
        }
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier
                .padding(all = 8.dp)
                .border(1.dp, MaterialTheme.colors.primary)
                .padding(all = 8.dp)
                .fillMaxWidth()
        ) {
            MessageFilterOverview()
            Button(
                onClick = {
                    openDialog.value = MainDialog.FILTER_SETTINGS
                },
                modifier = Modifier
                    .padding(all = 8.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(text = stringResource(R.string.ui_main_change_filter_settings))
            }
        }
        Column(
            modifier = Modifier
                .padding(all = 8.dp)
                .border(1.dp, MaterialTheme.colors.primary)
                .padding(all = 8.dp)
                .fillMaxWidth()
        ) {
            BluetoothDeviceOverview()
            Button(
                onClick = { openDialog.value = MainDialog.CHOOSE_BLUETOOTH_DEVICE },
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(vertical = 4.dp)
            ) {
                Text(text = stringResource(R.string.ui_main_bt_scan_button))
            }
        }
        //CodeListView()
    }
}

@Composable
fun CodeListView() {
    Column(
        modifier = Modifier
            .padding(all = 8.dp)
            .border(1.dp, MaterialTheme.colors.primary)
            .padding(all = 8.dp)
            .fillMaxWidth()
    ) {
        CodeView("123456")
        CodeView("654321")
        CodeView("987654")
        CodeView("456789")
        CodeView("000001")
        CodeView("000002")
        CodeView("000003")
        CodeView("000004")
        /*
        CodeView("000005")
        CodeView("000006")
        CodeView("000007")
        CodeView("000008")
        CodeView("000009")
        CodeView("00000A")
        CodeView("00000B")
        CodeView("00000C")
        CodeView("00000D")
        CodeView("00000E")
        CodeView("00000F")
        CodeView("000010")
         */
    }
}

@Preview(showBackground = true)
@Composable
fun CodeListViewPreview() {
    MaterialTheme {
        CodeListView()
    }
}

@Composable
fun CodeView(code: String) {
    val context = LocalContext.current
    val cb = {
        Toast.makeText(context, "Clicked $code", Toast.LENGTH_SHORT).show()
    }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .padding(all = 8.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = code,
        )
        Button(onClick = { cb() }) {
            Text(text = "Send again")
        }
        Button(onClick = { cb() }) {
            Text(text = "Copy code")
        }
    }
}

@Composable
fun ChooseBluetoothDeviceDialog(
    adapter: BluetoothAdapter?,
    onDismissRequest: () -> Unit,
) {
    ResizableDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        BluetoothDeviceListDialogView(
            adapter = adapter,
            onSelected = { onDismissRequest() },
            onCancel = onDismissRequest
        )
    }
}

@Composable
fun TestSmsReceive(code: String, modifier: Modifier) {
    val context = LocalContext.current
    Button(
        onClick = {
            context.startForegroundService(
                Intent(context, BlueService::class.java).let {
                    it.putExtra(BlueService.INTENT_COMMAND, BlueService.CMD_PUSH_CODE)
                    it.putExtra(BlueService.INTENT_CODE, code)
                }
            )
        },
        enabled = code.isNotEmpty(),
        modifier = modifier
    ) {
        Text(text = stringResource(R.string.ui_main_test_send_code))
    }
}

@Composable
fun MessageFilterOverview() {
    val context = LocalContext.current
    val filterText = MyDataStore(context).msgFilterText.collectAsState(initial = MsgFilterText())
    TwoColumnLabel(
        label = "Sender pattern:",
        value = filterText.value.sender_regex ?: ""
    )
    TwoColumnLabel(
        label = "Content pattern:",
        value = filterText.value.message_regex ?: ""
    )
}

@Composable
fun BluetoothDeviceOverview() {
    val context = LocalContext.current
    val btDevice = MyDataStore(context).btDeviceParams.collectAsState(initial = BtDeviceParams())
    TwoColumnLabel(
        label = stringResource(id = R.string.ui_main_current_bt_device_name),
        value = btDevice.value.name ?: "<not set>"
    )
    TwoColumnLabel(
        label = stringResource(R.string.ui_main_current_bt_device_address),
        value = btDevice.value.address ?: "<not set>"
    )
}

@Composable
fun TwoColumnLabel(label: String, value: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .fillMaxWidth()
    ) {

        Text(
            text = label,
        )
        Text(
            text = value,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
fun MessageFilterSettingsDialog(onDismissRequest: () -> Unit) {
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
        onDismissRequest()
    }

    ResizableDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp)
        ) {
            MessageFilterView(
                filterText.value,
                onSave = saveFilter,
                onCancel = onDismissRequest,
            )
        }
    }
}

@Composable
fun MessageFilterView(
    msgFilterText: MsgFilterText,
    onSave: (MsgFilterText) -> Unit,
    onCancel: () -> Unit,
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
    val testMessage = rememberSaveable { mutableStateOf("") }
    val parsedResult = remember { mutableStateOf("<no match>") }
    val testCode = remember { mutableStateOf("") }

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
        val result = parseCode(pat, testMessage.value)
        parsedResult.value = if (result.code != null) {
            "matched: ${result.code}"
        } else {
            result.error
        }
        testCode.value = result.code ?: ""
    }
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
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
            TestSmsReceive(
                code = testCode.value,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .align(alignment = Alignment.End)
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
            Button(
                onClick = onCancel,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(text = stringResource(R.string.ui_main_cancel_filter_button))
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
    Column(modifier = Modifier.padding(all = 8.dp)) {
        if (adapter != null) {
            BluetoothDeviceList(adapter = adapter, onSelected = onSelected)
        } else {
            Text(text = stringResource(R.string.ui_bt_dialog_no_adapter))
        }
        Spacer(Modifier.padding(all = 8.dp))
        Button(onClick = onCancel) {
            Text(text = stringResource(R.string.ui_bt_dialog_cancel_button))
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
    BluetoothDeviceChoiceView(
        bondedDevices = bondedDevices,
        scannedDevices = devList,
        scanning = discovering.value,
        onRequestScan = {
            adapter.startDiscovery()
            discovering.value = true
        },
        onCancel = {
            cancelDiscover()
        },
        onSelected = { dev: BtDeviceParams ->
            if (dev.address == null) {
                Log.e(MainActivity.TAG, "Null address selected -> skipping")
                Toast.makeText(
                    context,
                    context.getString(R.string.ui_bt_dialog_set_device_failed_toast)
                        .format(dev.name),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                cancelDiscover()
                onSelected(dev)
                scope.launch {
                    MyDataStore(context).run {
                        setBtDeviceParams(dev.address, dev.name ?: "")

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
            }
        },
        onStartBonding = { dev ->
            if (dev.address != null) {
                context.startForegroundService(
                    Intent(context, BlueService::class.java).let {
                        it.putExtra(BlueService.INTENT_COMMAND, BlueService.CMD_PAIR_BACKGROUND)
                        it.putExtra(BlueService.INTENT_ADDRESS, dev.address)
                    }
                )
            }
        }
    )
}

@Composable
fun BluetoothDeviceChoiceView(
    bondedDevices: List<BtDeviceParams>,
    scannedDevices: List<BtDeviceParams>,
    scanning: Boolean,
    onRequestScan: () -> Unit = {},
    onCancel: () -> Unit = {},
    onSelected: (BtDeviceParams) -> Unit = {},
    onStartBonding: (BtDeviceParams) -> Unit = {},
) {
    Column {
        BtScanView(
            scanning = scanning,
            onRequestScan = onRequestScan,
            onRequestCancel = onCancel
        )
        Divider(color = MaterialTheme.colors.onSecondary)
        Spacer(Modifier.height(8.dp))
        Divider(color = MaterialTheme.colors.onSecondary)
        BluetoothDeviceListView(
            title = "Paired devices",
            onSelected = onSelected,
            devices = bondedDevices,
        )
        BluetoothDeviceListView(
            title = "New devices",
            onSelected = { dev ->
                onSelected(dev)
                onStartBonding(dev)
            },
            devices = scannedDevices,
        )
        if (!scanning) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onRequestScan() },
                color = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                elevation = 5.dp
            ) {
                Text(
                    text = "Search for more devices ...",
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BtScanningAnimationView(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.None,
    contentDescription: String? = null,
) {
    val painterStates =
        listOf(
            R.drawable.btsearch0,
            R.drawable.btsearch1,
            R.drawable.btsearch2,
            R.drawable.btsearch3,
            R.drawable.btsearch4,
        ).map { painterResource(id = it) }
    val infiniteTransition = rememberInfiniteTransition()
    val index = infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = painterStates.size,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Image(
        painter = painterStates[index.value],
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

@Composable
fun BtScanView(
    scanning: Boolean,
    onRequestScan: () -> Unit,
    onRequestCancel: () -> Unit
) {
    val imageModifier = Modifier
        .height(50.dp)
        .padding(all = 8.dp)

    val scanViewText = derivedStateOf {
        if (scanning) {
            "Scanning for devices ..."
        } else {
            "Select a device"
        }
    }

    Surface(
        contentColor = MaterialTheme.colors.onSecondary,
        color = MaterialTheme.colors.secondary,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (scanning) {
                BtScanningAnimationView(
                    contentScale = ContentScale.FillHeight,
                    modifier = imageModifier.clickable {
                        onRequestCancel()
                    },
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.btsearchoff),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = imageModifier.clickable {
                        onRequestScan()
                    },
                )
            }
            Text(
                text = scanViewText.value,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
            )
            Switch(
                checked = scanning,
                onCheckedChange = {
                    if (it) {
                        onRequestScan()
                    } else {
                        onRequestCancel()
                    }
                },
                modifier = Modifier
                    .padding(all = 8.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun BluetoothDeviceChoiceViewPreview() {
    val discovering = remember { mutableStateOf(false) }
    val context = LocalContext.current

    BluePass4Theme {
        Surface(
            color = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface,
        ) {
            BluetoothDeviceChoiceView(
                bondedDevices = listOf(
                    BtDeviceParams("01:02:03:04:05:06", "My device 1"),
                    BtDeviceParams("F1:E2:D3:04:05:06", "My device 2"),
                ),
                scannedDevices = listOf(
                    BtDeviceParams("FE:DC:03:04:5B:A6", "New device 1"),
                ),
                scanning = discovering.value,
                onRequestScan = { discovering.value = true },
                onCancel = { discovering.value = false },
                onSelected = {
                    Toast.makeText(
                        context,
                        "Selected: ${it.name} ${it.address}",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    discovering.value = false
                }
            )
        }
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
fun BluetoothDeviceListView(
    title: String,
    onSelected: (BtDeviceParams) -> Unit,
    devices: List<BtDeviceParams>,
) {
    if (devices.isEmpty()) {
        return
    }

    Surface(
        color = MaterialTheme.colors.secondary,
        contentColor = MaterialTheme.colors.onSecondary,
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Divider(
        thickness = 1.dp,
        color = MaterialTheme.colors.onSecondary
    )

    Spacer(modifier = Modifier.height(2.dp))

    /*
    Divider(
        thickness = 1.dp,
        color = MaterialTheme.colors.primary
    )
     */

    for (dev in devices) {
        BtItemView(dev = dev, onSelected = onSelected)
        Spacer(Modifier.height(1.dp))
    }
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
fun BtItemView(dev: BtDeviceParams, onSelected: (BtDeviceParams) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        elevation = 5.dp
    ) {
        Column(
            Modifier
                .clickable { onSelected(dev) }
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
        ) {
            Text(text = dev.name ?: "<not set>")
            Text(text = dev.address ?: "<not set>")
        }
    }
}

@Composable
fun BluetoothBroadcasts(
    onDiscoverDone: () -> Unit,
    onNewDevice: (BluetoothDevice) -> Unit,
) {
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

// Taken from https://stackoverflow.com/questions/68818202/animatedvisibility-doesnt-expand-height-in-dialog-in-jetpack-compose/68818540#68818540
// Reported there: https://issuetracker.google.com/issues/194911971?pli=1
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ResizableDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    comp: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .wrapContentHeight()
                .animateContentSize()
                .fillMaxWidth(0.9f)
        ) {
            Surface(
                modifier = Modifier.border(1.dp, MaterialTheme.colors.primary),
                color = MaterialTheme.colors.background,
                contentColor = MaterialTheme.colors.onBackground,
            ) {
                comp()
            }
        }
    }
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

