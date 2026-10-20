# iOS app

The Xcode project is not checked in — generate it once, then it stays yours.

1. `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` to produce `Shared.framework`.
2. In Xcode, create an iOS App target named `iosApp` in this folder and add
   `iosApp/iOSApp.swift`, `iosApp/ContentView.swift` and `iosApp/Info.plist`.
3. Add the framework search path
   `$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework`
   and link `Shared.framework` (Embed & Sign).
4. Add a Run Script build phase before "Compile Sources":
   ```
   cd "$SRCROOT/.."
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```
5. Camera permission is already declared in `Info.plist`.
