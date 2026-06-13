package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_cities")
data class CityEntity(
    @PrimaryKey val id: Int, // Open-Meteo city id, unique
    val name: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val admin1: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
