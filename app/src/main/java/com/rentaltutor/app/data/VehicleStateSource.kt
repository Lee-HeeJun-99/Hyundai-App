package com.rentaltutor.app.data

import android.util.Log

enum class DataOrigin { MOCK, PLEOS_SDK, UNAVAILABLE }
data class VehicleSnapshot(val origin: DataOrigin, val note: String, val warningLampOn: Boolean?)
interface VehicleStateSource { fun snapshot(): VehicleSnapshot }
class MockVehicleStateSource : VehicleStateSource {
    override fun snapshot(): VehicleSnapshot = VehicleSnapshot(DataOrigin.MOCK, "가상 교육 데이터", true).also {
        Log.i("RentalTutor", "Vehicle snapshot origin=${it.origin}")
    }
}
class PleosVehicleStateSource : VehicleStateSource {
    override fun snapshot(): VehicleSnapshot = VehicleSnapshot(DataOrigin.UNAVAILABLE, "Pleos SDK 미연동", null).also {
        Log.i("RentalTutor", "Vehicle snapshot origin=${it.origin}; no SDK call")
    }
}
