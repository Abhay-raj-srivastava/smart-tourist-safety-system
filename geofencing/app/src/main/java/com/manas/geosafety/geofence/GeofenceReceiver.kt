package com.manas.geosafety.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.manas.geosafety.location.NotificationHelper

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
        val notificationHelper = NotificationHelper(context)

        for (geofence in triggeringGeofences) {
            val zoneName = geofence.requestId
            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    notificationHelper.showNotification(
                        title = "🟢 STATUS: INSIDE SAFE ZONE",
                        message = "You have entered $zoneName."
                    )
                }
                // FIX: Changed this transition to trigger a warning when leaving the perimeter
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    notificationHelper.showNotification(
                        title = "⚠️ WARNING: OUTSIDE SAFE BOUNDARY",
                        message = "You have exited the safe area ($zoneName)! Please return to the secure zone immediately."
                    )
                }
            }
        }
    }
}