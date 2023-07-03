import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.awt.FileDialog
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@Composable
@Preview
fun App() {
    var path by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    val translateLanguages = Language.values().toList().subList(1, Language.values().size).map { it.languageName }
    val selectedLanguage = remember { mutableStateListOf<String>() }
    val xmlMap = remember { mutableStateMapOf<String, String>().apply { translateLanguages.forEach { put(it, "") } } }
    val scrollState = rememberScrollState()
    var chatgptText by remember { mutableStateOf("点击上方按钮生成 chatgpt 提示语") }

    Column {
        Text(text = "导入的文件路径 $path", modifier = Modifier.padding(8.dp))
        Button(modifier = Modifier.padding(8.dp), onClick = {
            val fileDialog = FileDialog(ComposeWindow())
            fileDialog.isVisible = true
            val directory = fileDialog.directory
            val file = fileDialog.file
            if (directory != null && file != null) {
                path = "$directory$file"
            }
        }) {
            Text("选择要翻译的中文xml文件")
        }

        Row {
            Button(modifier = Modifier.padding(8.dp), onClick = {
                val file = File(path)

                try {
                    val content = file.readText()
                    contentText = content
                    chatgptText = "$contentText \n 将这段 xml 翻译成 ${selectedLanguage.joinToString(" ")}"
                    xmlMap[Language.CN.languageName] = content
                    println(content)
                } catch (e: Exception) {
                    contentText = "请先选择中文 xml 文件"
                    println("Failed to read the file: ${e.message}")
                }
            }) {
                Text("获取 Chatgpt 提示语")
            }

            val clipboardManager = LocalClipboardManager.current
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    clipboardManager.setText(
                        AnnotatedString(
                            "$contentText \n" +
                                    " 将这段 xml 翻译成 ${selectedLanguage.joinToString(" ")}"
                        )
                    )
                }
            ) {
                Text("Copy提示语")
            }

            Button(modifier = Modifier.padding(8.dp), onClick = {
                val xmlFile = File("/home/chenlijin/Downloads/strings.xml")
                val csvFile = File("/home/chenlijin/Downloads/output.csv")

                val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val cnDoc: Document = docBuilder.parse(xmlFile)
                val translateDocMap = hashMapOf<String, Document>().apply {
                    selectedLanguage.forEach { languageName ->
                        xmlMap[languageName]?.let {
                            val inputStream = ByteArrayInputStream(it.toByteArray())
                            put(languageName, docBuilder.parse(inputStream))
                        }
                    }
                }

                val nodeMap = hashMapOf<String, Map<String, String>>()
                translateDocMap.forEach { (languageName, document) ->
                    val nodeList: NodeList = document.getElementsByTagName("string")
                    val translateMap = hashMapOf<String, String>()
                    for (i in 0 until nodeList.length) {
                        val element = nodeList.item(i) as Element
                        val name = element.getAttribute("name")
                        val value = element.textContent.trim()
                        translateMap[name] = value
                    }
                    nodeMap[languageName] = translateMap
                }

                val cnResources: NodeList = cnDoc.getElementsByTagName("string")

                csvFile.bufferedWriter().use { writer ->
                    writer.write("stringId,中文,${selectedLanguage.joinToString(",")}\n") // CSV header

                    for (i in 0 until cnResources.length) {
                        val cnElement: Element = cnResources.item(i) as Element
                        val name = cnElement.getAttribute("name")
                        val cnValue = cnElement.textContent.trim()
                        val translateValues = arrayListOf<String>()
                        selectedLanguage.forEach { languageName ->
                            nodeMap[languageName]?.get(name).let { translateValue ->
                                if (translateValue != null) {
                                    translateValues.add(translateValue)
                                } else {
                                    translateValues.add("")
                                }
                            }
                        }
                        val translateValuesCsvContent = if (translateValues.isEmpty()) {
                            ""
                        } else {
                            "," + translateValues.joinToString(",") { "\"$it\"" }
                        }
                        writer.write("\"$name\",\"$cnValue\"${translateValuesCsvContent}\n")
                    }
                }

                println("XML to CSV conversion completed.")
            }) {
                Text("转换为 csv")
            }

            Button(modifier = Modifier.padding(8.dp), onClick = {
                val outputDir = File("/home/chenlijin/Downloads/output")
                outputDir.mkdirs()

                val valuesDir = File(outputDir, "values")
                valuesDir.mkdirs()

                Language.values().forEach { language ->
                    val valuesEnDir = File(outputDir, language.path)
                    valuesEnDir.mkdirs()
                    val file = File(valuesEnDir, "string.xml")
                    xmlMap[language.languageName]?.let {
                        file.writeText(it)
                    }
                }

                println("Folders created successfully.")
            }) {
                Text("导出所有语言的xml")
            }
        }

        LanguagesRadioGroup(translateLanguages, onSelectedChange = { selectedList ->
            selectedLanguage.clear()
            selectedLanguage.addAll(selectedList.sortedBy { translateLanguages.indexOf(it) })
        })

        if (selectedLanguage.size > 0) {
            LanguagesTable(selectedLanguage, xmlMap, onMapValueChange = { language, xml ->
                xmlMap[language] = xml
            })
        }

        Column(
            modifier = Modifier.padding(8.dp).verticalScroll(scrollState),
            content = {
                TextField(value = chatgptText, onValueChange = {
                    chatgptText = it
                })
            }
        )


    }
}


fun main() = application {
    Window(onCloseRequest = ::exitApplication, icon = painterResource("coffee_dog.jpeg"), title = "AndroidStringXml") {
        App()
    }
}
