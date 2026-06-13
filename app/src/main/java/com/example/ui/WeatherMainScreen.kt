package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CityEntity
import com.example.data.remote.CurrentForecast
import com.example.data.remote.DailyForecast
import com.example.data.remote.GeocodingResult
import com.example.data.remote.HourlyForecast
import com.example.data.remote.WeatherResponse
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherMainScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val savedCities by viewModel.savedCities.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val weatherUiState by viewModel.weatherUiState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.safeDrawing
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Text(
                            text = "天气设置与详情",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // A. Search Panel
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "搜索全球城市",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                // Search field
                                TextField(
                                    value = searchState.query,
                                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                                    placeholder = { Text("输入城市名...", fontSize = 13.sp) },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "搜索",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchState.query.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "清除",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        viewModel.performSearch(searchState.query)
                                        focusManager.clearFocus()
                                    }),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("drawer_search_input")
                                )

                                if (searchState.isSearching) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }

                                searchState.error?.let { err ->
                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                if (searchState.results.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("搜索结果：", fontSize = 11.sp, color = Color.Gray)
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        searchState.results.forEach { result ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.saveGeocodingAsCity(result)
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                        scope.launch { drawerState.close() }
                                                    }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(getCountryFlagEmoji(result.countryCode) + "  ")
                                                Column {
                                                    Text(result.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                    Text(
                                                        text = "${result.admin1 ?: ""}, ${result.country ?: ""}",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // B. Saved Cities Section
                    if (savedCities.isNotEmpty()) {
                        item {
                            Text(
                                text = "已保存的城市",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    savedCities.forEach { city ->
                                        val isSelected = selectedCity?.id == city.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                    else Color.Transparent
                                                )
                                                .clickable {
                                                    viewModel.selectCity(city)
                                                    scope.launch { drawerState.close() }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Text(getCountryFlagEmoji(city.countryCode) + "  ")
                                                Text(
                                                    text = city.name,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            if (savedCities.size > 1) {
                                                IconButton(
                                                    onClick = { viewModel.removeCity(city.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "删除",
                                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // C. Weather breakdown details
                    when (val state = weatherUiState) {
                        is WeatherUiState.Success -> {
                            val hourly = state.weather.hourly
                            val daily = state.weather.daily
                            
                            if (hourly != null) {
                                item {
                                    HourlyForecastCard(hourly = hourly)
                                }
                            }
                            if (daily != null) {
                                item {
                                    DailyForecastCard(daily = daily)
                                }
                            }
                        }
                        else -> {}
                    }

                    // D. Theme Selector settings
                    item {
                        ThemeSelectorCard(viewModel = viewModel)
                    }
                }
            }
        }
    ) {
        val currentCode = if (weatherUiState is WeatherUiState.Success) (weatherUiState as WeatherUiState.Success).weather.current?.weatherCode else null
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = getWeatherGradient(code = currentCode, isDark = isDarkTheme))
        ) {
            // Scaffold background must be completely transparent to reveal the gradients
            Scaffold(
                containerColor = Color.Transparent,
                modifier = modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = selectedCity?.name ?: "M1 天气",
                                    color = if (isDarkTheme) Color.White else Color(0xFF1D1B20),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "实时更新",
                                    color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF49454F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "打开菜单",
                                    tint = if (isDarkTheme) Color.White else Color(0xFF1D1B20)
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.refreshWeather() },
                                modifier = Modifier.testTag("refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新天气",
                                    tint = if (isDarkTheme) Color.White else Color(0xFF1D1B20)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = if (isDarkTheme) Color.White else Color(0xFF1D1B20),
                            navigationIconContentColor = if (isDarkTheme) Color.White else Color(0xFF1D1B20),
                            actionIconContentColor = if (isDarkTheme) Color.White else Color(0xFF1D1B20)
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    when (val state = weatherUiState) {
                        is WeatherUiState.Idle -> {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "正在定位或加载默认天气数据...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF1D1B20).copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        is WeatherUiState.Loading -> {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        is WeatherUiState.Success -> {
                            val current = state.weather.current
                            val daily = state.weather.daily

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .wrapContentHeight(Alignment.CenterVertically),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (current != null) {
                                    // 1. Huge weather symbol icon
                                    Icon(
                                        imageVector = getWeatherIcon(current.weatherCode),
                                        contentDescription = getWeatherDescription(current.weatherCode),
                                        tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(120.dp)
                                            .padding(bottom = 8.dp)
                                    )

                                    // 2. Large Temperature
                                    Text(
                                        text = "${current.temperature2m.toInt()}°",
                                        fontSize = 94.sp,
                                        fontWeight = FontWeight.Light,
                                        color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )

                                    // 3. Location Area
                                    selectedCity?.let { city ->
                                        Text(
                                            text = "${city.name} ${getCountryFlagEmoji(city.countryCode)}",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color.White else Color(0xFF1D1B20),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = city.admin1?.let { "$it, ${city.country}" } ?: city.country,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF49454F),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 4. Weather Description Text
                                    Text(
                                        text = getWeatherDescription(current.weatherCode),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDarkTheme) Color.White else Color(0xFF1D1B20),
                                        textAlign = TextAlign.Center
                                    )

                                    // 5. Apparent temp
                                    current.apparentTemperature?.let { apparent ->
                                        Text(
                                            text = "体感温度: ${apparent.toInt()}°C",
                                            fontSize = 13.sp,
                                            color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF49454F),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    // 6. Today's High and Low temperatures (index 0 of daily list)
                                    if (daily != null && daily.temperature2mMax.isNotEmpty() && daily.temperature2mMin.isNotEmpty()) {
                                        val maxTemp = daily.temperature2mMax[0]
                                        val minTemp = daily.temperature2mMin[0]
                                        
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .background(
                                                    if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.05f),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "最高气温",
                                                tint = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFE65100),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "最高 ${maxTemp.toInt()}°",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color.White else Color(0xFF1D1B20)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "最低气温",
                                                tint = if (isDarkTheme) Color(0xFF81D4FA) else Color(0xFF01579B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "最低 ${minTemp.toInt()}°",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color.White else Color(0xFF1D1B20)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        is WeatherUiState.Error -> {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "错误",
                                        tint = if (isDarkTheme) Color(0xFFFF8A80) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "获取天气失败",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color.White else Color(0xFF1D1B20)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        fontSize = 13.sp,
                                        color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF49454F),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.refreshWeather() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("重新尝试", color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Persistent Bottom Navigation Guide Chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { scope.launch { drawerState.open() } }
                            .background(if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            tint = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF1D1B20).copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "向右滑动或点击查看24小时预报和设置",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF49454F)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Current Weather Detail Card (Polished Hero Layout with Quick Grid)
// -------------------------------------------------------------
@Composable
fun CurrentWeatherCard(current: CurrentForecast) {
    Card(
        shape = RoundedCornerShape(24.dp), // Modern 24dp curves matching Professional Polish
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("current_weather_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant large climate graphic icon on top
            Icon(
                imageVector = getWeatherIcon(current.weatherCode),
                contentDescription = getWeatherDescription(current.weatherCode),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 6.dp)
            )

            // Dynamic temperature presentation label
            Text(
                text = "${current.temperature2m.toInt()}°",
                fontSize = 68.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // Current state description label
            Text(
                text = getWeatherDescription(current.weatherCode),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1D1B20),
                textAlign = TextAlign.Center
            )

            // High vs Low apparent temperatures (if dynamic)
            current.apparentTemperature?.let { apparent ->
                Text(
                    text = "体感温度: ${apparent.toInt()}°C",
                    fontSize = 13.sp,
                    color = Color(0xFF49454F),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            } ?: Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(8.dp))

            // Professional 3-Column Stats Grid Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 1: Relative Humidity %
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info, // humidity symbol represent
                            contentDescription = "湿度",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("湿度", fontSize = 11.sp, color = Color(0xFF49454F))
                        Text(
                            text = "${current.relativeHumidity2m?.toInt() ?: "--"}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1D1B20)
                        )
                    }
                }

                // Stat 2: Wind Speed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Refresh, // Wind rate
                            contentDescription = "风速",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("风速", fontSize = 11.sp, color = Color(0xFF49454F))
                        Text(
                            text = "${current.windSpeed10m ?: "--"} km/h",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1D1B20)
                        )
                    }
                }

                // Stat 3: Rain/Precipitation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Star, // precipitation
                            contentDescription = "降水量",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("降水", fontSize = 11.sp, color = Color(0xFF49454F))
                        Text(
                            text = "${current.precipitation ?: 0.0} mm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1D1B20)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Hourly Forecast Scroll Card
// -------------------------------------------------------------
@Composable
fun HourlyForecastCard(hourly: HourlyForecast) {
    Card(
        shape = RoundedCornerShape(24.dp), // Modern rounded 24dp
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hourly_forecast_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "今日预报",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val next24HoursLimit = minOf(hourly.time.size, 24)
                
                itemsIndexed((0 until next24HoursLimit).toList()) { index, idx ->
                    val rawTime = hourly.time[index]
                    val temp = hourly.temperature2m[index]
                    val code = hourly.weatherCode[index]

                    val formattedTime = formatHourlyTime(rawTime)
                    val isNow = index == 0

                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isNow) MaterialTheme.colorScheme.primary else Color(0xFFF3EDF7))
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isNow) "现在" else formattedTime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isNow) Color.White.copy(alpha = 0.9f) else Color(0xFF49454F)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Icon(
                                imageVector = getWeatherIcon(code),
                                contentDescription = null,
                                tint = if (isNow) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${temp.toInt()}°",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNow) Color.White else Color(0xFF1D1B20)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Daily Forecast vertical listing card (7 Days)
// -------------------------------------------------------------
@Composable
fun DailyForecastCard(daily: DailyForecast) {
    Card(
        shape = RoundedCornerShape(24.dp), // Modern 24dp rounded corners matching mockup specs
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA)), // PolishHighlight background color
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_forecast_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "7日天气预报",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            // Dynamic listing rows
            (0 until daily.time.size).forEach { index ->
                val date = daily.time[index]
                val code = daily.weatherCode[index]
                val maxTemp = daily.temperature2mMax[index]
                val minTemp = daily.temperature2mMin[index]

                val dayName = formatDailyTime(date)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day description Name (e.g., Today / Tomorrow / Wednesday)
                    Text(
                        text = dayName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF1D1B20),
                        modifier = Modifier.width(90.dp)
                    )

                    // Condition Symbol and Text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = getWeatherIcon(code),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getWeatherDescription(code),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFF49454F)
                        )
                    }

                    // Low - High temp bounds
                    Text(
                        text = "${minTemp.toInt()}° / ${maxTemp.toInt()}°",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(80.dp)
                    )
                }

                if (index < daily.time.size - 1) {
                    HorizontalDivider(color = Color(0xFFEADDFF).copy(alpha = 0.4f)) // Light accent divider line
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Utilities Map logic for symbols and display titles
// -------------------------------------------------------------

fun getWeatherIcon(code: Int?): ImageVector {
    return when (code) {
        0, 1 -> Icons.Outlined.WbSunny
        2, 3 -> Icons.Outlined.Cloud
        45, 48 -> Icons.Outlined.Dehaze
        51, 53, 55, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Outlined.WaterDrop
        71, 73, 75, 77, 85, 86 -> Icons.Outlined.AcUnit
        95, 96, 99 -> Icons.Outlined.Thunderstorm
        else -> Icons.Outlined.WbSunny
    }
}

fun getWeatherSymbol(code: Int?): String {
    return when (code) {
        0 -> "晴" // Clear Sky
        1 -> "晴" // Mainly Clear
        2 -> "云" // Partly Cloudy
        3 -> "阴" // Overcast
        45, 48 -> "雾" // Foggy
        51, 53, 55 -> "雨" // Drizzle
        56, 57 -> "冷" // Freezing Drizzle
        61, 63 -> "雨" // Light Rain
        65 -> "雨" // Heavy Rain
        66, 67 -> "冰" // Freezing Rain
        71, 73 -> "雪" // Light Snow
        75 -> "雪" // Heavy Snow
        77 -> "雪" // Snow Grains
        80, 81, 82 -> "雨" // Rain Showers
        85, 86 -> "雪" // Snow Showers
        95 -> "雷" // Thunderstorm
        96, 99 -> "雹" // Thunderstorm with Hail
        else -> "晴"
    }
}

fun getWeatherDescription(code: Int?): String {
    return when (code) {
        0 -> "晴朗"
        1 -> "大部分晴朗"
        2 -> "多云"
        3 -> "阴天"
        45, 48 -> "大雾"
        51, 53, 55 -> "毛毛雨"
        56, 57 -> "冻细雨"
        61, 63 -> "小雨"
        65 -> "大雨"
        66, 67 -> "冻雨"
        71, 73 -> "小雪"
        75 -> "大雪"
        77 -> "雪粒"
        80, 81, 82 -> "阵雨"
        85, 86 -> "阵雪"
        95 -> "雷阵雨"
        96, 99 -> "雷暴伴有冰雹"
        else -> "晴朗"
    }
}

// Helper to convert dynamic country codes to flag emoji
fun getCountryFlagEmoji(countryCode: String?): String {
    if (countryCode == null || countryCode.length != 2) return ""
    return try {
        val uppercaseCode = countryCode.uppercase(Locale.US)
        val firstChar = uppercaseCode.codePointAt(0) - 0x41 + 0x1F1E6
        val secondChar = uppercaseCode.codePointAt(1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    } catch (e: Exception) {
        ""
    }
}

private fun formatHourlyTime(rawTime: String): String {
    return try {
        // Raw matches: 2026-06-11T12:00
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val date = parser.parse(rawTime) ?: return rawTime
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        try {
            // Fallback parse
            val parser = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = parser.parse(rawTime) ?: return rawTime
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            formatter.format(date)
        } catch (_: Exception) {
            rawTime
        }
    }
}

private fun formatDailyTime(rawDate: String): String {
    return try {
        // Raw matches: 2026-06-11
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(rawDate) ?: return rawDate
        val formatter = SimpleDateFormat("M月d日 EEE", Locale.CHINESE)
        val formatted = formatter.format(date)
        
        // If it's today, say Today!
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val calendar = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        if (rawDate == today) {
            "今天"
        } else if (rawDate == tomorrow) {
            "明天"
        } else {
            formatted
        }
    } catch (e: Exception) {
        rawDate
    }
}

@Composable
fun ThemeSelectorCard(viewModel: WeatherViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_selector_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "主题与显示设置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFF49454F)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    "light" to "浅色",
                    "dark" to "深色",
                    "system" to "系统"
                )

                modes.forEach { (mode, label) ->
                    val isSelected = themeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                            )
                            .clickable { viewModel.setThemeMode(mode) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getWeatherGradient(code: Int?, isDark: Boolean): Brush {
    val colors = if (isDark) {
        when (code) {
            0, 1 -> listOf(Color(0xFF0F2027), Color(0xFF1F3D4E), Color(0xFF1B2E3C)) // Clear/Sunny Night Gradient
            2, 3 -> listOf(Color(0xFF16222F), Color(0xFF293F56)) // Cloudy Night Gradient
            45, 48 -> listOf(Color(0xFF2C3239), Color(0xFF454C55)) // Foggy Night Gradient
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> listOf(Color(0xFF1B2A4A), Color(0xFF2B3A5E)) // Rain Night Gradient
            71, 73, 75, 77, 85, 86 -> listOf(Color(0xFF1A3542), Color(0xFF2A485A)) // Snow Night Gradient
            95, 96, 99 -> listOf(Color(0xFF24153B), Color(0xFF381F54)) // Thunderstorm Night Gradient
            else -> listOf(Color(0xFF121A24), Color(0xFF1C2836))
        }
    } else {
        when (code) {
            0, 1 -> listOf(Color(0xFFE0F7FA), Color(0xFFFFF9C4)) // Clear / Sunny Day Gradient (Bright Cream/Turquoise)
            2, 3 -> listOf(Color(0xFFE3F2FD), Color(0xFFCFD8DC)) // Cloudy Day Gradient (Soft Slate Blue)
            45, 48 -> listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC)) // Foggy Day Gradient
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> listOf(Color(0xFFE8EAF6), Color(0xFFC5CAE9)) // Rain Day Gradient (Soft Teal Aqua)
            71, 73, 75, 77, 85, 86 -> listOf(Color(0xFFE0F2F1), Color(0xFFB2DFDB)) // Snow Day Gradient (Crisp Glacier Cyan)
            95, 96, 99 -> listOf(Color(0xFFEDE7F6), Color(0xFFD1C4E9)) // Thunderstorm Day Gradient (Muted purple-grey)
            else -> listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0))
        }
    }
    return Brush.verticalGradient(colors)
}

