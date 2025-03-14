package com.example.proyectodivisas

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.proyectodivisas.data.local.DatabaseHelper
import com.example.proyectodivisas.data.sync.WorkManagerSetup

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        fun logDatabaseContents(context: Context) {
            val dbHelper = DatabaseHelper(context)
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM exchange_rates", null)

            if (cursor.moveToFirst()) {
                do {
                    val currency = cursor.getString(cursor.getColumnIndexOrThrow("currency"))
                    val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                    val rate = cursor.getDouble(cursor.getColumnIndexOrThrow("rate"))
                    Log.d("DB_CHECK", "Moneda: $currency, Fecha: $date, Tasa: $rate")
                } while (cursor.moveToNext())
            } else {
                Log.d("DB_CHECK", "No hay datos en la base de datos.")
            }
            cursor.close()
        }

        super.onCreate(savedInstanceState)

        // Iniciar la sincronización de datos cada hora
        WorkManagerSetup.scheduleWork(applicationContext)

        setContent {
            // Aquí irá la UI de la app
        }
    }
}
