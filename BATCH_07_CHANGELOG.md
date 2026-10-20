# DukanPOS — Batch 07: Transaction integrity & reconciliation planning

Date: 2026-10-16 | Status: SOURCE PATCHED, FULL BUILD NOT VERIFIED | Deployment: NO-GO

## Changes actually made

- `shared/.../data/local/dao/ReturnDao.kt`: reject corrupt original sale-line quantities/prices before refund allocation. Explicitly reject missing products while returning stock, non-finite stock values, and vanished Khata customers rather than silently treating them as zero. All changes execute within existing Room `@Transaction` method.
- `shared/.../data/local/dao/SaleDao.kt`: reject missing product/customer in the mutation path and reject non-finite stock arithmetic within sale transaction, avoiding silent fallback to zero.
- Added this changelog and a separate Room/Khata verification checklist.

## Critical limitation: invoice-level Khata payments

`LedgerEntryEntity` has an optional `saleId`, but customer PAYMENT entries are customer-level; there is no persisted payment allocation to a specific invoice. **Therefore a subsequent Khata collection must NOT be assumed to increase one invoice's cash-refundable amount**. Batch 06's conservative limit stays intact. A genuine invoice-allocation feature requires a versioned database migration, explicit payment allocation rules (oldest-first vs operator-selected), allocation reversal for refunds, backup compatibility, and transactional end-to-end tests. It has NOT been implemented in this batch.

## Verification

- ZIP structure and integrity: checked during packaging.
- Gradle/JVM/Android compilation: NOT RUN (no Gradle executable or official wrapper in this environment).
- Room integration tests: NOT RUN (dependencies/build infrastructure unavailable).
- Real-device QA: NOT STARTED, remains LAST.

## Next ordered work

1. Obtain official Gradle wrapper; run JVM tests + Desktop compile + Android assemble; repair observed errors.
2. Build actual in-memory Room sale/return/payment/purchase transaction tests, including rollback assertions.
3. Design and migrate invoice payment allocations, including upgrade from legacy customer-level payments without guessing their invoice attribution.
4. Backup/restore recovery drills and functional release checklist.
5. Real devices only after all gates above pass.
