package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class MasterWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("widget_state", Context.MODE_PRIVATE)
            val widgetMode = prefs.getString("mode", "DEFAULT") ?: "DEFAULT"
            
            val sharedText = prefs.getString("content", "No recent activity.") ?: "No recent activity."
            val views = RemoteViews(context.packageName, R.layout.widget_master)
            
            views.setTextViewText(R.id.widget_content, sharedText)
            
            if (widgetMode == "SCAM") {
                views.setTextViewText(R.id.widget_title, "Threat Blocked")
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_scam)
                views.setViewVisibility(R.id.widget_action_btn, android.view.View.VISIBLE)
            } else if (widgetMode == "OTP") {
                views.setTextViewText(R.id.widget_title, "Secure OTP")
                views.setTextViewText(R.id.widget_content, "Tap to Reveal")
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_otp)
                views.setViewVisibility(R.id.widget_action_btn, android.view.View.GONE)
            } else {
                views.setTextViewText(R.id.widget_title, "Mina")
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_dark_glass)
                views.setViewVisibility(R.id.widget_action_btn, android.view.View.GONE)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                if (widgetMode == "OTP") {
                    putExtra("SECURE_OTP_TEXT", sharedText)
                    putExtra("SECURE_OTP_TITLE", "Widget OTP")
                } else if (widgetMode == "SCAM") {
                    putExtra("BLOCK_CALL", true)
                }
            }
            // Add a unique action based on mode to prevent Intent caching issues.
            intent.action = "WIDGET_ACTION_$widgetMode"
            
            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_action_btn, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
