package org.booncode.bluepass4.ui.playground

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.booncode.bluepass4.ui.theme.BluePass4Theme


@Composable
fun TestString(title: String, value: String, subtitle: String? = null) {
    AbstractTestView(
        title = title,
        comp = @Composable {
            Text(
                text = value ?: "<not set>",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(all = 8.dp)
                    .border(border = BorderStroke(width = 1.dp, color = Color.Black))
            )
        },
        subtitle = subtitle,
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAbstractString() {
    var state = remember { mutableStateOf(true) }
    BluePass4Theme {
        AbstractTestView(
            title = "bla",
            comp = @Composable {
                Switch(
                    checked = state.value,
                    onCheckedChange = { it -> state.value = it }
                )
            },
            subtitle = """
                Bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla!
                Bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla!
                Bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla!
                Bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla!
                Bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla!
            """.trimIndent()
        )
    }
}

@Composable
fun AbstractTestView(
    title: String,
    comp: @Composable () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(border = BorderStroke(1.dp, Color.Black))
    ) {
        Column(
            modifier = Modifier
                .weight(3.0f)
                .padding(all = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(end = 8.dp)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                        )
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(2.0f)
                //.padding(all = 8.dp)
                .align(alignment = Alignment.CenterVertically)
        ) {
            comp()
        }
    }
}

@Composable
fun TestBool(
    title: String,
    value: Boolean,
    onChanged: (Boolean) -> Unit,
    subtitle: String? = null
) {
    AbstractTestView(
        title = title,
        comp = @Composable() {
            Switch(
                checked = value,
                onCheckedChange = onChanged,
            )
        },
        subtitle = subtitle
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewTestOld() {
    var enabled = remember { mutableStateOf(false) }
    BluePass4Theme {
        Column {
            TestBool(
                title = "Enable filter",
                value = enabled.value,
                onChanged = { it -> enabled.value = it },
                subtitle = "Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer"
            )
            TestString(
                title = "Bluetooth Device",
                value = "Fairphone 3\n01:02:03:04:05:06",
                subtitle = "The currently configured bluetooth device"
            )
            TestString(
                title = "Filter: Message sender",
                value = "<not set>",
                subtitle = "This regular expression is used to filter for valid senders of the messages that shall be parsed"
            )
            TestString(
                title = "Filter: Message body",
                value = "<not set>",
                subtitle = "This regular expression is used to filter and parse the messages received by the specified senders. This regular expression has to contain exactly one group. This group has to match the code that shall be transfer via Bluetooth."
            )
        }
    }
}

data class DeviceData(val name: String, val address: String)

@Composable
fun BtDeviceView(devData: DeviceData?, onClick: () -> Unit) {
    val desc = if (devData == null) {
        "<not set>"
    } else {
        "${devData.name} (${devData.address})"
    }

    Row(
        modifier = Modifier
            .padding(all = 8.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = "Device:",
            color = MaterialTheme.colors.primary,
            modifier = Modifier
                .padding(all = 8.dp)
        )
        Spacer(modifier = Modifier.weight(1.0f))
        Text(
            text = desc,
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(all = 8.dp)
        )
    }
}

@Composable
fun StringPreference(
    pref: SharedPreferences,
    prefName: String,
    title: String,
    subtitle: String? = null
) {
    val value = pref.getString(prefName, null)
}

@Composable
fun PreferenceView(title: String, subtitle: String? = null, value: String? = null) {
    Row(Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "${title}:",
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                        )
                        .weight(0.7f),
                )
            }
        }
        Text(
            text = value ?: "<not set>",
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .border(border = BorderStroke(width = 1.dp, color = Color.Black))
                .padding(all = 8.dp)
        )
    }
}

@Composable
fun SettingsScreen(devData: DeviceData?) {
    BtDeviceView(
        devData = devData,
        onClick = {},
    )
    Text(
        text = "bla",
        textAlign = TextAlign.End
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewBtDevice() {
    BluePass4Theme {
        var num = remember { mutableStateOf(1) }
        BtDeviceView(
            devData = DeviceData("device${num.value}", "00:01:02:03:04:05"),
            onClick = { num.value += 1 }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPreferences() {
    BluePass4Theme {
        PreferenceView(
            title = "Some option",
            subtitle = """
                Bla asd asdia sdaisdjaisdjasidja sda sdas dasd a asdasdjgj asdasd
                asd ajsfijdais jasdjasid jaisd 
                some bla
            """.trimIndent(),
            value = "5"
        )
    }
}