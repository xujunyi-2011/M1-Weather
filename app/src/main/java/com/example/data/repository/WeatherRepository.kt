package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.local.CityDao
import com.example.data.local.CityEntity
import com.example.data.local.WeatherDatabase
import com.example.data.remote.GeocodingResult
import com.example.data.remote.OpenMeteoService
import com.example.data.remote.WeatherResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class WeatherRepository private constructor(
    context: Context
) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        WeatherDatabase::class.java,
        "weather_database"
    ).build()

    private val cityDao: CityDao = database.cityDao()

    val savedCities: Flow<List<CityEntity>> = cityDao.getAllCities()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val openMeteoService = Retrofit.Builder()
        // base URL is required by Retrofit build() even if absolute @GET routes are used
        .baseUrl("https://api.open-meteo.com/") 
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoService::class.java)

    suspend fun searchCity(name: String): List<GeocodingResult> {
        val response = openMeteoService.searchCity(name)
        return response.results ?: emptyList()
    }

    suspend fun getWeatherForecast(lat: Double, lon: Double): WeatherResponse {
        return openMeteoService.getWeatherForecast(lat, lon)
    }

    suspend fun saveCity(city: CityEntity) {
        cityDao.insertCity(city)
    }

    suspend fun deleteCity(id: Int) {
        cityDao.deleteById(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: WeatherRepository? = null

        fun getInstance(context: Context): WeatherRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = WeatherRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
