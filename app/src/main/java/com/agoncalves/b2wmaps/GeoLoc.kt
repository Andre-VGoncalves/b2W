package com.agoncalves.b2wmaps

import android.content.Context
import android.content.SharedPreferences
import com.agoncalves.b2wmaps.model.GeoLocResponse
import com.google.gson.Gson

class GeoLoc(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "geofenceDb", Context.MODE_PRIVATE)
    fun getGeofence(id: String): GeoLocResponse? {
        val gson = Gson()
        val json = prefs.getString(id, null)
        return if (json != null) {
            gson.fromJson(json, GeoLocResponse::class.java)
        } else {
            null
        }
    }
    fun saveGeofence(geofence: GeoLocResponse) {
        val gson = Gson()
        val json = gson.toJson(geofence)
        val editor = prefs.edit()
        editor.putString(geofence.id, json)
        editor.apply()
    }
    fun removeGeofence(id: String) {
        val editor = prefs.edit()
        editor.remove(id)
        editor.apply()
    }
}
