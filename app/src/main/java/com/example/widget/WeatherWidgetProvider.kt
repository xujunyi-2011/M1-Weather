package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.ACTION_UPDATE_WIDGET" || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, WeatherWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
            val cityName = prefs.getString("cityName", "北京") ?: "北京"
            val temp = prefs.getString("temperature", "24°C") ?: "24°C"
            val desc = prefs.getString("description", "多云") ?: "多云"
            val icon = prefs.getString("icon", "晴") ?: "晴"

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.weather_widget_layout).apply {
                setTextViewText(R.id.widget_city_title, cityName)
                setTextViewText(R.id.widget_temperature, temp)
                setTextViewText(R.id.widget_weather_desc, desc)
                setTextViewText(R.id.widget_weather_icon, icon)
            }

            // Create an Intent to launch MainActivity when clicked
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Set pending intent clicks on all views to open main application
            views.setOnClickPendingIntent(R.id.widget_city_title, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_temperature, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_weather_desc, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_weather_icon, pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // Helper to notify all widgets to update from SharedPreferences
        fun triggerUpdate(context: Context, cityName: String, temperature: String, description: String, icon: String) {
            val prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("cityName", cityName)
                putString("temperature", temperature)
                putString("description", description)
                putString("icon", icon)
                apply()
            }

            val updateIntent = Intent(context, WeatherWidgetProvider::class.java).apply {
                action = "com.example.ACTION_UPDATE_WIDGET"
            }
            context.sendBroadcast(updateIntent)
        }
    }
}
