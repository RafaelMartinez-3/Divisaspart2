package com.example.proyectodivisas.data.sync

import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.proyectodivisas.data.ExchangeRateProvider
import com.example.proyectodivisas.data.local.DatabaseHelper
import com.example.proyectodivisas.data.remote.ExchangeRateApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ExchangeRateSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://v6.exchangerate-api.com/v6/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(ExchangeRateApiService::class.java)
            val response = service.getExchangeRates("47ff18d7fe38e581b16da45b", "USD")

            if (response.isSuccessful) {
                Log.d("ExchangeRateSyncWorker", "API Response: ${response.body()}") // Nuevo log
                response.body()?.conversion_rates?.let { rates ->
                    saveExchangeRates(rates)
                    Log.d("ExchangeRateSyncWorker", "Datos insertados correctamente en ContentProvider")
                }
                Result.success()
            } else {
                Log.e("ExchangeRateSyncWorker", "Error en la respuesta: ${response.code()} - ${response.errorBody()?.string()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("ExchangeRateSyncWorker", "Error general en la sincronización", e)
            Result.retry()
        }
    }


    private fun saveExchangeRates(rates: Map<String, Double>) {
        val contentResolver = applicationContext.contentResolver
        val currentDate = System.currentTimeMillis().toString()

        for ((currency, rate) in rates) {
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_CURRENCY, currency)
                put(DatabaseHelper.COLUMN_RATE, rate)
                put(DatabaseHelper.COLUMN_DATE, currentDate)
            }
            val uri = contentResolver.insert(ExchangeRateProvider.CONTENT_URI, values)
            if (uri == null) {
                Log.e("ExchangeRateSyncWorker", "Error al insertar en ContentProvider. URI: ${ExchangeRateProvider.CONTENT_URI}, valores: $values")
            } else {
                Log.d("ExchangeRateSyncWorker", "Insertado correctamente en URI: $uri")
            }

        }
    }
}
