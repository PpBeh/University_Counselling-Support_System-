# OOP Notes — How the Refactor Meets the Brief

**Project:** University Counselling Appointment & Support System (AMCS2194)  
**Purpose:** assessor-friendly index of every OOP construct used in the final
Android code, with the exact files to open.

This is not a second narrative of the app. It is the evidence map: what the
concept means here, where it is implemented, and why it is genuine OOP rather
than a renamed data class.

---

## 0. The one-sentence version

The old code passed primitive rows and string roles through ViewModels. The
final code has a **pure Kotlin domain model** (`core/domain` and
`feature/*/domain`), **interfaces for every feature repository**, **Supabase
implementations that adapt DTO rows into those domain objects**, and
**ViewModels/UI that depend only on the interfaces**.

---

## 1. Encapsulation

> A class protects its own rules and exposes only meaningful behaviour.

| Where | What is protected | How |
|---|---|---|
| [`core/domain/ValueObjects.kt`](../app/src/main/java/com/example/counselling/core/domain/ValueObjects.kt) | `Email`, `PhoneNumber`, `Password`, `StudentId`, `TimeSlot`, `Rating`, `ReferenceCode` | Private constructors + `of()` factories enforce format/range once. UI cannot create an invalid `Email`. |
| [`core/domain/User.kt`](../app/src/main/java/com/example/counselling/core/domain/User.kt) | `User` identity, name, permissions, role-specific home tabs | Read-only `val`s; `UserPermissions` has private constructor and boolean capabilities. |
| [`feature/appointment/domain/Appointment.kt`](../app/src/main/java/com/example/counselling/feature/appointment/domain/Appointment.kt) | Reschedule/cancel/status rules | Rules are methods on the entity, not UI helpers. Fields are `val`; every change returns a new `Appointment`. |
| [`feature/records/domain/SessionRecord.kt`](../app/src/main/java/com/example/counselling/feature/records/domain/SessionRecord.kt) | Private-note length, public-summary length, 7-day edit window, archive/restore | `init` validation + `canBeEditedAt` + `withContent`; callers cannot mutate a record in place. |
| [`feature/resource/domain/Resource.kt`](../app/src/main/java/com/example/counselling/feature/resource/domain/Resource.kt) | Resource title/description/bracket invariants | Common `Resource` initializer; subclass-specific fields remain final. |
| [`feature/enquiry/domain/Enquiry.kt`](../app/src/main/java/com/example/counselling/feature/enquiry/domain/Enquiry.kt) | Reference/message rules and anonymous identity boundary | Base initializer plus sealed subclasses; an anonymous enquiry has no owner property. |
| [`feature/counsellor/domain/CounsellorRepository.kt`](../app/src/main/java/com/example/counselling/feature/counsellor/domain/CounsellorRepository.kt) | Removal outcome | `CounsellorRemovalResult` is a sealed result, so callers must handle both `Deleted` and `Deactivated`. |

`Password` deliberately provides `reveal()` as an `internal` method. It is not a
public `value` property, so logging/compose code cannot print the raw password by
accident.

---

## 2. Inheritance

> A specialised class reuses a general superclass and adds its own state.

| Superclass | Subclasses | File |
|---|---|---|
| `User` (sealed) | `Student`, `Counsellor`, `Administrator` | [`User.kt`](../app/src/main/java/com/example/counselling/core/domain/User.kt) |
| `Resource` (sealed) | `ArticleResource`, `VideoResource`, `AudioResource`, `PdfResource`, `HotlineResource` | [`Resource.kt`](../app/src/main/java/com/example/counselling/feature/resource/domain/Resource.kt) |
| `Enquiry` (sealed) | `IdentifiedEnquiry`, `AnonymousEnquiry` | [`Enquiry.kt`](../app/src/main/java/com/example/counselling/feature/enquiry/domain/Enquiry.kt) |
| `AppointmentStatus` (sealed) | `Pending`, `Confirmed`, `Completed`, `NoShow`, `Cancelled` | [`AppointmentStatus.kt`](../app/src/main/java/com/example/counselling/core/domain/AppointmentStatus.kt) |
| `BaseSupabaseRepository` (abstract) | every `Supabase*Repository` | [`BaseSupabaseRepository.kt`](../app/src/main/java/com/example/counselling/core/data/BaseSupabaseRepository.kt) |
| `BaseViewModel` (abstract) | every converted feature ViewModel | [`BaseViewModel.kt`](../app/src/main/java/com/example/counselling/core/ui/BaseViewModel.kt) |

The **database** mirrors this with class-table inheritance:

```text
users (supertype)            resources (supertype)         enquiries (supertype)
  ├── students                 ├── article_resources          ├── identified_enquiries
  ├── counsellors              ├── video_resources            └── anonymous_enquiries
  └── administrators           ├── audio_resources
                               ├── pdf_resources
                               └── hotline_resources
```

Each subtype table shares the supertype primary key and is protected by a
deferred exactly-one-subtype trigger. See
[`DATA_DICTIONARY.md`](DATA_DICTIONARY.md) and [`ERD.mmd`](ERD.mmd).

---

## 3. Polymorphism

> The same call produces role- or subtype-specific behaviour.

| Call | Runtime choices | Why it matters |
|---|---|---|
| `user.homeTabs()` | Student: Home/Appointments/Resources/Enquiries/Profile; Counsellor: Home/Schedule/Records/Enquiries/Profile; Admin: Home/Counsellors/Students/Resources/Profile | [`RoleNavigation.kt`](../app/src/main/java/com/example/counselling/navigation/RoleNavigation.kt) calls one method; there is no `when (role)`. |
| `user.homeGreeting()` | Each User subtype supplies its own Home subtitle | [`HomeScreen.kt`](../app/src/main/java/com/example/counselling/feature/home/ui/HomeScreen.kt) does not branch on a role string. |
| `user.permissions` | Different immutable capability sets | `canManageResources()`, `canViewPrivateSessionNotes()`, etc. |
| `appointment.status.allowedNextStatuses()` | Pending/Confirmed/Completed/NoShow/Cancelled expose different transitions and reschedule/cancel capabilities | [`AppointmentStatus.kt`](../app/src/main/java/com/example/counselling/core/domain/AppointmentStatus.kt); the typed action callbacks are supplied by the screen, gated by `UserPermissions` |
| `resource.type` / `contentUrl` / `mediaType` | Five media subclasses | UI treats a `Resource` uniformly but a `HotlineResource` is detectable by `isHotline`. |
| `Enquiry.isAnonymous` / `studentId` | Identified vs anonymous subclass | Domain prevents an anonymous row from exposing an owner. |
| `AppointmentStatus` sealed `when` | Exhaustive compiler check | No `else` fallback that silently accepts an unknown status. |

**Important design choice:** no ViewModel compares `"student"`, `"counsellor"`
or `"admin"` strings. The only role display still present is the profile badge
`roleLabel`, which is presentation copy derived from the already-created
subclass, not a permission decision.

---

## 4. Abstraction

> Callers depend on what a repository can do, not how Supabase does it.

| Interface | Implementation | Consumers |
|---|---|---|
| `AuthRepository` | `SupabaseAuthRepository` | `SessionManager`, Login/Register/Profile ViewModels |
| `UserRepository` | `SupabaseUserRepository` | `SessionManager` |
| `StudentRepository` | `SupabaseStudentRepository` | Profile, StudentList, StudentDetail ViewModels |
| `AppointmentRepository` | `SupabaseAppointmentRepository` | Booking, reschedule, appointment tabs, schedule, admin appointments, Home, records/feedback |
| `CounsellorRepository` | `SupabaseCounsellorRepository` | Counsellor list/detail/form/availability |
| `SessionRecordRepository` | `SupabaseSessionRecordRepository` | Records list/form, follow-up, summaries, Admin Home |
| `ResourceRepository` | `SupabaseResourceRepository` | Resources, ResourceForm, SelfCheck |
| `EnquiryRepository` | `SupabaseEnquiryRepository` | Student enquiries, counsellor inbox |
| `FeedbackRepository` | `SupabaseFeedbackRepository` | Student feedback, counsellor feedback |

All repository methods return `Result<T>` and use error messages translated by
[`ErrorTranslator`](../app/src/main/java/com/example/counselling/core/data/ErrorTranslator.kt).
The UI never sees a PostgREST exception string.

See the persistence diagram in
[`CLASS_DIAGRAM.md`](CLASS_DIAGRAM.md#6-persistence-architecture).

---

## 5. Association, aggregation and composition

> Objects are connected to other objects instead of copying flat row fields.

| Relationship | Example | File |
|---|---|---|
| Composition | `Appointment` HAS-A `Student`, `Counsellor`, `TimeSlot`, `ConcernCategory`, `SessionMode`, `AppointmentStatus` | [`Appointment.kt`](../app/src/main/java/com/example/counselling/feature/appointment/domain/Appointment.kt) |
| Composition | `SessionRecord` HAS-A `Appointment`, `RiskLevel`, optional `FollowUp` | [`SessionRecord.kt`](../app/src/main/java/com/example/counselling/feature/records/domain/SessionRecord.kt) |
| Aggregation | `Counsellor` HAS-MANY `AvailabilitySlot` | [`User.kt`](../app/src/main/java/com/example/counselling/core/domain/User.kt), [`AvailabilitySlot.kt`](../app/src/main/java/com/example/counselling/core/domain/AvailabilitySlot.kt) |
| Value composition | `AvailabilitySlot` HAS-A `TimeSlot` | [`AvailabilitySlot.kt`](../app/src/main/java/com/example/counselling/core/domain/AvailabilitySlot.kt) |
| Association | `CounsellorMatch` HAS-MANY `AvailableSlot` | [`CounsellorMatch.kt`](../app/src/main/java/com/example/counselling/feature/appointment/domain/CounsellorMatch.kt) |
| Association | `Feedback` references `appointmentId` and safe student/counsellor names | [`Feedback.kt`](../app/src/main/java/com/example/counselling/feature/enquiry/domain/Feedback.kt) |

These are object relationships, not "extra columns copied into every ViewModel".
For example, `SessionRecordsScreen` reads `record.studentName` and
`record.sessionStart` through the composed `Appointment`, not from a duplicated
record row.

---

## 6. Domain model

> The business rules are independent of Android and Supabase.

### Pure packages

```text
core/domain/                 User, Student, Counsellor, Administrator,
                             ValueObjects, Vocabulary, AppointmentStatus,
                             AvailabilitySlot, UserRepository, UserFactory
feature/appointment/domain/  Appointment, AvailableSlot, CounsellorMatch,
                             CounsellorMatchStrategy, AppointmentRepository
feature/student/domain/      StudentRepository
feature/auth/domain/         AuthRepository, RegistrationDetails
feature/counsellor/domain/   CounsellorRepository, WeeklyAvailabilityPattern
feature/records/domain/      SessionRecord, FollowUp, TriageBoard,
                             SessionRecordRepository, AdminDashboardStats
feature/resource/domain/     Resource hierarchy, ResourceType, ResourceDraft,
                             SelfCheckContent, ResourceRepository
feature/enquiry/domain/      Enquiry hierarchy, EnquiryStatus, EnquiryLookup,
                             Feedback, repositories
```

Verification command:

```powershell
Get-ChildItem -Recurse -File app\src\main\java\com\example\counselling\core\domain,app\src\main\java\com\example\counselling\feature\*\domain |
  Select-String -Pattern '^import (android|androidx|io\.github\.jan\.supabase)'
```

Expected: no output.

### Mapping boundary

```text
PostgREST row shape              Domain object
UserViewRow            →  UserFactory → Student / Counsellor / Administrator
AppointmentViewRow     →  AppointmentViewRow.toDomain() → Appointment
AvailableSlotViewRow   →  AvailableSlotViewRow.toDomain() → AvailableSlot
ResourceViewRow        →  SupabaseResourceRepository.toDomain() → Resource subtype
EnquiryViewRow         →  SupabaseEnquiryRepository.toDomain() → Identified/Anonymous
FeedbackViewRow        →  SupabaseFeedbackRepository.toDomain() → Feedback
SessionRecordTableRow  →  SupabaseSessionRecordRepository mapping → SessionRecord
```

DTOs are `private` inside their data-layer file so a ViewModel cannot import
them by accident. This is the main structural fix for the lecturer's complaint
that the database shapes leaked into the whole codebase.

---

## 7. Design patterns used

| Pattern | Where | Why it is the right pattern |
|---|---|---|
| **Repository** | every `*Repository` interface + `Supabase*Repository` | Screens/ViewModels never know PostgREST. |
| **Template Method** | [`BaseSupabaseRepository`](../app/src/main/java/com/example/counselling/core/data/BaseSupabaseRepository.kt) `runQuery` | One error/Result/translator skeleton; subclasses supply the query. |
| **Factory** | [`UserFactory`](../app/src/main/java/com/example/counselling/core/domain/UserFactory.kt) | One place turns a generic `UserRecord` into the correct subclass. |
| **Strategy** | [`CounsellorMatchStrategy`](../app/src/main/java/com/example/counselling/feature/appointment/domain/CounsellorMatch.kt) and `SpecialisationThenEarliestSlotStrategy` | Smart Match ranking can be replaced/tested without ViewModel changes. |
| **State** | [`AppointmentStatus`](../app/src/main/java/com/example/counselling/core/domain/AppointmentStatus.kt) | Transitions and permitted actions belong to the state, not to `if (status == "...")`. |
| **Value Object** | `TimeSlot`, `Email`, `Rating`, `ResourceDraft`, `FollowUp` | Equality and validation are meaningful; no invalid instances. |
| **Adapter (removed)** | previous `Profile` bridge and legacy Appointment repository | These existed only during Phases 4–5 and were deleted in Phase 6. |
| **Composition root / manual DI** | [`AppContainer`](../app/src/main/java/com/example/counselling/core/AppContainer.kt) | One place constructs concrete Supabase repositories; ViewModel factories receive interfaces. |

The old `ErrorMapper` object is now a thin facade for `SessionManager`; all
feature repositories inject `ErrorTranslator` directly.

---

## 8. UI architecture

- Every converted ViewModel extends `BaseViewModel`, which owns the shared
  `Channel<UiMessage>` used by Snackbars.
- ViewModels expose `StateFlow<UiState>`; screens collect it with
  `collectAsStateWithLifecycle`.
- Screens receive interfaces via explicit `viewModelFactory` companions.
- No Hilt/Koin; the composition root is `AppContainer`.
- Appointment actions are typed `AppointmentStatus` values and permission checks,
  not string comparisons.
- Resource management is gated by `canManageResources()`, not by a role enum.
- Navigation is `user.homeTabs()`, so adding a role-specific tab is a User
  subclass change, not an `AppNavGraph` branch.

See [`MODULE_NOTES.md`](MODULE_NOTES.md) for the per-module behaviour summary.

---

## 9. OOP audit checklist

| Requirement from the brief | Evidence | Status |
|---|---|---|
| Encapsulation | Value objects, entity `init` blocks, immutable `val`s, private DTOs | ✅ |
| Inheritance | User/Resource/Enquiry/AppointmentStatus + DB class-table schema | ✅ |
| Polymorphism | `homeTabs()`, `permissions`, `actionsFor`, Resource subtypes, sealed `when` | ✅ |
| Abstraction | 9 repository interfaces + Base classes | ✅ |
| Association/aggregation/composition | Appointment, SessionRecord, Counsellor–AvailabilitySlot, CounsellorMatch | ✅ |
| Domain model | pure `core/domain` + `feature/*/domain`, no Android/Supabase imports | ✅ |
| Database OOP | supertype/subtype tables, reference tables, exactly-one-subtype triggers, views, RPCs | ✅ |
| No role strings in decisions | `grep -R "when (role"` / `UserRole` returns no app code | ✅ |
| Comments | 110+ `OOP:` comments in domain/data/base/ViewModel files | ✅ |
| Tests | 58 JVM tests, 0 failures | ✅ |

### Useful final commands

```powershell
# compile + all tests
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain

# domain purity
Get-ChildItem -Recurse -File app\src\main\java\com\example\counselling\core\domain,app\src\main\java\com\example\counselling\feature\*\domain |
  Select-String -Pattern '^import (android|androidx|io\.github\.jan\.supabase)'

# legacy reference audit
Get-ChildItem -Recurse -File app\src\main\java\com\example\counselling |
  Select-String -Pattern 'UserRole|Profile\b|feature\.appointment\.model|legacyAppointment'
```

All three should produce no legacy output; the tests should report 58/58.

---

## 10. Honest limitations

1. **Manual end-to-end role testing is still blocked** by the malformed demo
   Auth rows in the current Supabase project. The code builds; the database RLS
   queries were proven in Phase 1 and SQL smoke tests.
2. `supabase-kt 3.2.2` is pinned to the project's Kotlin 2.2.10 toolchain.
3. Release signing is not configured; the debug APK is the deliverable.
4. Compose deprecation warnings (e.g. `Icons.Outlined.MenuBook`) are cosmetic and
   do not affect the OOP design.

These limitations are repeated in [`FINAL_CHECKLIST.md`](FINAL_CHECKLIST.md).
