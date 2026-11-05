package com.rentaltutor.app.data

import android.content.Context
import com.rentaltutor.app.domain.TrainingRecord
import com.rentaltutor.app.domain.VehicleFeature
import org.json.JSONArray
import org.json.JSONObject

class TrainingRecordStore(context: Context) {
    private val prefs = context.getSharedPreferences("training_history", Context.MODE_PRIVATE)
    fun save(record: TrainingRecord) {
        val obj = JSONObject().put("profileId", record.profileId)
            .put("passed", JSONArray(record.passed.map { it.name }))
            .put("needsReview", JSONArray(record.needsReview.map { it.name }))
            .put("completed", record.completed)
            .put("completedAtMillis", record.completedAtMillis)
        prefs.edit().putString(record.profileId, obj.toString()).apply()
    }
    fun load(profileId: String): TrainingRecord? = try {
        val raw = prefs.getString(profileId, null) ?: return null
        val obj = JSONObject(raw)
        fun features(key: String): Set<VehicleFeature> {
            val array = obj.getJSONArray(key)
            return (0 until array.length()).map { VehicleFeature.valueOf(array.getString(it)) }.toSet()
        }
        TrainingRecord(obj.getString("profileId"), features("passed"), features("needsReview"),
            obj.getBoolean("completed"), obj.getLong("completedAtMillis"))
    } catch (_: Exception) { null }
}
