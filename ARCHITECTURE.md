# Architecture

`app/src/main/java/com/rentaltutor/app/domain/TrainingDomain.kt` has no Android dependencies:

- `VehicleProfile`: control map and instructions for a fictional vehicle.
- `VehicleFeature`: the three training subjects.
- `EducationModule`: one required feature and instruction.
- `EducationPlanner`: changed controls and history-aware module selection.
- `SimulationEngine`: validates a virtual button and updates an immutable virtual state; a wrong action adds a review item.
- `AssessmentEngine`: evaluates required completion and creates a record.
- `TrainingRecord`: passed, review-needed, completion, timestamp, and profile ID.

`data/VehicleStateSource.kt` defines source tagging. `MockVehicleStateSource` returns `MOCK`; the Pleos placeholder returns `UNAVAILABLE`. `TrainingRecordStore` persists JSON in private SharedPreferences. `ui/MainActivity.kt` renders five simple screens and routes virtual interactions through the domain engines.

The activity is **not** marked distraction optimized. On Android Automotive, it checks `CarUxRestrictionsManager`; unknown or active restrictions block training. The OS may hide a non-optimized activity while driving. A false UX restriction is a practice gate, not proof of gear P. Phase 2 should add a verified parked/gear signal before any live-vehicle workflow. Phase 1 has no Pleos SDK dependency or vehicle permissions.
