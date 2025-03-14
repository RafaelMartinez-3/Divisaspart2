package com.example.proyectodivisas.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApiService {
    @GET("{apiKey}/latest/{baseCurrency}")
    suspend fun getExchangeRates(
        @Path("apiKey") apiKey: String,
        @Path("baseCurrency") baseCurrency: String
    ): Response<ExchangeRateResponse>
}

// Modelo de respuesta de la API
data class ExchangeRateResponse(
    val conversion_rates: Map<String, Double>
)
