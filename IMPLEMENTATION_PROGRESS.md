# DukanPOS — Production Hardening Implementation Log

**Batch:** 01 · **Date:** 2026-10-16 · **Language:** Roman Urdu + English

## Implemented in source (not build-verified)

| Audit ID | Change | Status |
|---|---|---|
| DP-002 | Android density-specific PNG launcher icon resources added | SOURCE UPDATED — build pending |
| DP-004 / DP-010 | Sale DAO checks line validity, positive finite quantity, per-product aggregated stock, payment bounds, customer existence, unmatched due and missing product before mutations | SOURCE UPDATED — integration tests pending |
| DP-009 | Negative checkout payment rejected before repository call | SOURCE UPDATED — unit tests pending |
| DP-005 / DP-006 | Return DAO validates line belongs to original sale, identity/prices/cost, positive quantity and rejects duplicated sale-line IDs | SOURCE UPDATED — integration tests pending |
| DP-007 | Khata refunds cannot exceed current outstanding customer balance; cash/refund policy still needs product decision | PARTIAL — not launch ready |
| DP-008 | Return refund is capped by remaining sale total; **returns of discounted invoices are intentionally blocked** until proportional discount/rounding policy is implemented | SAFETY STOPGAP — critical workflow incomplete |
| DP-012 | Backup snapshot multi-table read wrapped in Room `@Transaction` | SOURCE UPDATED — concurrency tests pending |
| DP-011 | Restore rejects wrong format, empty shop settings, duplicate primary identifiers and several dangling references before destructive replacement | PARTIAL — no checksum, user preview, pre-restore safety copy, comprehensive schema validation yet |
| DP-020 | Purchase DAO rejects empty, invalid/non-finite/nonpositive lines, negative cost and missing products before writing | SOURCE UPDATED — integration tests pending |

## Important outstanding blockers

- **DP-001 / DP-003:** Gradle wrapper is not included; `gradle` executable absent in this environment. Thus no dependency resolution, compilation, test runs, Android APK or iOS validation happened. Do not claim these patches compile until CI/local Gradle run verifies.
- **DP-008:** Discounted invoice returns require discount allocation implementation; checkout and data policies must be reconciled before deployment.
- **DP-011 / DP-016:** Restore still needs confirmation preview, protective rollback copy and comprehensive backup validations.
- **DP-013:** Room integration tests are missing; new checks are not database-proven.
- Further P0/P1 items from the audit remain pending.
- **Real-device UAT strictly LAST:** only after build, automated suites, data recovery, release readiness and functional QA pass.

## Next ordered batch

1. Restore a verified official Gradle 8.11.1 wrapper (script **and official wrapper JAR**) using a trusted build host and lock checksum; compile and address actual errors. Do not handcraft/forge wrapper JAR.
2. Implement fully policy-consistent discount-aware refunds and split cash/khata settlement.
3. Implement comprehensive Room integration tests and run automated test suite.
4. Implement protected backup/restore preview/confirmation/rollback and security hardening.
5. UI/regression testing, release preparation, **then** real-device UAT.

**Release gate:** NO-GO. Files are unverified source changes, not a certified shop-ready release.

## Batch 02 update — 2026-10-16

See `BATCH_02_CHANGELOG.md`. DP-008 discounted sale returns now have proportional cumulative invoice-level refund calculation in UI and DAO, with a pure Kotlin smoke test PASS. **Full Gradle/build/Room tests remain NOT VERIFIED, DP-007 remains open, and deployment is NO-GO.** Real-device testing is still last.

## Batch 03 (2026-10-16)
- Khata overpayment and missing-customer transaction guards implemented.
- Checked ledger balance arithmetic; common regression tests authored (not executed).
- Official Gradle wrapper is still not available; see BUILD_VERIFICATION.md.
- Room integration and full builds NOT verified; deployment NO-GO.
- Physical device tests remain LAST.

## Batch 04 (2026-10-16)
- Added pre-restore cross-table, numeric, unique-index and financial sanity validations (`RestoreValidation.kt`).
- Added five authored regression tests (`RestoreValidationTest.kt`), not executed.
- Full Gradle/Room tests remain pending. See `BATCH_04_CHANGELOG.md`. Deployment remains NO-GO.

## Batch 05 (2026-10-16)
- Checked overflow protection applied to sale khata balance changes (`safeLedgerBalance`).
- Repository payment boundary rejects non-positive values; customer lookup errors mapped to app failure.
- JVM unit test sources added; automated GitHub Actions JVM/Desktop/Android verification workflow authored.
- Standalone ledger arithmetic Kotlin compilation and 4 smoke checks passed. **Full Gradle build, Room tests and CI NOT run.**
- Cash vs khata returns settlement policy unresolved; production **NO-GO**. Physical-device testing **last phase only**.


## Batch 06
See `BATCH_06_CHANGELOG.md`. Cash-refund cap and tests added; integration and full build NOT verified. Device testing remains last.

## Batch 07 (2026-10-16)
- Additional in-transaction sale/return guards for missing products/customers, corrupt original return lines, and non-finite stock calculations.
- Documented why later customer-level Khata payments cannot safely be allocated to an invoice until a versioned migration plus reconciliation policy exists.
- Added `BATCH_07_CHANGELOG.md` and `ROOM_AND_KHATA_TEST_MATRIX.md` with 14 planned, **not executed**, integration scenarios.
- Full build and Room tests still unavailable/unverified. Production NO-GO. Device testing remains LAST.
