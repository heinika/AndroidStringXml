import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LanguagesRadioGroup(languages: List<String>, onSelectedChange: (List<String>) -> Unit) {
    val selectedOptions = remember { mutableStateListOf<String>() }

    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        languages.forEach { option ->
            val checked = remember { mutableStateOf(selectedOptions.contains(option)) }
            Checkbox(
                checked = checked.value,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        selectedOptions.add(option)
                        onSelectedChange(selectedOptions)
                    } else {
                        selectedOptions.remove(option)
                        onSelectedChange(selectedOptions)
                    }
                    checked.value = isChecked
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(text = option)
        }
    }
}


@Composable
fun LanguagesTable(
    languagesList: List<String>,
    xmlMap: Map<String, String>,
    onMapValueChange: (String, String) -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(languagesList[0]) }

    selectedLanguage = if (languagesList.contains(selectedLanguage)) selectedLanguage else languagesList[0]
    LazyColumn(Modifier.padding(16.dp)) {
        item(languagesList) {
            TabRow(
                selectedTabIndex = languagesList.indexOf(selectedLanguage),
                backgroundColor = Color.LightGray
            ) {
                languagesList.forEach { tabText ->
                    Tab(
                        selected = tabText == selectedLanguage,
                        onClick = {
                            selectedLanguage = tabText
                        }
                    ) {
                        Text(text = tabText)
                    }
                }
            }
        }

        item {
            val textFileModifier = Modifier.fillMaxWidth()
            xmlMap[selectedLanguage]?.let { xmlContent ->
                TextField(xmlContent, onValueChange = {
                    onMapValueChange(selectedLanguage, it)
                }, textFileModifier)
            }
        }
    }
}