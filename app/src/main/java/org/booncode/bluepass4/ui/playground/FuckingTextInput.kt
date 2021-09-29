package org.booncode.bluepass4.ui.playground

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.booncode.bluepass4.MsgFilterText
import org.booncode.bluepass4.MyDataStore
import org.booncode.bluepass4.ui.theme.BluePass4Theme

@Preview(showBackground = true)
@Composable
fun DataStoreBackedTextInputPreview() {
    BluePass4Theme {
        DataStoreBackedTextInput()
    }
}

@Composable
fun DataStoreBackedTextInput() {
    TextInputView()
}

@Composable
fun TextInputView() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val senderSaved = MyDataStore(context).senderFilter.collectAsState(initial = "XYZ")
    val current = remember { mutableStateOf<String?>(senderSaved.value) }

    LaunchedEffect(key1 = senderSaved) {
        current.value = senderSaved.value
    }
    Column {
        Text(text = "${current.value}, ${senderSaved.value}")
        OutlinedTextField(
            value = current.value ?: senderSaved.value,
            onValueChange = {
                current.value = it
            },
            label = {
                Text(text = "Regular expression for sender")
            },
        )
        Button(onClick = {
            scope.launch {
                val value = current.value
                if (value != null) {
                    MyDataStore(context).setMsgFilterSender(value)
                }
            }
        }) {
            Text(text = "Save")
        }
    }
}

