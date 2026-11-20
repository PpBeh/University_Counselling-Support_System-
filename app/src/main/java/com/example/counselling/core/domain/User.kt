// Core domain | Owner: shared (Beh, Jeremy, Michael)
// User generalisation and role-specific behaviour. Pure Kotlin: no Android or
// Supabase imports live in this file.
package com.example.counselling.core.domain

import java.time.Duration
import java.time.Instant

// OOP: Polymorphism - permissions are asked through methods instead of UI code
// comparing a role string. Each concrete User supplies its own capability set.
class UserPermissions private constructor(
    private val allowBookAppointments: Boolean,
    private val allowManageResources: Boolean,
    private val allowManageCounsellors: Boolean,
    private val allowViewStudentDirectory: Boolean,
    private val allowViewPrivateSessionNotes: Boolean,
    private val allowManageAvailability: Boolean,
    private val allowAccessEnquiryInbox: Boolean,
    private val allowViewAllAppointments: Boolean,
    private val allowGiveFeedback: Boolean,
    private val allowViewFeedback: Boolean,
    private val allowDeactivateOwnAccount: Boolean,
) {

    fun canBookAppointments(): Boolean = allowBookAppointments
    fun canManageResources(): Boolean = allowManageResources
    fun canManageCounsellors(): Boolean = allowManageCounsellors
    fun canViewStudentDirectory(): Boolean = allowViewStudentDirectory
    fun canViewPrivateSessionNotes(): Boolean = allowViewPrivateSessionNotes
    fun canManageAvailability(): Boolean = allowManageAvailability
    fun canAccessEnquiryInbox(): Boolean = allowAccessEnquiryInbox
    fun canViewAllAppointments(): Boolean = allowViewAllAppointments
    fun canGiveFeedback(): Boolean = allowGiveFeedback
    fun canViewFeedback(): Boolean = allowViewFeedback
    fun canDeactivateOwnAccount(): Boolean = allowDeactivateOwnAccount

    companion object {
        // OOP: Encapsulation - the three capability sets are immutable values.
        val STUDENT = UserPermissions(
            allowBookAppointments = true,
            allowManageResources = false,
            allowManageCounsellors = false,
            allowViewStudentDirectory = false,
            allowViewPrivateSessionNotes = false,
            allowManageAvailability = false,
            allowAccessEnquiryInbox = false,
            allowViewAllAppointments = false,
            allowGiveFeedback = true,
            allowViewFeedback = true,
            allowDeactivateOwnAccount = true,
        )

        val COUNSELLOR = UserPermissions(
            allowBookAppointments = false,
            allowManageResources = true,
            allowManageCounsellors = false,
            allowViewStudentDirectory = true,
            allowViewPrivateSessionNotes = true,
            allowManageAvailability = true,
            allowAccessEnquiryInbox = true,
            allowViewAllAppointments = false,
            allowGiveFeedback = false,
            allowViewFeedback = true,
            allowDeactivateOwnAccount = false,
        )

        val ADMINISTRATOR = UserPermissions(
            allowBookAppointments = false,
            allowManageResources = true,
            allowManageCounsellors = true,
            allowViewStudentDirectory = true,
            allowViewPrivateSessionNotes = false,
            allowManageAvailability = true,
            allowAccessEnquiryInbox = false,
            allowViewAllAppointments = true,
            allowGiveFeedback = false,
            allowViewFeedback = false,
            allowDeactivateOwnAccount = false,
        )
    }
}

// OOP: Enumeration of navigation destinations without Android types. The
// navigation layer maps these logical tabs to Material icons and route strings.
enum class HomeTab(val label: String) {
    HOME("Home"),
    APPOINTMENTS("Appointments"),
    SCHEDULE("Schedule"),
    RECORDS("Records"),
    COUNSELLORS("Counsellors"),
    STUDENTS("Students"),
    RESOURCES("Resources"),
    ENQUIRIES("Enquiries"),
    PROFILE("Profile"),
}

// OOP: Inheritance - User is the abstract superclass. Student, Counsellor and
// Administrator extend it and override homeTabs(), permissions and homeGreeting().
sealed class User(
    val id: String,
    val email: Email,
    fullName: String,
    val phone: PhoneNumber?,
    val isActive: Boolean,
    val authUserId: String? = null,
) {

    val fullName: String = fullName.trim()

    init {
        if (id.isBlank()) {
            throw DomainException("User id cannot be blank.")
        }
        if (this.fullName.length < 2 || this.fullName.length > 100) {
            throw DomainException("Full name must be between 2 and 100 characters.")
        }
    }

    val firstName: String
        get() = fullName.substringBefore(' ')

    val initials: String
        get() {
            val parts = fullName.split(" ").filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts.first().take(1).uppercase()
                else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
            }
        }

    abstract val permissions: UserPermissions

    abstract fun homeTabs(): List<HomeTab>

    // OOP: Polymorphism - the role-specific line under the name in Home.
    abstract fun homeGreeting(): String

    fun displayName(): String = fullName

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is User && id == other.id && javaClass == other.javaClass)

    override fun hashCode(): Int = 31 * id.hashCode() + javaClass.hashCode()

    override fun toString(): String = "${javaClass.simpleName}($id)"
}

// OOP: Inheritance - Student extends User with academic identity.
class Student(
    id: String,
    email: Email,
    fullName: String,
    phone: PhoneNumber?,
    isActive: Boolean,
    val studentId: StudentId,
    programme: String,
    val preferredMode: SessionMode,
    authUserId: String? = null,
) : User(id, email, fullName, phone, isActive, authUserId) {

    val programme: String = programme.trim()

    init {
        if (this.programme.length < 2 || this.programme.length > 120) {
            throw DomainException("Programme must be between 2 and 120 characters.")
        }
    }

    override val permissions: UserPermissions
        get() = UserPermissions.STUDENT

    override fun homeTabs(): List<HomeTab> = listOf(
        HomeTab.HOME,
        HomeTab.APPOINTMENTS,
        HomeTab.RESOURCES,
        HomeTab.ENQUIRIES,
        HomeTab.PROFILE,
    )

    override fun homeGreeting(): String =
        "You do not have to manage everything alone. What feels useful today?"

    // OOP: Encapsulation - the 0-upcoming rule lives on Student, not the UI.
    fun canDeactivate(activeAppointmentCount: Int): Boolean {
        if (activeAppointmentCount < 0) {
            throw DomainException("Active appointment count cannot be negative.")
        }
        return activeAppointmentCount == 0
    }
}

// OOP: Inheritance - Counsellor extends User and owns directory attributes.
class Counsellor(
    id: String,
    email: Email,
    fullName: String,
    phone: PhoneNumber?,
    isActive: Boolean,
    specialisations: List<ConcernCategory>,
    languages: List<String>,
    bio: String,
    val availability: List<AvailabilitySlot> = emptyList(),
    authUserId: String? = null,
) : User(id, email, fullName, phone, isActive, authUserId) {

    val specialisations: List<ConcernCategory> = specialisations.distinct()

    val languages: List<String> = languages
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    val bio: String = bio.trim()

    init {
        if (this.specialisations.isEmpty()) {
            throw DomainException("A counsellor needs at least one specialisation.")
        }
        if (this.languages.isEmpty()) {
            throw DomainException("A counsellor needs at least one language.")
        }
        if (this.bio.length > 2000) {
            throw DomainException("Counsellor bio must be 2,000 characters or fewer.")
        }
    }

    override val permissions: UserPermissions
        get() = UserPermissions.COUNSELLOR

    override fun homeTabs(): List<HomeTab> = listOf(
        HomeTab.HOME,
        HomeTab.SCHEDULE,
        HomeTab.RECORDS,
        HomeTab.ENQUIRIES,
        HomeTab.PROFILE,
    )

    override fun homeGreeting(): String =
        "Your space to support students with care and privacy."

    fun supports(category: ConcernCategory): Boolean =
        category in specialisations

    fun speaks(language: String): Boolean =
        languages.any { it.equals(language.trim(), ignoreCase = true) }

    val specialisationLabels: List<String>
        get() = specialisations.map { it.label }

    val slots: List<AvailabilitySlot>
        get() = availability

    fun availableThisWeek(now: Instant = Instant.now()): Boolean =
        availability.any {
            it.isEditable(now) && it.timeSlot.start.isBefore(now.plus(Duration.ofDays(7)))
        }

    fun upcomingFreeSlots(now: Instant = Instant.now()): List<AvailabilitySlot> =
        availability.filter { it.isEditable(now) }.sortedBy { it.timeSlot.start }

    fun withAvailability(slots: List<AvailabilitySlot>): Counsellor = Counsellor(
        id = id,
        email = email,
        fullName = fullName,
        phone = phone,
        isActive = isActive,
        specialisations = specialisations,
        languages = languages,
        bio = bio,
        availability = slots,
        authUserId = authUserId,
    )
}

// OOP: Inheritance - Administrator extends User; the subtype is the role.
class Administrator(
    id: String,
    email: Email,
    fullName: String,
    phone: PhoneNumber?,
    isActive: Boolean,
    authUserId: String? = null,
) : User(id, email, fullName, phone, isActive, authUserId) {

    override val permissions: UserPermissions
        get() = UserPermissions.ADMINISTRATOR

    override fun homeTabs(): List<HomeTab> = listOf(
        HomeTab.HOME,
        HomeTab.COUNSELLORS,
        HomeTab.STUDENTS,
        HomeTab.RESOURCES,
        HomeTab.PROFILE,
    )

    override fun homeGreeting(): String =
        "A calm overview of the counselling service."
}
