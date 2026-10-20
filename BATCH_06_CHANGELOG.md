# Batch 06 — Return settlement safety and build-readiness audit

## Implemented
- Introduced shared `validateCashRefund` policy with explicit input invariants. A cash refund is capped by original invoice `paidPaisa` less earlier **cash** refunds; the check runs inside `ReturnDao.commitReturn` transaction. This prevents giving out cash not recorded as received at checkout.
- Added Room query to calculate prior cash refunds for the SAME sale; previously all returns were counted only against invoice totals.
- Removed a redundant second stock validation loop from `SaleDao.commitSale`; earlier grouped-per-product validation inside the same transaction is authoritative and covers duplicate product lines.
- Added four settlement regression cases in `ReturnSettlementTest.kt`.

## Important limits / deliberately not claimed
- **Conservative policy:** Payments collected later on customer khata are not allocated to individual invoice sales by the current schema. Cash refunds on old udhaar invoices might therefore be rejected even where payments were later collected. An invoice-level payment allocation ledger and mixed cash/khata refund are future work.
- **Room integration tests and full Gradle builds are NOT verified** in this environment; adding source assertions is not equivalent to executing DB tests.
- No physical-device testing was performed; it remains last.
- Original invoice paid amount can include non-cash methods (Easypaisa, card): this cap is an anti-over-refund bound, NOT a payment-tender-specific payout policy. Cash drawer reconciliation and original-tender refund support remain pending.

## Commands to run with configured Gradle + Android SDK
`gradle :shared:jvmTest :desktopApp:compileKotlinJvm :androidApp:assembleDebug --stacktrace`

## Deployment gate: NO-GO
Full compilation, transaction integration tests, backup restore drills, payment-method refund policy and shop UAT still required.
