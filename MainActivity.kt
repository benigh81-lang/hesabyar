package com.example.kharjyar

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import com.example.kharjyar.data.ExpenseParser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KharjyarScreen() }
    }

    @Composable
    private fun KharjyarScreen() {
        var permissionGranted by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
        }
        var candidates by remember { mutableStateOf(emptyList<SmsCandidate>()) }
        var approved by remember { mutableStateOf(loadApproved()) }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionGranted = it
        }

        MaterialTheme(colorScheme = darkColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Scaffold { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("خرج‌یار", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("مدیریت هوشمند هزینه‌های شخصی", color = MaterialTheme.colorScheme.secondary)
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("تراکنش‌های تأییدشده", fontWeight = FontWeight.SemiBold)
                                Text("${approved.size}", style = MaterialTheme.typography.headlineMedium)
                                Text("موارد ثبت‌شده روی همین گوشی", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(
                            onClick = {
                                if (permissionGranted) candidates = readSms()
                                else permissionLauncher.launch(Manifest.permission.READ_SMS)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (permissionGranted) "بررسی پیامک‌های گوشی" else "اجازه خواندن پیامک‌ها")
                        }
                        Text("پیام‌های احتمالی خرید", fontWeight = FontWeight.Bold)
                        if (!permissionGranted) Text("مجوز پیامک فقط پس از تأیید شما درخواست می‌شود.")
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(candidates, key = { it.id }) { item ->
                                Card(shape = RoundedCornerShape(16.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(item.sender, fontWeight = FontWeight.Bold)
                                        Text(item.message.take(180))
                                        Text("مبلغ تشخیص‌داده‌شده: ${item.amount?.toString() ?: "نامشخص"}")
                                        Text(item.note, color = MaterialTheme.colorScheme.secondary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = {
                                                approved = (approved + item).distinctBy { it.id }
                                                saveApproved(approved)
                                                candidates = candidates.filterNot { it.id == item.id }
                                            }) { Text("تأیید و ثبت") }
                                            OutlinedButton(onClick = {
                                                candidates = candidates.filterNot { it.id == item.id }
                                            }) { Text("رد") }
                                        }
                                    }
                                }
                            }
                        }
                        if (candidates.isEmpty()) Text("برای شروع، دکمه بررسی پیامک‌های گوشی را بزن.")
                    }
                }
            }
        }
    }

    private fun saveApproved(items: List<SmsCandidate>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject()
                .put("id", item.id)
                .put("sender", item.sender)
                .put("message", item.message)
                .put("amount", item.amount ?: JSONObject.NULL)
                .put("note", item.note))
        }
        getSharedPreferences("kharjyar", MODE_PRIVATE).edit().putString("approved", arr.toString()).apply()
    }

    private fun loadApproved(): List<SmsCandidate> {
        val raw = getSharedPreferences("kharjyar", MODE_PRIVATE).getString("approved", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SmsCandidate(
                    o.getLong("id"), o.getString("sender"), o.getString("message"),
                    if (o.isNull("amount")) null else o.getLong("amount"), o.optString("note")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun readSms(): List<SmsCandidate> {
        val result = mutableListOf<SmsCandidate>()
        val cursor: Cursor? = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf("_id", "address", "body"),
            null, null, "date DESC"
        )
        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow("_id")
            val addressCol = c.getColumnIndexOrThrow("address")
            val bodyCol = c.getColumnIndexOrThrow("body")
            var count = 0
            while (c.moveToNext() && count < 250) {
                val body = c.getString(bodyCol) ?: continue
                val parsed = ExpenseParser.parse(body)
                if (parsed.isLikelyPurchase) {
                    result.add(SmsCandidate(
                        c.getLong(idCol), c.getString(addressCol) ?: "ناشناس",
                        body, parsed.amount, parsed.explanation
                    ))
                    count++
                }
            }
        }
        return result
    }
}

data class SmsCandidate(val id: Long, val sender: String, val message: String, val amount: Long?, val note: String)
