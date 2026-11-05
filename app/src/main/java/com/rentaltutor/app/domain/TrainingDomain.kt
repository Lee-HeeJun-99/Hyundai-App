package com.rentaltutor.app.domain

enum class VehicleFeature(val label: String) {
    FRONT_DEFROST("앞유리 김서림 제거"), FOG_LIGHT("안개등"), WARNING_RESPONSE("경고등 대응")
}

enum class VirtualControl(val label: String) {
    DEFROST_BUTTON("앞유리 제상 버튼"), CLIMATE_MENU("공조 화면의 앞유리 제상"),
    FOG_BUTTON("안개등 버튼"), LIGHT_MENU("조명 화면의 안개등"),
    WARNING_INFO("경고등 설명 확인"), IGNORE_WARNING("경고등 무시")
}

data class VehicleProfile(
    val id: String,
    val name: String,
    val controls: Map<VehicleFeature, VirtualControl>,
    val controlNotes: Map<VehicleFeature, String>
) {
    fun controlFor(feature: VehicleFeature): VirtualControl = controls.getValue(feature)
}

object DemoVehicles {
    val A = VehicleProfile("A", "가상 차량 A", mapOf(
        VehicleFeature.FRONT_DEFROST to VirtualControl.DEFROST_BUTTON,
        VehicleFeature.FOG_LIGHT to VirtualControl.FOG_BUTTON,
        VehicleFeature.WARNING_RESPONSE to VirtualControl.WARNING_INFO
    ), mapOf(
        VehicleFeature.FRONT_DEFROST to "물리 버튼으로 앞유리 제상",
        VehicleFeature.FOG_LIGHT to "물리 버튼으로 안개등",
        VehicleFeature.WARNING_RESPONSE to "화면에서 경고등 설명 확인"
    ))
    val B = VehicleProfile("B", "가상 차량 B", mapOf(
        VehicleFeature.FRONT_DEFROST to VirtualControl.CLIMATE_MENU,
        VehicleFeature.FOG_LIGHT to VirtualControl.LIGHT_MENU,
        VehicleFeature.WARNING_RESPONSE to VirtualControl.WARNING_INFO
    ), mapOf(
        VehicleFeature.FRONT_DEFROST to "공조 화면에서 앞유리 제상",
        VehicleFeature.FOG_LIGHT to "조명 화면에서 안개등",
        VehicleFeature.WARNING_RESPONSE to "화면에서 경고등 설명 확인"
    ))
    val all = listOf(A, B)
}

data class EducationModule(val feature: VehicleFeature, val instruction: String, val required: Boolean = true)

data class TrainingRecord(
    val profileId: String,
    val passed: Set<VehicleFeature>,
    val needsReview: Set<VehicleFeature>,
    val completed: Boolean,
    val completedAtMillis: Long
)

data class EducationPlan(val modules: List<EducationModule>, val changed: Set<VehicleFeature>)

class EducationPlanner {
    fun changedFeatures(previous: VehicleProfile?, current: VehicleProfile): Set<VehicleFeature> =
        if (previous == null) current.controls.keys else current.controls.keys.filterTo(mutableSetOf()) {
            previous.controls[it] != current.controls[it]
        }

    fun plan(current: VehicleProfile, previous: VehicleProfile?, history: TrainingRecord?): EducationPlan {
        val changed = if (previous == null && history?.profileId == current.id) emptySet() else changedFeatures(previous, current)
        val passed = if (history?.profileId == current.id) history.passed else emptySet()
        val review = if (history?.profileId == current.id) history.needsReview else emptySet()
        val needed = current.controls.keys.filter { it in changed || it !in passed || it in review }
        return EducationPlan(needed.map { EducationModule(it, current.controlNotes.getValue(it)) }, changed)
    }
}

data class VirtualVehicleState(
    val defrostOn: Boolean = false,
    val fogLightOn: Boolean = false,
    val warningAcknowledged: Boolean = false
)

data class SimulationSnapshot(
    val vehicle: VirtualVehicleState = VirtualVehicleState(),
    val passed: Set<VehicleFeature> = emptySet(),
    val needsReview: Set<VehicleFeature> = emptySet(),
    val attempts: Int = 0
)

data class SimulationOutcome(val snapshot: SimulationSnapshot, val correct: Boolean, val feedback: String)

class SimulationEngine(private val profile: VehicleProfile) {
    fun apply(current: SimulationSnapshot, feature: VehicleFeature, control: VirtualControl): SimulationOutcome {
        require(feature in profile.controls) { "Profile does not support $feature" }
        val correct = profile.controlFor(feature) == control
        val vehicle = if (!correct) current.vehicle else when (feature) {
            VehicleFeature.FRONT_DEFROST -> current.vehicle.copy(defrostOn = true)
            VehicleFeature.FOG_LIGHT -> current.vehicle.copy(fogLightOn = true)
            VehicleFeature.WARNING_RESPONSE -> current.vehicle.copy(warningAcknowledged = true)
        }
        val passed = if (correct) current.passed + feature else current.passed - feature
        val review = if (correct) current.needsReview - feature else current.needsReview + feature
        return SimulationOutcome(
            current.copy(vehicle = vehicle, passed = passed, needsReview = review, attempts = current.attempts + 1),
            correct,
            if (correct) "올바른 가상 조작입니다." else "이 차량의 조작 방식과 다릅니다. 다시 연습하세요."
        )
    }
}

class AssessmentEngine {
    fun assess(profile: VehicleProfile, modules: List<EducationModule>, snapshot: SimulationSnapshot, nowMillis: Long): TrainingRecord {
        val required = modules.filter { it.required }.map { it.feature }.toSet()
        val completed = required.all { it in snapshot.passed } && snapshot.needsReview.isEmpty()
        return TrainingRecord(profile.id, snapshot.passed, snapshot.needsReview, completed, if (completed) nowMillis else 0L)
    }
}
