# RentalTutor

RentalTutor is a parked-only Android Automotive education app. Phase 1 uses two fictional vehicle profiles and a virtual control simulator. No button sends a command to a real vehicle.

## Screens

- Main: demo reservation, selected vehicle, training entry.
- Vehicle selection: compare physical-button profile A and touchscreen profile B.
- Training: required modules, prior progress, and changed controls.
- Simulation: virtual front defrost, fog light, and warning-response actions. A wrong action is recorded for retry.
- Result: completion, passed and review-needed features, locally saved history.

The on-screen `MOCK` badge and `origin=MOCK` log identify simulated data. `PleosVehicleStateSource` returns `UNAVAILABLE` and calls no SDK method.

See [ARCHITECTURE.md](ARCHITECTURE.md) and [BUILD_AND_RUN.md](BUILD_AND_RUN.md).
