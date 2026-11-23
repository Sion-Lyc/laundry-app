@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sion.laundry

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.load(this)
        Notifier.ensureChannel(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "laundry_check", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CheckWorker>(1, TimeUnit.DAYS).build()
        )
        setContent { MaterialTheme { App() } }
    }
}

val DEFAULTS = linkedMapOf("内衣" to 1, "袜子" to 1, "上衣" to 3, "裤子" to 5, "鞋子" to 30, "其他" to 7)

@Composable
fun App() {
    val ctx = LocalContext.current
    val items by Store.items.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Item?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        Notifier.check(ctx)
    }

    val sorted = items.sortedWith(
        compareByDescending<Item> { it.due }.thenByDescending { it.daysWorn.toDouble() / it.threshold }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("换洗提醒") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "添加") }
        }
    ) { pad ->
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("点右下角 + 添加你的第一件衣物")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sorted, key = { it.id }) { item ->
                    ItemCard(item,
                        onWash = { Store.wash(ctx, item) },
                        onEdit = { editing = item },
                        onDelete = { Store.remove(ctx, item) })
                }
            }
        }
    }

    if (showAdd) ItemDialog(null, { showAdd = false }) { n, c, t -> Store.add(ctx, n, c, t); showAdd = false }
    editing?.let { it ->
        ItemDialog(it, { editing = null }) { n, c, t -> Store.update(ctx, it, n, c, t); editing = null }
    }
}

@Composable
fun ItemCard(item: Item, onWash: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = if (item.due) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(item.category) })
            }
            Spacer(Modifier.height(6.dp))
            Text("已穿 ${item.daysWorn} 天 / 阈值 ${item.threshold} 天", color = color)
            LinearProgressIndicator(
                progress = { (item.daysWorn.toFloat() / item.threshold).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                color = color
            )
            Text(
                if (item.due) "⚠ 该换洗了！洗完点「换洗」重置" else "上次换洗：${item.lastWash}",
                style = MaterialTheme.typography.bodySmall, color = color
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onWash) { Text("换洗") }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除") }
            }
        }
    }
}

@Composable
fun ItemDialog(initial: Item?, onDismiss: () -> Unit, onSave: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var cat by remember { mutableStateOf(initial?.category ?: "内衣") }
    var th by remember { mutableStateOf((initial?.threshold ?: DEFAULTS["内衣"]!!).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加物品" else "编辑物品") },
        text = {
            Column {
              OutlinedTextField(name, { name = it }, label = { Text("名称（可自定义）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("分类", style = MaterialTheme.typography.labelMedium)
                DEFAULTS.keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { c ->
                            FilterChip(selected = cat == c, onClick = {
                                cat = c
                              if (initial == null) th = DEFAULTS[c].toString()
                            }, label = { Text(c) })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(th, { th = it.filter(Char::isDigit) }, label = { Text("换洗阈值（天）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
          TextButton(onClick = {
                val t = th.toIntOrNull() ?: 1
                if (name.isNotBlank() && t >= 1) onSave(name.trim(), cat, t)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
