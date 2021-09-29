package org.booncode.bluepass4.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.booncode.bluepass4.PREF_SHOWCASE
import org.booncode.bluepass4.ui.theme.BluePass4Theme
import org.booncode.bluepass4.myDataStore

@Composable
fun <T> Pref(key: Preferences.Key<T>, initial: T, comp: (title: String, value: T) -> Unit)
{
    val context = LocalContext.current
    val read_sc = context.myDataStore.data.map {
        it[key]
    }.collectAsState(initial = initial)

    val scope = rememberCoroutineScope()
}

@Composable
fun StringPreferenceView(title: String, value: String, subtitle: String? = null) {
    val context = LocalContext.current
    val read_sc = context.myDataStore.data.map {
        it[PREF_SHOWCASE] ?: 0
    }.collectAsState(initial = 0)

    val scope = rememberCoroutineScope()

    GenericPreferenceView(
        title = title,
        comp = @Composable {
            Row {
                Text(
                    text = "${value ?: "<not set>"} - ${read_sc.value}",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .border(border = BorderStroke(width = 1.dp, color = Color.Black))
                )
                Button(
                    onClick = {
                        scope.launch {
                            context.myDataStore.edit {
                                val current = it[PREF_SHOWCASE] ?: 0
                                it[PREF_SHOWCASE] = current + 1
                            }
                        }
                    },
                ) {
                    Text(
                        text = "Click me"
                    )
                }
            }
        },
        subtitle = subtitle,
    )
}

@Composable
fun BooleanPreferenceView(
    title: String,
    value: Boolean,
    onChanged: (Boolean) -> Unit,
    subtitle: String? = null
) {
    GenericPreferenceView(
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

@Composable
fun GenericPreferenceView(
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

/* Preview */

@Preview(showBackground = true)
@Composable
fun PreviewExample() {
    var enabled = remember { mutableStateOf(false) }
    BluePass4Theme {
        Column {
            BooleanPreferenceView(
                title = "Enable filter",
                value = enabled.value,
                onChanged = { enabled.value = it },
                subtitle = "Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer Rababer"
            )
            StringPreferenceView(
                title = "Bluetooth Device",
                value = "Fairphone 3\n01:02:03:04:05:06",
                subtitle = "The currently configured bluetooth device"
            )
            StringPreferenceView(
                title = "Filter: Message sender",
                value = "<not set>",
                subtitle = "This regular expression is used to filter for valid senders of the messages that shall be parsed"
            )
            StringPreferenceView(
                title = "Filter: Message body",
                value = "<not set>",
                subtitle = "This regular expression is used to filter and parse the messages received by the specified senders. This regular expression has to contain exactly one group. This group has to match the code that shall be transfer via Bluetooth."
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewString() {
    var state = remember { mutableStateOf(true) }
    BluePass4Theme {
        StringPreferenceView(
            title = "bla",
            value = "bla",
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
