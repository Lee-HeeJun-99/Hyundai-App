package com.rentaltutor.app.domain

import org.junit.Assert.*
import org.junit.Test

class TrainingDomainTest {
    private val planner = EducationPlanner()

    @Test fun profileChangeFindsChangedControls() {
        val changed = planner.changedFeatures(DemoVehicles.A, DemoVehicles.B)
        assertEquals(setOf(VehicleFeature.FRONT_DEFROST, VehicleFeature.FOG_LIGHT), changed)
        val plan = planner.plan(DemoVehicles.B, DemoVehicles.A, null)
        assertEquals(3, plan.modules.size)
    }

    @Test fun completedHistorySkipsUnneededEducation() {
        val history = TrainingRecord(DemoVehicles.A.id, VehicleFeature.entries.toSet(), emptySet(), true, 100L)
        val plan = planner.plan(DemoVehicles.A, null, history)
        assertTrue(plan.modules.isEmpty())
        val review = history.copy(needsReview = setOf(VehicleFeature.FOG_LIGHT))
        assertEquals(listOf(VehicleFeature.FOG_LIGHT), planner.plan(DemoVehicles.A, null, review).modules.map { it.feature })
    }

    @Test fun wrongControlDoesNotChangeVehicleAndRequiresRetry() {
        val engine = SimulationEngine(DemoVehicles.A)
        val wrong = engine.apply(SimulationSnapshot(), VehicleFeature.FRONT_DEFROST, VirtualControl.CLIMATE_MENU)
        assertFalse(wrong.correct)
        assertFalse(wrong.snapshot.vehicle.defrostOn)
        assertTrue(VehicleFeature.FRONT_DEFROST in wrong.snapshot.needsReview)
        val retry = engine.apply(wrong.snapshot, VehicleFeature.FRONT_DEFROST, VirtualControl.DEFROST_BUTTON)
        assertTrue(retry.correct)
        assertTrue(retry.snapshot.vehicle.defrostOn)
        assertFalse(VehicleFeature.FRONT_DEFROST in retry.snapshot.needsReview)
    }

    @Test fun allRequiredModulesMustPassBeforeCompletion() {
        val modules = planner.plan(DemoVehicles.A, null, null).modules
        val engine = SimulationEngine(DemoVehicles.A)
        var snapshot = SimulationSnapshot()
        snapshot = engine.apply(snapshot, VehicleFeature.FRONT_DEFROST, VirtualControl.DEFROST_BUTTON).snapshot
        assertFalse(AssessmentEngine().assess(DemoVehicles.A, modules, snapshot, 42L).completed)
        snapshot = engine.apply(snapshot, VehicleFeature.FOG_LIGHT, VirtualControl.FOG_BUTTON).snapshot
        snapshot = engine.apply(snapshot, VehicleFeature.WARNING_RESPONSE, VirtualControl.WARNING_INFO).snapshot
        val record = AssessmentEngine().assess(DemoVehicles.A, modules, snapshot, 42L)
        assertTrue(record.completed)
        assertEquals(42L, record.completedAtMillis)
    }
}
