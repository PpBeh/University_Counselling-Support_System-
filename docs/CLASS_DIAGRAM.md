# OOP Class Diagram

**Project:** University Counselling Appointment & Support System (AMCS2194)  
**Scope:** final Android domain + persistence architecture after Phases 2–6.

The Android domain packages are pure Kotlin. Android/Compose lives in `ui`, and
Supabase DTOs/mappers live in `data`. This is intentional: the class diagram
below is the business model, not a diagram of Composable functions.

---

## 1. User hierarchy and counsellor availability

```mermaid
classDiagram
    direction TB

    class User {
        <<sealed>>
        +String id
        +Email email
        +String fullName
        +PhoneNumber phone
        +Boolean isActive
        +String authUserId
        +UserPermissions permissions
        +homeTabs() List~HomeTab~
        +homeGreeting() String
        +initials() String
    }

    class Student {
        +StudentId studentId
        +String programme
        +SessionMode preferredMode
    }

    class Counsellor {
        +List~ConcernCategory~ specialisations
        +List~String~ languages
        +String bio
        +List~AvailabilitySlot~ availability
        +availableThisWeek(now) Boolean
        +upcomingFreeSlots(now) List~AvailabilitySlot~
        +withAvailability(slots) Counsellor
    }

    class Administrator {
        +permissions = ADMINISTRATOR
    }

    class UserPermissions {
        <<value object>>
        +canBookAppointments()
        +canManageResources()
        +canManageCounsellors()
        +canViewStudentDirectory()
        +canViewPrivateSessionNotes()
        +canManageAvailability()
        +canAccessEnquiryInbox()
        +canViewAllAppointments()
        +canGiveFeedback()
        +canViewFeedback()
        +canDeactivateOwnAccount()
    }

    class HomeTab {
        <<enumeration>>
        HOME
        APPOINTMENTS
        SCHEDULE
        RECORDS
        COUNSELLORS
        STUDENTS
        RESOURCES
        ENQUIRIES
        PROFILE
    }

    class AvailabilitySlot {
        +String id
        +String counsellorId
        +TimeSlot timeSlot
        +Boolean isBooked
        +isPast(now) Boolean
        +isEditable(now) Boolean
        +overlaps(other) Boolean
    }

    class TimeSlot {
        <<value object>>
        +Instant start
        +Instant end
        +durationMinutes() Long
        +overlaps(other) Boolean
        +isFuture(now) Boolean
    }

    User <|-- Student
    User <|-- Counsellor
    User <|-- Administrator
    User *-- UserPermissions
    User ..> HomeTab : returns
    Counsellor "1" o-- "*" AvailabilitySlot : has many
    AvailabilitySlot *-- TimeSlot
    WeeklyAvailabilityPattern ..> TimeSlot : generates
```

`Counsellor` is the polymorphic object that supplies directory data,
availability and dashboard tabs. UI code never checks a `role` string; it calls
`user.homeTabs()` and `user.permissions`.

---

## 2. Appointment and State pattern

```mermaid
classDiagram
    direction TB

    class Appointment {
        +String id
        +Student student
        +Counsellor counsellor
        +String slotId
        +TimeSlot slot
        +SessionMode mode
        +ConcernCategory concern
        +String note
        +AppointmentStatus status
        +String followUpOf
        +String studentName
        +String counsellorName
        +String slotStart
        +canBeRescheduledBy(actor, now) Boolean
        +canBeCancelledBy(actor, now) Boolean
        +reschedule(newSlotId, newSlot, now, actor) Appointment
        +cancel(now, actor) Appointment
        +withStatus(next, now, actor) Appointment
    }

    class AppointmentStatus {
        <<sealed>>
        +wireValue String
        +label String
        +isTerminal Boolean
        +allowedNextStatuses() Set~AppointmentStatus~
        +canTransitionTo(next) Boolean
        +canReschedule() Boolean
        +canCancel() Boolean
    }

    class Pending
    class Confirmed
    class Completed
    class NoShow
    class Cancelled

    Appointment *-- Student
    Appointment *-- Counsellor
    Appointment *-- TimeSlot
    Appointment o-- AppointmentStatus
    AppointmentStatus <|-- Pending
    AppointmentStatus <|-- Confirmed
    AppointmentStatus <|-- Completed
    AppointmentStatus <|-- NoShow
    AppointmentStatus <|-- Cancelled
```

The status transition table is owned by the State classes, not by UI string
comparisons. `Appointment` also owns the 12-hour student/counsellor reschedule
rules and cancellation-before-start rule.

---

## 3. Resources, self-check and recommendations

```mermaid
classDiagram
    direction TB

    class Resource {
        <<sealed>>
        +String id
        +String title
        +String description
        +ConcernCategory category
        +Set~StressBracket~ targetBrackets
        +Boolean isPublished
        +ResourceType type
        +String contentUrl
        +String mediaType
        +Boolean isHotline
        +targets(bracket) Boolean
    }

    class ArticleResource
    class VideoResource
    class AudioResource
    class PdfResource
    class HotlineResource {
        +String phoneNumber
        +String operatingHours
    }

    class ResourceDraft {
        <<value object>>
        +validate() ResourceDraft
    }

    class ResourceType {
        <<enumeration>>
        ARTICLE
        VIDEO
        AUDIO
        PDF
        HOTLINE
    }

    class SelfCheckResult {
        <<value object>>
        +Int score
        +SelfCheckBracket bracket
        +String dominantConcern
    }

    Resource <|-- ArticleResource
    Resource <|-- VideoResource
    Resource <|-- AudioResource
    Resource <|-- PdfResource
    Resource <|-- HotlineResource
    Resource *-- ResourceType
    ResourceDraft ..> ResourceType
    SelfCheckResult ..> Resource : recommendResources filters
```

This mirrors the database's `resources` class-table inheritance with five
subtype tables. `v_resources` is mapped back to the correct Kotlin subtype.

---

## 4. Session records, Follow-up and Triage

```mermaid
classDiagram
    direction TB

    class SessionRecord {
        +String id
        +Appointment appointment
        +String privateNotes
        +String publicSummary
        +RiskLevel risk
        +FollowUp followUp
        +Boolean isArchived
        +Instant createdAt
        +Instant updatedAt
        +canBeEditedAt(now) Boolean
        +archive() SessionRecord
        +restore() SessionRecord
        +withContent(...) SessionRecord
    }

    class FollowUp {
        <<value object>>
        +Instant dueAt
        +isOverdue(now) Boolean
        +isDueSoon(now) Boolean
    }

    class TriageBoard {
        +bucketOf(record) TriageBucket
        +group(records) Map
    }

    class TriageBucket {
        <<enumeration>>
        OVERDUE
        DUE_SOON
        LATER
    }

    class RiskLevel {
        <<enumeration>>
        LOW
        MEDIUM
        HIGH
    }

    class StudentSessionSummary {
        <<value object>>
        +recordId
        +sessionStart
        +concernCategory
        +counsellorName
        +publicSummary
    }

    class AdminDashboardStats {
        <<value object>>
        +totalStudents
        +activeStudents
        +totalCounsellors
        +activeCounsellors
        +totalAppointments
        +upcomingAppointments
        +completedAppointments
        +sessionRecords
        +totalResources
        +publishedResources
        +feedbackCount
        +averageRating
    }

    SessionRecord *-- Appointment
    SessionRecord o-- RiskLevel
    SessionRecord o-- FollowUp
    TriageBoard ..> SessionRecord
    TriageBoard ..> TriageBucket
```

`StudentSessionSummary` is a separate safe projection; students never receive
the private `SessionRecord` or its `Appointment` internals.

---

## 5. Enquiry hierarchy and feedback

```mermaid
classDiagram
    direction TB

    class Enquiry {
        <<sealed>>
        +String id
        +String referenceCode
        +ConcernCategory category
        +String message
        +EnquiryStatus status
        +String reply
        +String recommendedResourceId
        +String recommendedResourceTitle
        +Instant repliedAt
        +Instant createdAt
        +canReply() Boolean
        +isAnonymous Boolean
        +studentId String
        +studentName String
    }

    class IdentifiedEnquiry {
        +String ownerId
        +String ownerName
    }

    class AnonymousEnquiry

    class EnquiryStatus {
        <<enumeration>>
        OPEN
        ANSWERED
        CLOSED
    }

    class EnquiryLookup {
        <<value object>>
        +referenceCode
        +category
        +message
        +reply
        +status
        +recommendedResourceTitle
    }

    class Feedback {
        +String id
        +String appointmentId
        +String studentName
        +String counsellorName
        +Rating rating
        +String comment
        +Instant sessionStart
        +String concernCategory
    }

    Enquiry <|-- IdentifiedEnquiry
    Enquiry <|-- AnonymousEnquiry
    Enquiry o-- EnquiryStatus
    EnquiryLookup ..> EnquiryStatus
    Feedback *-- Rating
```

Anonymous rows carry no student id/name in the domain, even if a future backend
projection accidentally includes an empty column.

---

## 6. Persistence architecture

```mermaid
classDiagram
    direction LR

    class BaseSupabaseRepository {
        <<abstract>>
        #SupabaseClient supabase
        #ErrorTranslator errorTranslator
        #runQuery(fallback, block) Result~T~
        #failure(fallback, error) Result~Nothing~
    }

    class ErrorTranslator {
        +translate(error, fallback) String
    }

    class UserRepository {
        <<interface>>
        +getCurrentUser(authUserId) Result~User~
    }

    class StudentRepository {
        <<interface>>
    }
    class AppointmentRepository {
        <<interface>>
    }
    class CounsellorRepository {
        <<interface>>
    }
    class SessionRecordRepository {
        <<interface>>
    }
    class ResourceRepository {
        <<interface>>
    }
    class EnquiryRepository {
        <<interface>>
    }
    class FeedbackRepository {
        <<interface>>
    }
    class AuthRepository {
        <<interface>>
    }

    class SupabaseUserRepository
    class SupabaseStudentRepository
    class SupabaseAppointmentRepository
    class SupabaseCounsellorRepository
    class SupabaseSessionRecordRepository
    class SupabaseResourceRepository
    class SupabaseEnquiryRepository
    class SupabaseFeedbackRepository
    class SupabaseAuthRepository

    class UserViewRow {
        <<DTO>>
    }
    class AppointmentViewRow {
        <<DTO>>
    }
    class ResourceViewRow {
        <<DTO>>
    }
    class EnquiryViewRow {
        <<DTO>>
    }
    class FeedbackViewRow {
        <<DTO>>
    }

    BaseSupabaseRepository <|-- SupabaseUserRepository
    BaseSupabaseRepository <|-- SupabaseStudentRepository
    BaseSupabaseRepository <|-- SupabaseAppointmentRepository
    BaseSupabaseRepository <|-- SupabaseCounsellorRepository
    BaseSupabaseRepository <|-- SupabaseSessionRecordRepository
    BaseSupabaseRepository <|-- SupabaseResourceRepository
    BaseSupabaseRepository <|-- SupabaseEnquiryRepository
    BaseSupabaseRepository <|-- SupabaseFeedbackRepository
    BaseSupabaseRepository <|-- SupabaseAuthRepository

    UserRepository <|.. SupabaseUserRepository
    StudentRepository <|.. SupabaseStudentRepository
    AppointmentRepository <|.. SupabaseAppointmentRepository
    CounsellorRepository <|.. SupabaseCounsellorRepository
    SessionRecordRepository <|.. SupabaseSessionRecordRepository
    ResourceRepository <|.. SupabaseResourceRepository
    EnquiryRepository <|.. SupabaseEnquiryRepository
    FeedbackRepository <|.. SupabaseFeedbackRepository
    AuthRepository <|.. SupabaseAuthRepository

    SupabaseUserRepository ..> UserViewRow : decodes
    SupabaseAppointmentRepository ..> AppointmentViewRow : decodes
    SupabaseResourceRepository ..> ResourceViewRow : decodes
    SupabaseEnquiryRepository ..> EnquiryViewRow : decodes
    SupabaseFeedbackRepository ..> FeedbackViewRow : decodes

    BaseSupabaseRepository *-- ErrorTranslator
```

The data class names above are illustrative of each repository's private DTO;
they are deliberately private so ViewModels cannot accidentally depend on
PostgREST row shapes.

---

## 7. UI dependency direction

```text
Composable Screen
      │ events / state
      ▼
ViewModel ──────────────► Repository INTERFACE
      │                         │
      │ StateFlow<UiState>      │ Result<Domain>
      ▼                         ▼
Compose UI              SupabaseRepository IMPLEMENTATION
                                │
                                │ DTO decode + mapper
                                ▼
                        PostgREST view / RPC
```

- Screens do not import `data` or `io.github.jan.supabase`.
- ViewModels import domain interfaces only (plus `SessionManager` for auth).
- `AppContainer` is the only composition root and is manual DI.
- Every repository returns `Result<T>` with `ErrorTranslator` messages.

---

## 8. Ownership map

| Package | Owner | Main classes |
|---|---|---|
| `core/domain` | shared | `User`, `Student`, `Counsellor`, `Administrator`, `ValueObjects`, `AppointmentStatus`, `AvailabilitySlot` |
| `core/data` | shared | `BaseSupabaseRepository`, `SupabaseUserRepository`, `ErrorTranslator`, `UserViewRow` |
| `core/ui` | shared | `BaseViewModel` |
| `feature/auth` | Beh | `AuthRepository`, `RegistrationDetails` |
| `feature/student` | Beh | `StudentRepository`, `SupabaseStudentRepository` |
| `feature/appointment` | Beh | `Appointment`, `AppointmentRepository`, `CounsellorMatchStrategy` |
| `feature/counsellor` | Jeremy | `CounsellorRepository`, `WeeklyAvailabilityPattern` |
| `feature/records` | Jeremy | `SessionRecord`, `FollowUp`, `TriageBoard`, `SessionRecordRepository` |
| `feature/resource` | Michael | `Resource` hierarchy, `ResourceDraft`, `SelfCheckContent`, `ResourceRepository` |
| `feature/enquiry` | Michael | `Enquiry` hierarchy, `Feedback`, `EnquiryRepository`, `FeedbackRepository` |

The class diagram is intentionally kept in one file so the assessor can compare
it with the ERD in [`ERD.mmd`](ERD.mmd).
