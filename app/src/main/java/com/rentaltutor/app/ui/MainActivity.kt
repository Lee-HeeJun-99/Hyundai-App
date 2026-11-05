package com.rentaltutor.app.ui

import android.app.Activity
import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.rentaltutor.app.data.MockVehicleStateSource
import com.rentaltutor.app.data.TrainingRecordStore
import com.rentaltutor.app.domain.*

class MainActivity : Activity() {
    private enum class Screen { MAIN, VEHICLE, TRAINING, SIMULATION, RESULT }
    private var screen = Screen.MAIN
    private var profile = DemoVehicles.A
    private var previousProfile: VehicleProfile? = null
    private val planner = EducationPlanner()
    private val assessor = AssessmentEngine()
    private val stateSource = MockVehicleStateSource()
    private lateinit var store: TrainingRecordStore
    private var plan = EducationPlan(emptyList(), emptySet())
    private var simulation = SimulationSnapshot()
    private var moduleIndex = 0
    private var feedback = ""
    private var car: Car? = null
    private var uxManager: CarUxRestrictionsManager? = null
    private var practiceAllowed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TrainingRecordStore(this)
        connectUxGate()
        render()
    }

    override fun onResume() {
        super.onResume()
        refreshUxGate()
    }

    override fun onPause() {
        practiceAllowed = false
        super.onPause()
    }

    override fun onDestroy() {
        try { uxManager?.unregisterListener() } catch (_: Exception) { }
        try { car?.disconnect() } catch (_: Exception) { }
        super.onDestroy()
    }

    private fun connectUxGate() {
        try {
            car = Car.createCar(this)
            uxManager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
            uxManager?.registerListener { restrictions ->
                runOnUiThread {
                    practiceAllowed = !restrictions.isRequiresDistractionOptimization
                    Log.i("RentalTutor", "practiceAllowed=$practiceAllowed by Car UX restrictions")
                    if (screen == Screen.SIMULATION || screen == Screen.TRAINING) render()
                }
            }
            refreshUxGate()
        } catch (e: Exception) {
            Log.w("RentalTutor", "Car UX state unavailable; practice blocked", e)
            practiceAllowed = false
        }
    }

    private fun refreshUxGate() {
        practiceAllowed = try {
            uxManager?.currentCarUxRestrictions?.isRequiresDistractionOptimization == false
        } catch (_: Exception) { false }
        if (screen == Screen.SIMULATION || screen == Screen.TRAINING) render()
    }

    private fun dp(value: Int): Int = (resources.displayMetrics.density * value).toInt()
    private fun layout(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(20), dp(28), dp(24))
            setBackgroundColor(0xFF101B27.toInt())
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        return root
    }
    private fun label(root: LinearLayout, text: String, size: Float = 20f, color: Int = 0xFFF1F5F9.toInt()) {
        root.addView(TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            setPadding(0, dp(5), 0, dp(5))
        })
    }
    private fun button(root: LinearLayout, text: String, enabled: Boolean = true, action: () -> Unit) {
        root.addView(Button(this).apply {
            this.text = text; isEnabled = enabled; minHeight = dp(56)
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(10)
        })
    }
    private fun header(root: LinearLayout, title: String) {
        label(root, "RentalTutor", 29f, 0xFF7DD3FC.toInt())
        label(root, title, 25f)
        val snapshot = stateSource.snapshot()
        label(root, "데이터 출처: ${snapshot.origin} · ${snapshot.note}", 15f, 0xFFFBBF24.toInt())
        label(root, "이 앱은 실제 차량을 제어하지 않습니다.", 15f, 0xFFCBD5E1.toInt())
    }
    private fun render() {
        val root = layout()
        when (screen) {
            Screen.MAIN -> renderMain(root)
            Screen.VEHICLE -> renderVehicle(root)
            Screen.TRAINING -> renderTraining(root)
            Screen.SIMULATION -> renderSimulation(root)
            Screen.RESULT -> renderResult(root)
        }
    }
    private fun renderMain(root: LinearLayout) {
        header(root, "처음 타는 차, 출발 전에 연습하세요")
        label(root, "현재 예약: 데모 예약 · 오늘 10:00 · 교육용", 18f)
        label(root, "예약 차량: ${profile.name}", 20f)
        val history = store.load(profile.id)
        if (history?.completed == true) label(root, "이전 교육 완료 기록이 있습니다.", 16f, 0xFF86EFAC.toInt())
        button(root, "교육 시작") { preparePlan(); screen = Screen.TRAINING; render() }
        button(root, "차량 선택 / 비교") { screen = Screen.VEHICLE; render() }
    }
    private fun renderVehicle(root: LinearLayout) {
        header(root, "차량 선택")
        for (candidate in DemoVehicles.all) {
            label(root, "${candidate.name}${if (candidate.id == profile.id) " · 선택됨" else ""}", 21f, 0xFF7DD3FC.toInt())
            VehicleFeature.entries.forEach { feature ->
                label(root, "${feature.label}: ${candidate.controlNotes.getValue(feature)}", 16f)
            }
            button(root, "${candidate.name} 선택") {
                previousProfile = profile
                profile = candidate
                preparePlan()
                screen = Screen.TRAINING
                render()
            }
        }
        button(root, "홈으로") { screen = Screen.MAIN; render() }
    }
    private fun preparePlan() {
        val history = store.load(profile.id)
        plan = planner.plan(profile, previousProfile, history)
        simulation = SimulationSnapshot(
            passed = (history?.passed ?: emptySet()) - plan.changed,
            needsReview = history?.needsReview ?: emptySet()
        )
        moduleIndex = 0
        feedback = ""
        previousProfile = profile
    }
    private fun renderTraining(root: LinearLayout) {
        header(root, "필수 교육 모듈")
        label(root, "선택 차량: ${profile.name}", 19f)
        if (plan.changed.isNotEmpty()) label(root, "차량 변경 기능: ${plan.changed.joinToString { it.label }}", 16f)
        if (plan.modules.isEmpty()) {
            label(root, "완료한 교육을 다시 요구하지 않습니다.", 17f, 0xFF86EFAC.toInt())
            button(root, "결과 보기") { screen = Screen.RESULT; render() }
        } else {
            plan.modules.forEachIndexed { index, module ->
                val status = if (module.feature in simulation.passed) "완료" else if (module.feature in simulation.needsReview) "재학습" else "대기"
                label(root, "${index + 1}. ${module.feature.label} · $status\n${module.instruction}", 17f)
            }
            button(root, "가상 교육 시작", practiceAllowed) {
                screen = Screen.SIMULATION; render()
            }
            if (!practiceAllowed) label(root, "차량 UX 제한 상태가 확인되지 않았거나 제한 중입니다. 주차 상태에서 다시 시도하세요.", 16f, 0xFFFBBF24.toInt())
        }
        button(root, "차량 변경") { screen = Screen.VEHICLE; render() }
        button(root, "홈으로") { screen = Screen.MAIN; render() }
    }
    private fun renderSimulation(root: LinearLayout) {
        header(root, "가상 실습")
        if (!practiceAllowed) {
            label(root, "실습 중단: 차량 UX 제한 상태가 확인되지 않았거나 제한 중입니다.", 19f, 0xFFFBBF24.toInt())
            button(root, "교육 목록으로") { screen = Screen.TRAINING; render() }
            return
        }
        if (moduleIndex >= plan.modules.size) { screen = Screen.RESULT; render(); return }
        val module = plan.modules[moduleIndex]
        label(root, "${moduleIndex + 1}/${plan.modules.size} · ${module.feature.label}", 22f)
        label(root, "목표: ${module.instruction}", 18f)
        label(root, "가상 상태: 앞유리 제상 ${onOff(simulation.vehicle.defrostOn)} · 안개등 ${onOff(simulation.vehicle.fogLightOn)} · 경고등 설명 ${if (simulation.vehicle.warningAcknowledged) "확인" else "미확인"}", 17f)
        if (feedback.isNotEmpty()) label(root, feedback, 18f, if (module.feature in simulation.needsReview) 0xFFFCA5A5.toInt() else 0xFF86EFAC.toInt())
        val options = when (module.feature) {
            VehicleFeature.FRONT_DEFROST -> listOf(VirtualControl.DEFROST_BUTTON, VirtualControl.CLIMATE_MENU)
            VehicleFeature.FOG_LIGHT -> listOf(VirtualControl.FOG_BUTTON, VirtualControl.LIGHT_MENU)
            VehicleFeature.WARNING_RESPONSE -> listOf(VirtualControl.WARNING_INFO, VirtualControl.IGNORE_WARNING)
        }
        for (control in options) button(root, control.label) {
            if (!practiceAllowed) { render(); return@button }
            val outcome = SimulationEngine(profile).apply(simulation, module.feature, control)
            simulation = outcome.snapshot
            feedback = outcome.feedback
            val record = assessor.assess(profile, plan.modules, simulation, System.currentTimeMillis())
            store.save(record)
            Log.i("RentalTutor", "simulation origin=MOCK feature=${module.feature} correct=${outcome.correct} review=${simulation.needsReview}")
            render()
        }
        if (module.feature in simulation.passed && module.feature !in simulation.needsReview) {
            button(root, if (moduleIndex + 1 == plan.modules.size) "결과 보기" else "다음 교육") {
                moduleIndex++
                feedback = ""
                screen = if (moduleIndex >= plan.modules.size) Screen.RESULT else Screen.SIMULATION
                render()
            }
        }
        button(root, "교육 목록으로") { screen = Screen.TRAINING; render() }
    }
    private fun onOff(value: Boolean) = if (value) "켜짐" else "꺼짐"
    private fun renderResult(root: LinearLayout) {
        header(root, "교육 결과")
        val record = store.load(profile.id)
        label(root, "교육 완료: ${if (record?.completed == true) "완료" else "미완료"}", 22f,
            if (record?.completed == true) 0xFF86EFAC.toInt() else 0xFFFBBF24.toInt())
        label(root, "통과: ${record?.passed?.joinToString { it.label } ?: "없음"}", 18f)
        label(root, "재교육 필요: ${record?.needsReview?.joinToString { it.label } ?: "없음"}", 18f)
        if (record?.completed == true) label(root, "교육 이력이 기기에 저장되었습니다. 완료 시각: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(record.completedAtMillis))}", 16f)
        button(root, "교육 목록으로") { preparePlan(); screen = Screen.TRAINING; render() }
        button(root, "홈으로") { screen = Screen.MAIN; render() }
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        screen = when (screen) {
            Screen.MAIN -> { super.onBackPressed(); return }
            Screen.VEHICLE, Screen.TRAINING -> Screen.MAIN
            Screen.SIMULATION, Screen.RESULT -> Screen.TRAINING
        }
        render()
    }
}
