// Test support | Owner: shared (Beh, Jeremy, Michael)
// Demo accounts seeded in the Supabase project. Instrumented tests sign in as
// a real user because the app has no fake-session injection point.
package com.example.counselling.support

object TestAccounts {
    const val PASSWORD = "Demo@12345"

    const val STUDENT_EMAIL = "michaelgoh0831@gmail.com"
    const val STUDENT_NAME = "Michael Goh"

    const val COUNSELLOR_EMAIL = "counsellor.daniel@university.edu.my"
    const val COUNSELLOR_NAME = "Mr. Daniel Lim"

    const val ADMIN_EMAIL = "admin@university.edu.my"
    const val ADMIN_NAME = "System Administrator"
}
