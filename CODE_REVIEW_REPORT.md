# DukanPOS — Complete Code Review Report

**Review date:** 2026-10-20
**Scope:** Poora repo — `shared/` (common + android + jvm + ios), `androidApp/`, `desktopApp/`, `iosApp/`, Gradle config, tests, docs, build outputs.
**Language:** Roman Urdu + English (technical terms English mein).
**Method:** Har source file line-by-line padhi gayi. Build outputs (`androidApp/build`, `shared/build/test-results`) bhi check kiye. Neeche har finding ke saath file aur line number diya gaya hai.

> **Note:** Is review mein Room ka koi integration test ya device test run nahi kiya gaya. Findings code padh kar nikali gayi hain; jahan cheez device par verify karni chahiye wahan **"(device par verify karein)"** likha hai.

---

## 1. Executive summary

| | |
|---|---|
| **Overall verdict** | Architecture mazboot hai, lekin **production ke liye abhi NO-GO**. 2 critical data-corruption bugs hain jo pehle din se shop ka data kharab kar denge. |
| **Critical (P0)** | 3 |
| **High (P1)** | 11 |
| **Medium (P2)** | 17 |
| **Low (P3)** | 20+ |
| **Feature gaps** | 25+ (sab se bara: weight/fractional qty sale, receipt printing, product delete, customer edit) |
| **Build status** | Android debug + release APK **16 Oct ko successfully build hue** the; 59 JVM tests pass hue the. Docs (`README.md`, `IMPLEMENTATION_PROGRESS.md`) abhi bhi "never compiled" bol rahe hain, wo purane hain. |

### Sab se pehle ye 5 kaam karein

1. **CR-01** — Product save karne par stock **double** ho jata hai. Opening stock 10 daalo to 20 save hota hai.
2. **CR-02** — Fresh install par jo **pehla backup** export hota hai wo **restore nahi ho sakta**.
3. **CR-04 / CR-05** — Reports mein profit aur "Cash" figures ghalat hain.
4. **CR-08** — Kiryana ka basic kaam, yani **0.5 kg / 2.5 kg** bechna, UI se mumkin hi nahi.
5. **CR-14** — Project **git mein nahi hai**. Pehle `git init` + commit karein, phir fixes karein.

---

## 2. App ko samajhna (Architecture overview)

### 2.1 Kya hai ye app
Pakistani kiryana shop ke liye **offline-first POS**: sale, stock, purchase (maal aamad), returns, udhaar khata, WhatsApp reminder, reports, JSON backup/restore aur CSV export. English + Urdu (RTL).

### 2.2 Tech stack
| Layer | Tech |
|---|---|
| Language / UI | Kotlin 2.1.0, Compose Multiplatform 1.7.3, Material 3 |
| Targets | Android (minSdk 26, target 35), Desktop (JVM), iOS (framework only, Xcode project nahi hai) |
| DB | Room KMP **2.7.0-alpha11** + bundled SQLite (**alpha**) |
| DI | Koin 4.0.0 |
| Navigation | JetBrains navigation-compose **2.8.0-alpha10** (type-safe routes) |
| Scanner | Android: CameraX + ML Kit · iOS: AVFoundation · Desktop: text field |
| Build | AGP 8.7.3, Gradle wrapper properties = 8.14.3 (CI = 8.11.1), KSP1 |

### 2.3 Layers
```
presentation (Compose screens + ViewModels, StateFlow, Channel effects)
      │
      ▼
domain (models, repository interfaces, use cases — framework-free)
      ▲
      │ implements
data (Room entities, DAOs with @Transaction write paths, mappers, repo impls)
```

### 2.4 Main flows
| Flow | Path |
|---|---|
| Sale | `PosScreen` → `PosViewModel.onConfirmCheckout` → `CheckoutUseCase` → `SaleRepositoryImpl` → `SaleDao.commitSale` (@Transaction: invoice counter, sale, items, stock decrement, movements, khata ledger) |
| Return | `SaleDetailScreen` → `SaleDetailViewModel` → `RecordSaleReturnUseCase` → `ReturnDao.commitReturn` (discount-aware refund allocation, cash cap, khata cap) |
| Purchase | `PurchaseScreen` → `RecordPurchaseUseCase` → `PurchaseDao.commitPurchase` (stock in, optional cost update, supplier upsert) |
| Khata payment | `CustomerDetailScreen` → `RecordCustomerPaymentUseCase` → `CustomerDao.applyLedgerDelta` |
| Backup | `SettingsViewModel` → `BackupRepositoryImpl.exportJson / restoreJson` → `validateRestoreSnapshot` → `BackupDao.replaceAll` |
| Reports | `ReportRepositoryImpl` mein kai Room flows `combine` hote hain, aggregation SQL mein hoti hai |

### 2.5 Strengths (jo acha hai)
- **Money = `Long` paisa** value class, kahin Double nahi. Bohat acha decision hai.
- Sale / return / purchase har ek **single `@Transaction`** mein hai, aur stock DAO ke andar dobara check hota hai.
- Invoice number **counter table** se aata hai, `COUNT(*)+1` se nahi.
- Sale lines par **price aur cost ka snapshot** save hota hai.
- Return refund **discount-aware** hai aur cumulative rounding sahi hai (`RefundAllocation.kt`).
- Restore se pehle **validation** hoti hai (`RestoreValidation.kt`): FK, unique, numeric checks.
- Overflow-safe ledger math hai (`LedgerMath.kt`).
- Strings data class mein hain aur Urdu override karta hai (missing key English par fallback kar jati hai).
- Domain layer framework-free hai, isliye use-case tests fast chalte hain.

---

## 3. Findings — Critical (P0)

### CR-01 · Product save karne par stock DOUBLE ho jata hai, aur concurrent sales overwrite ho jati hain
**Files:** `domain/usecase/SaveProductUseCase.kt:39-50`, `data/repository/ProductRepositoryImpl.kt:50-54, 65-84`, `presentation/products/ProductEditViewModel.kt:74,107`

**Masla:**
1. `repository.upsert(normalized)` poori row likhta hai, **`stockQty` samet** (Room `@Upsert` full entity).
2. Uske baad `adjustStock(delta)` chalta hai, jo `UPDATE products SET stockQty = stockQty + :delta` karta hai. Yani wahi delta **dobara** add ho jata hai.

**Scenario:**
- Naya product, opening stock = 10 → insert 10 → phir +10 → **DB mein 20**. Movement bhi `qtyAfter = 20` likhta hai.
- Existing product 10 → user 15 karta hai → upsert 15 → phir +5 → **20**.
- **Lost update:** Edit screen 10 par khuli thi, beech mein 2 sale ho gayi (DB = 8). User save karta hai (price change, stock untouched = "10"): upsert 10 likhta hai (2 sales ka decrement mit gaya), delta = 10−8 = 2 → +2 → **12**.

**Impact:** Pehle din se har product ka stock ghalat hoga. Stock value, low-stock alerts aur negative-stock protection sab ghalat ho jayenge.

**Fix:**
- Product upsert mein `stockQty` **mat likho**. Product update ke liye alag `@Query UPDATE products SET name=…, costPaisa=…, salePaisa=… WHERE id=:id` banao jo stock ko touch na kare.
- Insert ke waqt row `stockQty = 0` se banao, phir `OPENING` movement ke through delta lagao.
- Stock edit ko explicit **"Stock adjust"** action banao (reason: count correction / wastage), aur usay ek `@Transaction` DAO method mein rakho (update + movement ek saath).
- `SaveProductUseCase` ke liye test add karo (abhi koi test nahi hai, isi wajah se ye bug pakda nahi gaya).

---

### CR-02 · Fresh install par pehla backup restore NAHI hota
**Files:** `data/repository/SettingsRepositoryImpl.kt:22-24`, `data/local/dao/BackupDao.kt:111-126`, `data/repository/RestoreValidation.kt:8`, `presentation/settings/SettingsViewModel.kt:99-103`

**Masla:**
- `shop_settings` table mein row tab tak nahi banti jab tak user koi setting change na kare (`get()` sirf in-memory default return karta hai).
- Export → `settings = []`.
- Restore validation `require(s.settings.size == 1)` → **"Backup requires exactly one valid shop settings row"** → restore fail.
- `runExport` mein `repository.update(repository.get())` settings ko persist karta hai, lekin **export ke BAAD**. Isliye pehli file hamesha kharab hoti hai, aur doosri export theek hoti hai.
- `RestoreValidationTest.missingSettingsRejected` isi bug ko "expected behaviour" ke taur par lock kar raha hai.

**Impact:** Offline app mein backup hi wahid safety net hai. Shopkeeper ne setup ke baad backup liya, phone kho gaya, aur restore fail. Error message bhi generic ("Restore failed") hai.

**Fix:**
- App start par (ya DB `onCreate` callback mein) settings row seed karo.
- Restore mein `settings.isEmpty()` ho to default settings row accept kar lo.
- `runExport` wala hack hata do.
- Test add karo: fresh DB → export → restore = success.

---

### CR-03 · Android: backup/restore ke dauran screen rotate ho to job hamesha ke liye atak jata hai
**Files:** `androidApp/.../MainActivity.kt:14-29, 37-52`, `shared/src/androidMain/.../PlatformActions.android.kt:15-56`

**Masla:**
- `pendingCreate/pendingOpen` Activity ke instance fields hain.
- File picker khula ho aur Activity recreate ho jaye (rotation, dark-mode toggle, low-memory kill) to purani Activity ka `onDestroy` bridge clear kar deta hai.
- Result nayi Activity ko milta hai jiska `pendingCreate == null` hai, isliye result **drop** ho jata hai.
- `suspendCancellableCoroutine` **kabhi resume nahi hota**. `SettingsViewModel._job` hamesha `EXPORTING/RESTORING` rehta hai aur saare backup buttons app restart tak disabled rehte hain.
- Purani Activity ka lambda bhi continuation mein leak hota hai.

**Fix:** Pending continuation ko process-level object (ya `SavedStateHandle` + request id) mein rakho, aur `rememberLauncherForActivityResult` ko Compose side par move karo. Kam se kam `onDestroy` mein pending continuation ko `resume(null)` kar do taake job unlock ho jaye.

---

## 4. Findings — High (P1)

### CR-04 · Discounted invoice return hone par profit DO DAFA kam ho jata hai
**Files:** `data/repository/ReportRepositoryImpl.kt:80`, `data/local/dao/ReportDao.kt:104-114`

`returnedProfit = (unitPrice − unitCost) × qty` hai, yani discount ignore hota hai, jabke refund discount-allocated hota hai.

**Example:** Subtotal 100, cost 80, discount 10. Profit = 20 − 10 = **10**. Poora return: refund 90, returnedProfit 20 → profit = 20 − 10 − 20 = **−10**. Sahi jawab **0** hai.

**Fix:** returned profit = `refundPaisa − Σ(unitCost × qty)` (refund pehle se allocated hai).

### CR-05 · Reports ka "Cash" figure ghalat hai
**Files:** `ReportRepositoryImpl.kt:78`, `ReportDao.kt:75-82`
- `cashCollected = SUM(paidPaisa) − all refunds`. **KHATA refunds bhi minus ho rahe hain**, halanke unme cash gaya hi nahi.
- `paidPaisa` mein **Easypaisa / JazzCash / Bank / Card** bhi shamil hain, phir bhi tile ka label "Cash" hai.
- Khata **payments jo receive hui** (asli cash in hand) **shamil hi nahi** hain.

**Impact:** Din ke end par galla (drawer) kabhi match nahi karega.
**Fix:** Payment method ke hisaab se breakdown do. Cash = cash sales paid + khata cash payments − cash refunds.

### CR-06 · Dashboard aur Reports "aaj" ko midnight ke baad update nahi karte
**Files:** `ReportRepositoryImpl.kt:34-37`, `DashboardViewModel.kt:25-32`, `ReportsViewModel.kt:35-38`

`today` sirf ek dafa Flow banate waqt calculate hota hai. Dashboard start destination hai aur uska ViewModel kabhi clear nahi hota. Jo dukaan app raat bhar khuli chhorti hai, wo agle din "Today's sale" mein **kal ka data** dekhegi.
**Fix:** Ek `currentDate` ticker flow (har minute, ya date change par) se `flatMapLatest` karo.

### CR-07 · Udhaar invoice ka return mumkin nahi rehta jab customer khata clear kar de (DP-007 abhi open hai)
**Files:** `data/local/dao/ReturnDao.kt:143-150`, `domain/model/ReturnSettlement.kt`

Udhaar sale 1000 → customer ne baad mein 1000 payment de di (balance 0) → return:
- CASH: cap = invoice `paidPaisa` = 0 → reject
- KHATA: cap = current balance = 0 → reject

**Result:** Return kisi bhi tarah record nahi ho sakta. Error raw English mein dikhta hai (`"Refund exceeds outstanding khata; use cash refund policy"`).
**Fix:** Business policy decide karo (customer credit/advance allow karo, ya cash refund ko us invoice par lagne wale khata payments tak allow karo), phir UI mein pehle se batao ke kaunsa method allowed hai.

### CR-08 · Weight / fractional quantity bechna UI se mumkin hi nahi
**Files:** `presentation/pos/PosScreen.kt:230-235`, `core/designsystem/Components.kt:118-151`, `PurchaseScreen.kt:230-235`, `SaleDetailScreen.kt:107-112`

Qty sirf `QtyStepper` ±1 se badalti hai, koi qty input nahi hai. `ProductUnit.allowsFraction` kahin use nahi hota. Kiryana ka core use case (0.25 kg, 1.5 kg daal, ya Rs 50 ki cheeni) block hai. Purchase mein 48 carton daalne ke liye 47 taps lagte hain.
**Fix:** Qty par tap karne se numeric dialog khule. Fractional units ke liye "by amount" option bhi do (Rs 50 ka → qty = 50 / price).

### CR-09 · Android: scanner band karne ke baad bhi camera chalta rehta hai
**File:** `shared/src/androidMain/.../BarcodeScanner.android.kt:71-76, 115-121`

`onDispose` sirf executor aur scanner band karta hai, **`provider.unbindAll()` nahi** karta. Camera NavBackStackEntry (POS screen) ke lifecycle se bound rehta hai, isliye POS screen par camera ON rehta hai (battery, privacy indicator), aur analyzer shut-down executor par chalne ki koshish karta hai, jis se `RejectedExecutionException` aa sakti hai. (Device par verify karein.)
**Fix:** `onDispose { cameraProvider?.unbindAll() }`, aur `onBarcode` ke liye `rememberUpdatedState` use karo.

### CR-10 · Purchase screen ke cost field mein decimal type nahi hota
**File:** `presentation/purchase/PurchaseScreen.kt:241-244`

`value = line.unitCost.rupees.toString()`, aur `onValueChange` Money parse karke wapas likhta hai. User "12." type kare to wo "12" ban jata hai aur **point gayab** ho jata hai. Field khali karo to "0" aa jata hai. Ghalat input bhi khamoshi se 0 ho jata hai.
**Fix:** Har line ke liye local `TextFieldValue` / string state rakho, aur parse sirf valid hone par karo.

### CR-11 · Stock aur Purchase list sirf 200 products dikhati hain
**File:** `data/local/dao/ProductDao.kt:35` (`LIMIT 200`)

Stock screen ek "sab products" list hai, lekin 201st product search ke baghair kabhi nazar nahi aata, aur koi indication bhi nahi hai. Aam kiryana mein 500–2000 SKU hote hain.
**Fix:** Paging3 (ya kam se kam empty query par limit hatao, ya "X more, search karein" footer dikhao).

### CR-12 · Sales history sirf aakhri 100 invoices dikhati hai, search/filter nahi hai
**File:** `presentation/sales/SalesHistoryViewModel.kt:15`

Purani invoice tak pohanchne ka koi rasta nahi hai, isliye **purani sale ka return nahi ho sakta**. `observeSales(from, until)` repo mein maujood hai lekin unused hai.
**Fix:** Date range + invoice no / customer search + paging.

### CR-13 · Unhandled exceptions se app crash ho sakti hai
| Location | Masla |
|---|---|
| `SaveProductUseCase.kt` (poora) | `upsert` / `adjustStock` exception par try/catch nahi hai. `viewModelScope` mein uncaught exception Android par **app crash** karta hai |
| `KhataViewModel.kt:42-53` | `addCustomer` mein error handling nahi hai |
| `SettingsViewModel.kt:115-117` | `update` mein error handling nahi hai |
| `PosViewModel.kt:105-115` | barcode lookup DB error par try/catch nahi hai |
| `CheckoutUseCase.kt:44` | `settingsRepository.get()` try ke bahar hai |

**Fix:** Use-case boundary par `runCatching` lagao, ya ek `CoroutineExceptionHandler` + `AppError.Unexpected` effect use karo.

### CR-14 · Project git mein nahi hai
Root mein `.git` nahi hai. Itni batches (01–07) ke source patches bina version control ke lage hain, isliye koi diff, rollback ya review trail nahi hai. `gradlew` / `gradlew.bat` / `gradle-wrapper.jar` bhi missing hain (sirf `.properties` hai).
**Fix:** `git init`, pehla commit current state ka karo, phir official wrapper generate karke commit karo. `.gitignore` mein `hs_err_pid*.log`, `/build`, `*/build` add karo.

---

## 5. Findings — Medium (P2)

| ID | Masla | File:line | Fix |
|---|---|---|---|
| CR-15 | **Checkout discount field reopen par khali dikhta hai** lekin cart mein discount lagu rehta hai. Total kam dikhta hai aur field "" | `pos/CheckoutSheet.kt:57,121-125` | `discountText` ko `cart.discount` se initialize karo |
| CR-16 | Discount > subtotal ya negative discount ka UI mein koi check nahi. DAO `require` fail karta hai aur user ko raw English "Invalid sale totals" dikhta hai | `CheckoutSheet.kt:121-125`, `CheckoutUseCase.kt`, `SaleDao.kt:98-99` | Use case mein `0 ≤ discount ≤ subtotal` validate karo, localized message do |
| CR-17 | `Money.parseOrNull("NaN")` par `roundToLong()` **crash** karta hai. Checkout mein ye composition ke dauran call hota hai (`val paid = …`). Desktop / hardware keyboard par reproducible. `"Infinity"` overflow karta hai | `core/common/Money.kt:63-68` | `toDoubleOrNull()?.takeIf { it.isFinite() }` + max range check |
| CR-18 | Error messages sentence nahi, sirf field labels hain: NAME_REQUIRED → "Name", QTY_MUST_BE_POSITIVE → "Qty", PAYMENT_EXCEEDS_DUE → "Enter a valid amount". `Unexpected` raw exception message (English, technical) dikhata hai | `presentation/common/ErrorText.kt:14-36` | Har MessageKey ke liye proper EN/UR sentence. DAO `require` failures ko typed errors mein map karo |
| CR-19 | Settings text fields har keystroke par async DB write karte hain aur value DB flow se aati hai, isliye fast typing par **characters drop / cursor jump** hote hain. Har keystroke `get()` + `upsert` ka race hai | `settings/SettingsScreen.kt:135-162`, `SettingsViewModel.kt:115-117` | Local form state + debounce/save button |
| CR-20 | iOS `saveTextFile` share sheet present karte hi `true` return kar deta hai. User cancel kare tab bhi "Done" dikhta hai, aur backup asal mein save nahi hua | `iosMain/.../PlatformActions.ios.kt:40-47` | `completionWithItemsHandler` se suspend karo |
| CR-21 | Android `openOutputStream(uri)` default mode `"w"` hai, jo kuch providers (Drive / Android 10+) par **truncate nahi karta**. Existing badi file overwrite ho to JSON ke aakhir mein purana garbage reh jata hai aur backup corrupt hota hai | `PlatformActions.android.kt:38` | `openOutputStream(uri, "wt")` |
| CR-22 | Android Auto-Backup sirf `dukan.db` include karta hai, **WAL (`-wal`, `-shm`) nahi**. Cloud restore par recent transactions missing ya inconsistent ho sakti hain. Khata PII bhi Google cloud mein jata hai | `res/xml/backup_rules.xml`, `data_extraction_rules.xml` | WAL files include karo, ya backup se pehle checkpoint karo. Privacy decision document karo |
| CR-23 | Restore fail hone ka **reason user ko nahi dikhta**. Restore se pehle current DB ki **automatic safety copy** nahi banti | `SettingsViewModel.kt:78-93` | Error reason dikhao. Restore se pehle auto-export karo |
| CR-24 | Room **schema JSON export nahi ho raha**. `room.schemaLocation` relative `"schemas"` KSP arg hai aur repo mein schema file nahi mili. Room Gradle plugin root mein declared hai lekin `shared` mein apply nahi hai. v1 → v2 migration likhna aur test karna mushkil hoga | `shared/build.gradle.kts:100` | `plugins { alias(libs.plugins.room) }` + `room { schemaDirectory("$projectDir/schemas") }`, aur schema commit karo |
| CR-25 | **Alpha dependencies** production mein: Room 2.7.0-alpha11, sqlite 2.5.0-alpha11, navigation 2.8.0-alpha10. KSP1 forced (`ksp.useKSP2=false`) | `gradle/libs.versions.toml` | Room 2.7.x stable + matching sqlite, navigation stable |
| CR-26 | Product form validation kamzor hai: negative stock / low-stock accept hota hai; `PIECE` unit par 2.5 stock; cost ghalat type ho to **khamoshi se 0**; sale price < cost par koi warning nahi (`SALE_PRICE_BELOW_COST` defined hai, use nahi hota) | `SaveProductUseCase.kt:19-29`, `ProductEditViewModel.kt:105-108` | Proper field validation |
| CR-27 | Checkout mein customer selection **saare customers ke horizontal chips** hain. Search nahi, aur checkout se naya customer add nahi ho sakta. 100+ customers par ye unusable hai | `CheckoutSheet.kt:97-117` | Searchable picker + "naya customer" |
| CR-28 | Desktop: `BarcodeScannerView.jvm` **kabhi dikhta hi nahi** (`isCameraScanningSupported()=false` button chhupa deta hai). USB scanner search field mein type karta hai, lekin **Enter par product cart mein add nahi hota** | `PosScreen.kt:135`, `jvmMain/.../BarcodeScanner.jvm.kt` | Search field par `onKeyEvent(Enter)` → exact barcode match → auto add |
| CR-29 | **Security**: koi app lock/PIN nahi, koi role nahi (cashier bhi profit dekh sakta hai aur prices/stock edit kar sakta hai). Backup JSON plain text hai aur customer phone numbers (PII) ke saath hai | — | Owner PIN, profit/edit ko PIN ke peeche rakho, optional encrypted backup |
| CR-30 | Reports: daily series (7-day bars) aur Top products **returns se net nahi** hain, jabke summary net hai, isliye numbers aapas mein match nahi karte. `salesCount` mein fully returned sales bhi count hoti hain | `ReportDao.kt:37-73` | Returns ko series / top products mein bhi subtract karo |
| CR-31 | Restore ke baad POS ViewModel ka **purana cart** (purane product objects / IDs) zinda rehta hai. Restored DB mein wahi ID kisi aur product ki ho sakti hai, isliye sale ghalat product ka stock kam kar sakti hai | `PosViewModel.kt` | Restore ke baad app restart / saare VM state reset karo |

---

## 6. Findings — Low (P3)

| ID | Masla | File:line |
|---|---|---|
| CR-32 | Return ka `ReturnConflictException` `AppError.NotEnoughStock` mein map hota hai, isliye user ko "Out of stock" message dikhta hai | `RecordSaleReturnUseCase.kt:33-34` |
| CR-33 | WhatsApp na ho (`NoApp`) to bhi "phone number nahi hai" wala message dikhta hai | `CustomerDetailViewModel.kt:70` |
| CR-34 | Payment record karte hi amount/note clear ho jate hain, chahe payment **fail** ho jaye. Note field reminder button ke neeche hai, isliye confusing hai | `CustomerDetailScreen.kt:129-166` |
| CR-35 | Unknown barcode → "Add" product screen kholta hai lekin **barcode pre-fill nahi** hota, user ko dobara scan karna padta hai | `PosScreen.kt:80-86`, `Routes.kt:17` |
| CR-36 | Reports mein Month range par bhi header **"Last 7 days"** dikhta hai | `ReportsScreen.kt:139` |
| CR-37 | Sales history / sale detail mein "Due" pill invoice-level hai. Baad mein khata payment aaye tab bhi hamesha "Due" dikhata hai | `SalesHistoryScreen.kt:109-114` |
| CR-38 | Sale detail par discount, payment method aur **returns history** (`returns` StateFlow screen collect hi nahi karti) nahi dikhte. Receipt re-share nahi ho sakti | `SaleDetailScreen.kt` |
| CR-39 | `PurchaseItemEntity.toDomain()` unit hamesha `PIECE` set karta hai | `data/mapper/Mappers.kt:241` |
| CR-40 | `PurchaseItem.updatesCostPrice` ka KDoc kehta hai "Off by default" lekin default `true` hai | `domain/model/Purchase.kt:33-37` |
| CR-41 | Supplier name exact / case-sensitive match hota hai, isliye "Ali Traders" aur "ali traders" do suppliers ban jate hain | `PurchaseDao.kt:35-36, 82-87` |
| CR-42 | Search: `searchText` index BINARY collation ka hai, isliye `LIKE` (case-insensitive) us index ko use **nahi** kar sakta. OR ke saath infix `%…%` bhi hai, yani full scan hota hai. Doc comment claim ghalat hai. User input mein `%`/`_` escape nahi hote | `ProductDao.kt:17-38` |
| CR-43 | CSV: formula injection (`=`, `+`, `-`, `@` se shuru hone wale naam). `stockQty.toString()` scientific notation (`1.0E-4`) de sakta hai. Sales CSV mein discount / returns column nahi | `BackupRepositoryImpl.kt:37-106` |
| CR-44 | CSV export 3 alag save dialogs kholta hai, aur sirf ek save ho tab bhi "Done" dikhata hai (`products \|\| sales \|\| khata`) | `SettingsViewModel.kt:64-76` |
| CR-45 | Checkout: discount badlo to "Received" field ka typed amount reset ho kar total ban jata hai | `CheckoutSheet.kt:62-67` |
| CR-46 | Purchase save ke baad `showSnackbar` (≈4s) suspend karta hai, phir back jata hai | `PurchaseScreen.kt:73-76` |
| CR-47 | iOS scanner: camera permission request / denied UI nahi hai. `session.startRunning()` main thread par chalta hai (Apple warning). `keyWindow` deprecated hai | `iosMain/.../BarcodeScanner.ios.kt` |
| CR-48 | Hardcoded strings jo translate nahi hote: "Camera ki ijazat dein", "Barcode scan karein…", "Close scanner", "Reduce / Increase quantity", theme names (System/Light/Dark), "Save backup" dialog titles | multiple |
| CR-49 | `Clock` injection inconsistent hai: `KhataViewModel`, `SaleDetailViewModel`, `SettingsViewModel` `Clock.System` direct use karte hain; screens `SystemClock()` banati hain. Tests mein time control nahi hota | — |
| CR-50 | `SendUdhaarReminderUseCase` `settingsRepository.get()` call karta hai aur result use nahi karta. `allowOverpayment` parameter dead hai | `BuildReminderUseCase.kt:22`, `RecordCustomerPaymentUseCase.kt:19` |
| CR-51 | `ProductRepositoryImpl.adjustStock` transaction mein nahi hai (update → read → insert movement) | `ProductRepositoryImpl.kt:65-84` |
| CR-52 | APK size: debug 49 MB / release 31 MB. `materialIconsExtended` + bundled ML Kit model | `shared/build.gradle.kts` |
| CR-53 | Nested `Scaffold` + `enableEdgeToEdge()`: outer Scaffold padding + inner TopAppBar insets ki wajah se top par **double status-bar gap** ho sakta hai (device par verify karein) | `DukanNavHost.kt:45-84` |
| CR-54 | `Customer` doc kehta hai "negative = advance paid", lekin DAO negative balance reject karta hai. Model aur policy mismatch hai | `domain/model/Customer.kt:7-9`, `CustomerDao.kt:72` |
| CR-55 | Docs stale hain: README / IMPLEMENTATION_PROGRESS "not compiled" kehte hain jabke APK ban chuke hain. CI Gradle 8.11.1 use karta hai, wrapper 8.14.3. Root mein `hs_err_pid7140.log` (Gradle daemon OOM, 1.4 MB) para hai. `gradle.properties` mein "Gradle 9.4+" comment irrelevant hai | root |

---

## 7. Dead code / adha-bana features

Ye cheezen code mein hain lekin UI se reachable nahi hain. Inhe complete karo ya hata do:

| Cheez | Kahan | Status |
|---|---|---|
| `receiptFooter`, `ownerName`, shop `phone`/`address` | ShopSettings | Save hote hain, lekin **koi receipt hi nahi hai** |
| `invoicePrefix`, shop `address` | ShopSettings | Settings UI mein editable nahi |
| `PosViewModel.onUnitPriceChange` / `Cart.setUnitPrice` | POS | Cart mein price change ka UI nahi |
| `Product.displayName(preferUrdu)` | Product | Urdu naam kabhi kahin nahi dikhta |
| `ProductRepository.setActive` | — | Product delete/deactivate ka UI nahi |
| `upsertCategory`, `observeCategories` | — | Category banane ka UI nahi, isliye category chips kabhi nahi aate |
| `observeMovements` | — | Stock history screen nahi |
| `PurchaseRepository.observeRecent`, `getPurchase`, `observeSuppliers` | — | Purchase history aur supplier list nahi |
| `CustomerRepository.recordAdjustment`, `OPENING_BALANCE` | — | Opening balance / manual adjustment ka UI nahi |
| `Customer.creditLimit`, `isOverLimit` | — | Credit limit kahin enforce nahi hoti |
| `ProductsViewModel.onlyLowStock` | — | Toggle wired nahi; dashboard ka "Low stock" tile filter ke baghair stock screen kholta hai |
| `MessageKey.SALE_PRICE_BELOW_COST` | — | Kabhi emit nahi hota |
| `SalesSummary.averageTicket`, `discountGiven` | — | UI mein nahi |
| `BarcodeScannerView` (desktop) | jvmMain | Unreachable |
| `turbine` dependency | commonTest | Use nahi hoti |

---

## 8. Feature gaps (kiryana POS ke liye kya missing hai)

**Must-have (launch se pehle):**
1. Fractional / weight / amount-based quantity entry (CR-08)
2. **Receipt**: share as image/PDF ya WhatsApp, aur thermal printer (ESC/POS Bluetooth)
3. Product **delete / deactivate**, inactive products dekhna aur reactivate karna
4. Customer **edit / delete / phone update**, **opening balance** (purana khata register se manually daalna)
5. Sale **void/cancel** (ghalat sale foran undo karna). Abhi sirf return hai
6. Cart mein **price edit** (bhao-taao) aur line discount
7. Sales history search + date filter + paging (CR-12)
8. Stock **adjustment with reason** (wastage, expiry, count correction). `WASTAGE` type maujood hai, UI nahi
9. Backup **reminder/nag**: last backup date dikhao, aur X din baad yaad dilao (README khud is gap ko list karta hai)
10. Day close / **cash drawer** report (opening cash, cash in, cash out, expected vs counted)

**Should-have:**

11. Supplier khata (payables): purchase ka paid/udhaar, supplier ko payment
12. Expenses (bijli, kiraya, salary) aur net profit
13. Categories management UI
14. Purchase history + purchase return (supplier ko maal wapas)
15. Stock movement history per product
16. Customer credit limit warning at checkout
17. Customer advance / credit balance support
18. Hold / park sale (do customers ek saath)
19. Continuous scan mode (abhi har scan ke baad scanner band ho jata hai)
20. Low-stock list / reorder list share karna (WhatsApp to supplier)
21. Owner PIN + cashier mode (CR-29)
22. Urdu product names counter par dikhana (`displayName`)

**Nice-to-have:**

23. Optional cloud sync / multi-device (abhi ek hi device hai, "two tills" comments ke bawajood)
24. Product images, Excel/CSV **import** (bulk product entry)
25. FBR / GST invoice (bari dukaanon ke liye)
26. Expiry date / batch tracking (dawai / dairy)

---

## 9. Testing gaps

| Area | Abhi | Chahiye |
|---|---|---|
| Domain use cases | Checkout, Payment guard, Purchase, Return, Refund allocation (fakes ke saath) | **`SaveProductUseCase`** (CR-01 isi wajah se pakda nahi gaya), `RecordSaleReturnUseCase` error mapping |
| Room DAO (in-memory) | **Zero** | `commitSale` / `commitReturn` / `commitPurchase` / `applyLedgerDelta` rollback tests (`ROOM_AND_KHATA_TEST_MATRIX.md` ke 14 scenarios) |
| Backup round-trip | Sirf validator unit tests | Fresh DB → export → restore = success (CR-02), bari DB ka export |
| Reports | **Zero** | Discount + return profit (CR-04), cash breakdown (CR-05), timezone bucketing |
| ViewModels | **Zero** | Checkout flow, return draft, settings (Turbine dependency pehle se maujood hai) |
| UI / instrumentation | **Zero** | Sale → return → report smoke test |
| Platform | Zero | SAF rotation (CR-03), scanner lifecycle (CR-09) |

**Note:** `shared/build/test-results` ke mutabiq 16 Oct ko **59 tests pass** hue the. Ye sab pure-Kotlin tests hain; koi DB test nahi hai.

---

## 10. Build / release checklist

- [ ] `git init` + `.gitignore` update (CR-14)
- [ ] Official Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) commit karo. CI aur wrapper ka version ek karo
- [ ] `gradle.properties`: `-Xmx4096M` daemon + `kotlin.daemon -Xmx3072M` ki wajah se is machine par OOM crash hua (`hs_err_pid7140.log`, "Failed to commit metaspace"). RAM ke hisaab se kam karo ya swap barhao
- [ ] Room plugin apply karo + schema export (CR-24)
- [ ] Alpha → stable dependencies (CR-25)
- [ ] Release `signingConfig`, `versionCode` strategy
- [ ] R8 release APK ko device par smoke test karo (Koin / Room / serialization keep rules verify karo)
- [ ] Crash reporting (offline-friendly, e.g. local log file export)
- [ ] Privacy policy (camera, contacts phone numbers, cloud backup)

---

## 11. Recommended fix order (roadmap)

**Phase 0 — Safety (1–2 din)**
- CR-14 git + wrapper
- CR-01 stock double-count fix + `SaveProductUseCase` tests
- CR-02 settings seed + restore round-trip test
- CR-03 SAF bridge fix, CR-21 `"wt"` mode

**Phase 1 — Correctness (3–5 din)**
- CR-04, CR-05, CR-30 reports fix + report tests
- CR-06 date ticker
- CR-13 exception handling
- CR-16, CR-17, CR-18 validation + proper messages
- CR-09 camera unbind, CR-10 cost field, CR-15 discount field, CR-19 settings form
- Room in-memory DAO tests (matrix DB-01…DB-12)

**Phase 2 — Kiryana usability (1–2 hafte)**
- CR-08 fractional/amount qty, cart price edit
- Receipt share/print
- Product deactivate, customer edit + opening balance, sale void
- CR-11, CR-12 paging + sales search
- CR-27 customer picker, CR-28 desktop Enter-to-add
- Backup reminder + pre-restore auto copy (CR-23)

**Phase 3 — Business policy (product decision chahiye)**
- CR-07 / DP-007 return settlement + customer credit (CR-54)
- Supplier khata, expenses, day close
- Owner PIN / roles (CR-29)

**Phase 4 — Release**
- Stable deps, schema export, signing, R8 smoke test, **real-device UAT sab se aakhir mein**

---

*End of report.*
