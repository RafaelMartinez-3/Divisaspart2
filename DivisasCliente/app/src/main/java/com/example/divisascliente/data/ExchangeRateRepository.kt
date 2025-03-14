package com.example.divisascliente.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

class ExchangeRateRepository(private val context: Context) {

    fun obtenerDatosDesdeContentProvider(
        moneda: String,
        fechaInicio: Long,
        fechaFin: Long
    ): List<Pair<String, Double>> {
        val uri = Uri.parse("content://com.example.proyectodivisas.provider/exchange_rates")
        val projection = arrayOf("fecha", "tipo_cambio")
        val selection = "moneda = ? AND fecha BETWEEN ? AND ?"
        val selectionArgs = arrayOf(moneda, fechaInicio.toString(), fechaFin.toString())

        val cursor: Cursor? = context.contentResolver.query(uri, projection, selection, selectionArgs, "fecha ASC")

        val listaDatos = mutableListOf<Pair<String, Double>>()

        cursor?.use {
            while (it.moveToNext()) {
                val fecha = it.getString(it.getColumnIndexOrThrow("fecha"))
                val tipoCambio = it.getDouble(it.getColumnIndexOrThrow("tipo_cambio"))

                // 🔍 Convertir la fecha al formato correcto
                val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val fechaTimestamp = formato.parse(fecha)?.time ?: 0

                listaDatos.add(Pair(fecha, tipoCambio))

                // 📌 Verificar en Logcat
                Log.d("ContentProvider", "Fecha: $fecha - Timestamp: $fechaTimestamp - Tipo de cambio: $tipoCambio")
            }
        }

        if (listaDatos.isEmpty()) {
            Log.w("ContentProvider", "⚠ No se encontraron datos para la moneda $moneda en el rango seleccionado.")
        }

        return listaDatos
    }
}
