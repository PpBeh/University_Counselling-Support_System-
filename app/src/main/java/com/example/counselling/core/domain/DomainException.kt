// Core domain | Owner: shared (Beh, Jeremy, Michael)
// Pure-Kotlin business error carried by value objects and entities.
package com.example.counselling.core.domain

// OOP: Encapsulation - invalid domain state is reported through one typed
// exception instead of leaking database or UI exceptions into the domain.
class DomainException(message: String) : IllegalArgumentException(message)
