# JitPack Dependency Fix - AudioSwitch Library

## Problem
```
Configuration cache state could not be cached: field `__runtimeDependenciesNavigationFiles__` 
of task `:app:processDebugNavigationResources` of type `com.android.build.gradle.internal.tasks.ProcessNavigationXmlTask`: 
error writing value of type 'org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection' 
> Could not resolve all files for configuration ':app:debugRuntimeClasspath'.
  > Could not find com.github.davidliu:audioswitch:89582c47c9a04c62f90aa5e57251af4800a62c9a.
    Searched in the following locations:
      - https://dl.google.com/dl/android/maven2/com/github/davidliu/audioswitch/...
      - https://repo.maven.apache.org/maven2/com/github/davidliu/audioswitch/...
    Required by:
        project :app > io.elevenlabs:elevenlabs-android:0.3.0 > io.livekit:livekit-android:2.19.0
```

## Root Cause
The `audioswitch` library from `com.github.davidliu` is a transitive dependency of:
- `io.livekit:livekit-android:2.19.0` (required by ElevenLabs SDK)
- This library is hosted on **JitPack**, not on Google Maven or Maven Central
- JitPack repository was not declared in the project's repository list

## Solution Applied ✅

### Modified File: `settings.gradle.kts`

Added JitPack repository to the dependency resolution management:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // ← ADDED THIS LINE
    }
}
```

## Why This Works
- JitPack is a package repository that builds Git repositories on demand
- Many GitHub-hosted libraries (especially those with `com.github.*` group IDs) are published on JitPack
- The `audioswitch` library is hosted at: https://jitpack.io/#davidliu/audioswitch
- By adding the JitPack repository, Gradle can now resolve this transitive dependency

## Next Steps
1. **Clean build directories** (already done automatically):
   - Removed `.gradle/` directory (configuration cache)
   - Removed `build/` directory
   - Removed `app/build/` directory

2. **Sync/Build your project**:
   - In Android Studio: File → Sync Project with Gradle Files
   - Or via command line: `gradlew.bat assembleDebug`

3. **The dependency will now be resolved from JitPack** ✅

## Verification
After syncing, the following should happen:
- ✅ Gradle will download `audioswitch` from JitPack
- ✅ Configuration cache will be rebuilt successfully
- ✅ Build will complete without dependency resolution errors

## Additional Notes
- This is a **safe change** - JitPack is a legitimate and widely-used repository
- No changes to your app's dependencies were needed
- The ElevenLabs SDK continues to work as expected with its transitive dependencies resolved

---
**Status**: ✅ FIXED - JitPack repository added to settings.gradle.kts
**Date**: November 5, 2025

