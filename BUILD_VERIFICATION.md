# DukanPOS — Reproducible Build Verification (Batch 03)

## Environment prerequisite

JDK 17, Android SDK 35 (for Android builds), and **trusted official Gradle 8.11.1**. iOS builds also require supported macOS/Xcode. This archive does not contain `gradlew` or `gradle/wrapper/gradle-wrapper.jar`; neither was present in supplied sources and the current execution environment does not provide a trusted copy. Never download a wrapper jar from an arbitrary mirror.

On a trusted host with Gradle 8.11.1 installed, from repository root:

```sh
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
./gradlew --version
./gradlew :shared:jvmTest --stacktrace
./gradlew :desktopApp:compileKotlinJvm --stacktrace
./gradlew :androidApp:assembleDebug --stacktrace
```

Commit the generated official `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` and `gradle/wrapper/gradle-wrapper.properties`; pin the distribution SHA-256 after independently checking the official Gradle checksums. Re-run all tasks on clean CI and archive logs. Gradle/build/test success **has not been established** for this batch.

## Database integration test checklist (requires real Room runtime)

1. In-memory Room sale commit: stock=5, two lines qty=4; verify rejection, zero invoice/stock/ledger writes.
2. Missing customer or negative paid sale; verify atomic rollback.
3. Return wrong saleItemId, duplicate line, over-return; assert no stock/ledger writes.
4. Discounted mixed-line partial returns; cumulative refund = original net paid amount.
5. Purchase missing product, qty <= 0, duplicate product lines; assert rollback.
6. Khata exact payment vs overpayment, missing customer, balance overflow; assert ledger and customer balance change together.
7. Backup snapshot during writes and restore malformed references; no partial database replacement.

These are **required future executed tests**, not claims that Room tests already ran. Real-device/hardware UAT remains the final phase.
