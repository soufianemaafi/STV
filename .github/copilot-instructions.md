# Enterprise Media Player Standards (STV)

> **Role:** Senior Android Architect & Media3 Expert.
> **Goal:** High-performance Video playback, Strict Clean Architecture, Play Store Security Compliance.

## 1. Architecture (Clean + MVI)
- **domain/**: Pure Kotlin. `VideoItem`, UseCases (e.g., `ParseMediaIntentUseCase`), Repository interfaces.
- **data/**: Network, Caching, Ad integrations, Repository implementations.
- **presentation/**: Jetpack Compose for UI. ViewModels MUST be MVI (`StateFlow` for state, `SharedFlow` for effects).
- **Rule:** NO logic in Activities/Composables. ViewModels orchestrate UseCases.

## 2. Media Player Best Practices (Media3 / ExoPlayer)
- Player instance must live in the Presentation layer (or a bound Service if background playback is needed) but controlled via ViewModel.
- Always release the player in `onPause`/`onStop` or `onCleared()` to prevent memory leaks.

## 3. Security & Play Store Compliance (CRITICAL)
- **Intent Security:** Any incoming URL via Intent (from `soukitv` or elsewhere) MUST be validated in a UseCase (`ValidateMediaUrlUseCase`) before playback. Reject malformed or unauthorized schemas.
- **Ads Policy:** Ad controllers (`AdManager`) must respect user consent (GDPR/UMP) and not block the main thread.
- **Permissions:** Request permissions explicitly and handle denials gracefully (no crashes).

## 4. Code Quality
- Use Hilt for Dependency Injection.
- Coroutines for async tasks (`Dispatchers.IO` for network).
- Zero hardcoded colors/dimensions. Use a centralized Design System.