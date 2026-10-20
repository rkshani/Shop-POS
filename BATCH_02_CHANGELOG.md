# DukanPOS — Batch 02 Implementation Changelog

**Date:** 2026-10-16  
**Status:** Source patch complete; full project compilation / Room integration not verified.  
**Language:** Roman Urdu + English.

## Implemented

1. **DP-008 — Discount-aware invoice refund allocation (source change).** New pure Kotlin `domain/model/RefundAllocation.kt` function uses the *actual invoice total* against the cumulative returned gross value, rounding cumulative values to paisa. Multiple partial returns reconcile to the charged invoice amount upon fully returning the invoice (provided original sale lines and subtotal reconcile).
2. **DB-enforced refund check.** `ReturnDao.commitReturn` fetches original invoice line snapshots within its `@Transaction`, computes existing returned gross plus proposed returned gross, and rejects mismatched refund money; sale-line, ownership, identity and quantity checks from Batch 01 remain.
3. **UI parity.** `SaleDetailViewModel.refundTotal()` now calculates discount-adjusted refund amounts using the same helper; `SaleReturn.allocatedRefund` passes the previewed amount through repository mapping. Loaded prior returns preserve the actual recorded refund via `Mappers.kt` so history does not incorrectly display the undiscounted line sum.
4. **New common tests.** `RefundAllocationTest.kt` covers whole discounted invoice, multiple partial returns, one-paisa rounding and excess quantity precondition.

## Executed checks

- **PASS:** Direct `kotlinc` compilation of standalone pure allocation helper plus smoke harness; JVM run verified full discounted return, cumulative rounding, invalid over-refund rejection.
- **NOT EXECUTED:** `:shared:jvmTest`, Room integration tests, Android/Desktop/iOS builds and CI; Gradle wrapper + local `gradle` missing.
- **NOT EXECUTED:** Real-device testing; intentionally kept as final release phase.

## Critical open work / production blockers

- **DP-001 / DP-003:** Provision an *official* Gradle wrapper 8.11.1 from a trusted Gradle-enabled host, then resolve build dependencies and compiler errors. Do not fabricate a wrapper JAR.
- **DP-007:** Refund allocation between original invoice paid amount, post-sale khata collections, customer credit, cash refund and settlement remains a policy blocker. The DAO currently only permits KHATA deduction up to the customer's current overall balance; this is NOT a complete invoice-specific settlement policy.
- **DP-013:** Room transactional integration tests are required for sale, purchase, return, ledger, negative paths, and rollback; new refund checks need real in-memory DB validation.
- **DP-011:** Restore still requires pre-restore recovery backup and stronger user confirmation / validation.
- **Financial reconciliation:** Verify item-level rounding for fractional quantities and invoices where stored subtotal may differ from sum of per-line rounded totals, and decide whether to reject inconsistency or normalize rules at sale capture.
- **Release:** UI automation, backup drills, signed RC, privacy checks and owner UAT still pending.

## Recommended next batch

1. Install official Gradle wrapper through a trusted build environment and run all JVM/Android build tasks; fix observed compile issues.
2. Implement Room integration tests and sale-level/payment-level reconciliation requirements.
3. Agree and implement a shop-safe return settlement model for partially paid / settled udhaar invoices.
4. Complete backup/restore safeguards; automation + release checks; **physical device UAT LAST**.

**Release decision: NO-GO** — do not deploy in a real shop yet. These are actual source edits with limited pure Kotlin smoke verification, **not** an end-to-end tested application.
