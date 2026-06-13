package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CityEntity
import com.example.data.remote.GeocodingResult
import com.example.data.remote.WeatherResponse
import com.example.data.repository.WeatherRepository
import com.example.widget.WeatherWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(val weather: WeatherResponse) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

data class SearchUiState(
    val query: String = "",
    val results: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)

class WeatherViewModel(
    private val repository: WeatherRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _savedCities = MutableStateFlow<List<CityEntity>>(emptyList())
    val savedCities: StateFlow<List<CityEntity>> = _savedCities.asStateFlow()

    private val _selectedCity = MutableStateFlow<CityEntity?>(null)
    val selectedCity: StateFlow<CityEntity?> = _selectedCity.asStateFlow()

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val themePrefs = application.getSharedPreferences("weather_theme_prefs", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(themePrefs.getString("selected_theme", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        themePrefs.edit().putString("selected_theme", mode).apply()
    }

    private var searchJob: Job? = null

    init {
        // Collect saved cities reactively
        repository.savedCities
            .onEach { cities ->
                _savedCities.value = cities
                
                // If the selected city isn't set yet, or has been removed, select the first one!
                if (_selectedCity.value == null && cities.isNotEmpty()) {
                    selectCity(cities.first())
                } else if (_selectedCity.value == null && cities.isEmpty()) {
                    // Seed standard cities on first launch for a beautiful out-of-the-box user experience
                    seedDefaultCities()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun seedDefaultCities() {
        viewModelScope.launch {
            val defaultCities = listOf(
                CityEntity(1816670, "北京", "中国", "CN", 39.9042, 116.4074, "Asia/Shanghai", "北京"),
                CityEntity(2643743, "伦敦", "英国", "GB", 51.5074, -0.1278, "Europe/London", "英格兰"),
                CityEntity(1850147, "东京", "日本", "JP", 35.6762, 139.6503, "Asia/Tokyo", "东京"),
                CityEntity(5128581, "纽约", "美国", "US", 40.7128, -74.0060, "America/New_York", "纽约")
            )
            for (city in defaultCities) {
                repository.saveCity(city)
            }
        }
    }

    fun selectCity(city: CityEntity) {
        _selectedCity.value = city
        loadWeather(city)
    }

    fun loadWeather(city: CityEntity) {
        viewModelScope.launch {
            _weatherUiState.value = WeatherUiState.Loading
            try {
                val forecast = repository.getWeatherForecast(city.latitude, city.longitude)
                _weatherUiState.value = WeatherUiState.Success(forecast)
                
                // Real-time widget sync
                forecast.current?.let { current ->
                    val tempStr = "${current.temperature2m.toInt()}°C"
                    val descStr = getWeatherDescription(current.weatherCode)
                    val iconStr = getWeatherSymbol(current.weatherCode)
                    WeatherWidgetProvider.triggerUpdate(
                        getApplication(),
                        city.name,
                        tempStr,
                        descStr,
                        iconStr
                    )
                }
            } catch (e: Exception) {
                _weatherUiState.value = WeatherUiState.Error(e.localizedMessage ?: "获取天气数据失败")
            }
        }
    }

    fun refreshWeather() {
        _selectedCity.value?.let { loadWeather(it) }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchState.value = _searchState.value.copy(query = newQuery, error = null)
        
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _searchState.value = _searchState.value.copy(results = emptyList(), isSearching = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce API requests by 500ms
            performSearch(newQuery)
        }
    }

    fun performSearch(query: String) {
        viewModelScope.launch {
            _searchState.value = _searchState.value.copy(isSearching = true, error = null)
            try {
                val results = repository.searchCity(query)
                _searchState.value = _searchState.value.copy(
                    results = results,
                    isSearching = false
                )
            } catch (e: Exception) {
                _searchState.value = _searchState.value.copy(
                    isSearching = false,
                    error = e.localizedMessage ?: "搜索位置失败"
                )
            }
        }
    }

    fun saveGeocodingAsCity(geo: GeocodingResult) {
        viewModelScope.launch {
            val city = CityEntity(
                id = geo.id,
                name = geo.name,
                country = geo.country ?: "Unknown",
                countryCode = geo.countryCode ?: "UN",
                latitude = geo.latitude,
                longitude = geo.longitude,
                timezone = geo.timezone ?: "auto",
                admin1 = geo.admin1
            )
            repository.saveCity(city)
            selectCity(city)
            // Clear search after saving
            _searchState.value = SearchUiState()
        }
    }

    fun removeCity(cityId: Int) {
        viewModelScope.launch {
            repository.deleteCity(cityId)
            
            // If the deleted city is currently selected, select another city
            if (_selectedCity.value?.id == cityId) {
                val remainingCities = _savedCities.value.filter { it.id != cityId }
                if (remainingCities.isNotEmpty()) {
                    selectCity(remainingCities.first())
                } else {
                    _selectedCity.value = null
                    _weatherUiState.value = WeatherUiState.Idle
                }
            }
        }
    }

    class Factory(
        private val repository: WeatherRepository,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
                return WeatherViewModel(repository, application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
