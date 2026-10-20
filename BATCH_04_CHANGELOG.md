# DukanPOS — Batch 04 Changelog

**2026-10-16 | Status: SOURCE UPDATED, UNVERIFIED BUILD, DEPLOYMENT NO-GO**

## Implemented
- Added `RestoreValidation.kt`: validates an untrusted restore snapshot **before** calling the existing Room `@Transaction replaceAll` operation.
- Rejects unsupported format, missing/multiple settings rows, duplicate primary keys and unique-index values, broken foreign references, invalid numeric values/quantities, invalid financial totals, over-refunded sales, and return quantities that disagree with stored sale lines.
- Connected restore entry point in `BackupRepositoryImpl.restoreJson()` to the new validator.
- Added five common-test regression cases in `RestoreValidationTest.kt` (authored, **not executed**).
- Preserved all Batch 01–03 modifications.

## Still not verified / remaining risk
- Full Gradle build, official wrapper bootstrap, Room in-memory integration tests, and test execution remain unavailable in this environment. No green build is claimed.
- Restore validation does NOT independently prove end-to-end ledger reconciliation, hash/integrity authenticity, or guarantee restoration from tampered backups. No encrypted backup or confirmation UX is implemented here.
- Some historic data produced by older bugs may be rejected by the stricter validator. Users must preserve original backups; do not overwrite them.
- Return cash-vs-khata settlement allocation for partially paid invoices needs a defined and tested shop policy (DP-007). Existing method checks remain.
- Real-device testing remains the final phase only.

## Next batch
- Build/toolchain bootstrap and compilation, followed by Room-backed database integration tests and khata reconciliation.

**Production release: NO-GO.**
