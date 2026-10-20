# DukanPOS — Batch 05 changelog

Date: 2026-10-16. **Deployment: NO-GO.** Source changes are not a verified full app build.

## Actual changes

1. Extracted shared `safeLedgerBalance` checked addition/subtraction to `data/local/dao/LedgerMath.kt`.
2. `SaleDao.commitSale` now uses checked arithmetic before persisting khata due, rejecting overflows transactionally rather than wrapping to a negative customer balance.
3. `CustomerRepositoryImpl.recordPayment` rejects non-positive payment values at the repository boundary, even if a caller bypasses the use case.
4. `RecordCustomerPaymentUseCase` now converts customer lookup exceptions to `AppResult.Failure` rather than letting an uncaught exception escape.
5. Added common-test cases for zero/negative customer payments and four checked-ledger arithmetic cases.
6. Added GitHub Actions workflow `.github/workflows/verify.yml` to run JVM tests, desktop compilation and Android debug assembly with Gradle 8.11.1 and Android SDK 35 on Linux.

## Verification performed

- Standalone `kotlinc` compilation of `LedgerMath.kt`: **PASS**.
- Standalone JVM smoke execution of 4 edge cases (normal addition, exact debit, positive overflow, negative underflow): **PASS**.
- ZIP integrity check: performed during packaging.

## Not verified

- Full Gradle sync/build, Android assembly, desktop compilation, KMP common tests, Room in-memory integration tests: **NOT RUN**. No installed Gradle or resolved Maven artifacts in this local environment.
- CI workflow is **authored but not executed**; it does not substitute for a green CI run. An official Gradle wrapper is still missing.
- Cash-vs-khata partial return allocation remains an **unresolved business policy**. A return may be mathematically accurate but settled via a method inconsistent with the initial payment breakdown; define cash-first vs due-first allocation, handle previous payments, and add transaction-backed tests before release.
- Room data integrity tests and real backup recovery tests remain pending.
- Real device/hardware tests must happen at the **very last phase**.

## Recommended next batch

Run full CI/Gradle in a network-enabled Android development environment, fix resulting compile errors, implement Room-backed transactional tests and the agreed refund settlement allocation policy. Maintain NO-GO until verified.
