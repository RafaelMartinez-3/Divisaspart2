package com.example.divisascliente

import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var selectedCurrency by remember { mutableStateOf("") }
    var availableCurrencies by remember { mutableStateOf(listOf<String>()) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var exchangeRates by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Obtener lista de monedas al iniciar
    LaunchedEffect(Unit) {
        availableCurrencies = getAvailableCurrencies(context)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Selecciona una moneda:")
        DropdownMenuSample(availableCurrencies) { selectedCurrency = it }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Selecciona la fecha de inicio:")
        DatePickerButton("Inicio", startDate) { startDate = it }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Selecciona la fecha de fin:")
        DatePickerButton("Fin", endDate) { endDate = it }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (selectedCurrency.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty()) {
                isLoading = true
                exchangeRates = getExchangeRates(context, selectedCurrency, startDate, endDate)
                isLoading = false
            }
        }) {
            Text("Consultar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (exchangeRates.isNotEmpty()) {
            ExchangeRateChart(exchangeRates)
        } else {
            Text("⚠️ No hay datos disponibles para la moneda y fechas seleccionadas.")
        }
    }
}

@Composable
fun ExchangeRateChart(exchangeRates: List<Pair<String, Double>>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Variación del tipo de cambio",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        AndroidView(factory = { context ->
            LineChart(context).apply {
                val entries = exchangeRates.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.second.toFloat())
                }

                val dataSet = LineDataSet(entries, "Tipo de Cambio").apply {
                    color = android.graphics.Color.BLUE
                    valueTextColor = android.graphics.Color.BLACK
                    lineWidth = 4f
                    circleRadius = 6f
                    setDrawCircleHole(false)
                    setDrawValues(false)
                    setDrawFilled(true)
                    fillColor = android.graphics.Color.CYAN
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }

                data = LineData(dataSet)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textSize = 12f
                    granularity = 1f
                    setDrawGridLines(false)
                    labelRotationAngle = -45f
                    valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return exchangeRates.getOrNull(value.toInt())?.first ?: ""
                        }
                    }
                }

                axisLeft.apply {
                    textSize = 12f
                    setDrawGridLines(true)
                    granularity = 0.1f
                }
                axisRight.isEnabled = false

                description = Description().apply {
                    text = "Histórico de la moneda seleccionada"
                    textSize = 12f
                }

                legend.textSize = 14f

                setBackgroundColor(android.graphics.Color.WHITE)
                setNoDataText("No hay datos disponibles")

                invalidate()
            }
        }, modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .padding(8.dp))
    }
}

@Composable
fun DatePickerButton(label: String, date: String, onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    Button(onClick = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }) {
        Text(text = if (date.isEmpty()) "Seleccionar $label" else date)
    }
}

@Composable
fun DropdownMenuSample(items: List<String>, onItemSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf(items.firstOrNull() ?: "Seleccionar moneda") }

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true }) {
            Text(selectedItem)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(text = currency) },
                    onClick = {
                        selectedItem = currency
                        expanded = false
                        onItemSelected(currency)
                    }
                )
            }
        }
    }
}

fun getExchangeRates(
    context: Context,
    currency: String,
    startDate: String,
    endDate: String
): List<Pair<String, Double>> {
    val resolver: ContentResolver = context.contentResolver
    val uri = Uri.parse("content://com.example.proyectodivisas.provider/exchange_rates")

    val startDateTimestamp = convertDateToTimestamp(startDate)
    val endDateTimestamp = convertDateToTimestamp(endDate)

    val selection = "currency = ? AND date BETWEEN ? AND ?"
    val selectionArgs = arrayOf(currency, startDateTimestamp.toString(), endDateTimestamp.toString())

    Log.d("Consulta", "Ejecutando consulta con Moneda: $currency, Inicio: $startDateTimestamp, Fin: $endDateTimestamp")

    val cursor: Cursor? = resolver.query(uri, arrayOf("date", "rate"), selection, selectionArgs, "date ASC")

    val exchangeRates = mutableListOf<Pair<String, Double>>()

    cursor?.use {
        val dateIndex = it.getColumnIndexOrThrow("date")
        val rateIndex = it.getColumnIndexOrThrow("rate")

        while (it.moveToNext()) {
            val timestamp = it.getLong(dateIndex)
            val date = convertTimestampToDate(timestamp)
            val rate = it.getDouble(rateIndex)
            exchangeRates.add(Pair(date, rate))
        }
    }

    return exchangeRates
}

fun getAvailableCurrencies(context: Context): List<String> {
    val resolver: ContentResolver = context.contentResolver
    val uri = Uri.parse("content://com.example.proyectodivisas.provider/exchange_rates")
    val cursor: Cursor? = resolver.query(uri, arrayOf("currency"), null, null, null)
    val currencies = mutableListOf<String>()

    cursor?.use {
        while (it.moveToNext()) {
            val currency = it.getString(it.getColumnIndexOrThrow("currency"))
            currencies.add(currency)
        }
    }

    return currencies
}

fun convertDateToTimestamp(date: String): Long {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.parse(date)?.time ?: 0
}

fun convertTimestampToDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
