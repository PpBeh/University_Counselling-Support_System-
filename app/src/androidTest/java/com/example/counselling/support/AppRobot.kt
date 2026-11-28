// Test support | Owner: shared (Beh, Jeremy, Michael)
// Small robot over the Compose test API: waits, sign-in/out, tab clicks and
// text matching. Keeping this here means each test reads as user steps.
package com.example.counselling.support

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp

/** Network calls against Supabase can take a few seconds on a cold emulator. */
const val NETWORK_TIMEOUT = 30_000L

/** Local UI transitions and sheets. */
const val UI_TIMEOUT = 8_000L

fun ComposeTestRule.awaitText(
    text: String,
    substring: Boolean = false,
    timeout: Long = NETWORK_TIMEOUT,
) {
    waitUntil(timeoutMillis = timeout) {
        onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeTestRule.awaitAnyText(
    vararg texts: String,
    timeout: Long = NETWORK_TIMEOUT,
): String {
    var found = ""
    waitUntil(timeoutMillis = timeout) {
        found = texts.firstOrNull { textExists(it) }.orEmpty()
        found.isNotEmpty()
    }
    return found
}

fun ComposeTestRule.textExists(text: String, substring: Boolean = false): Boolean =
    onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()

fun ComposeTestRule.awaitGone(text: String, timeout: Long = NETWORK_TIMEOUT) {
    waitUntil(timeoutMillis = timeout) { !textExists(text) }
}

fun ComposeTestRule.awaitContentDescription(
    description: String,
    timeout: Long = NETWORK_TIMEOUT,
) {
    waitUntil(timeoutMillis = timeout) {
        onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeTestRule.assertTextVisible(text: String, substring: Boolean = false) {
    val nodes = onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes()
    if (nodes.isEmpty()) {
        throw AssertionError("Expected text \"$text\" on screen but it was not found.")
    }
}

fun ComposeTestRule.signedIn(): Boolean =
    onAllNodes(hasText("Home") and isSelectable()).fetchSemanticsNodes().isNotEmpty()

fun ComposeTestRule.signedOut(): Boolean =
    onAllNodes(hasText("Sign in") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()

/**
 * Advances the Compose clock past entrance/transition animations so that hit
 * targets stop moving before a click. Closes the main flake source on a cold
 * emulator, where a click during a slide-in often lands off-target.
 */
fun ComposeTestRule.settleAnimations(millis: Long = 500) {
    mainClock.advanceTimeBy(millis)
}

/** Polls [condition], advancing the Compose clock, and reports success. */
fun ComposeTestRule.waitFor(millis: Long, condition: () -> Boolean): Boolean {
    val deadline = System.currentTimeMillis() + millis
    while (System.currentTimeMillis() < deadline) {
        if (condition()) return true
        settleAnimations(250)
    }
    return condition()
}

/** Last account signed in by this instrumentation process, to avoid repeat logins. */
private var lastSignedInEmail: String? = null

/**
 * Guarantees the login screen is showing. If a previous test left a session
 * behind (Supabase persists it), this signs out through the Profile tab.
 * Clicks are retried because a single tap can land during a transition.
 */
fun ComposeTestRule.ensureSignedOut() {
    lastSignedInEmail = null
    waitUntil(NETWORK_TIMEOUT) { signedOut() || signedIn() }
    if (signedOut()) return

    clickTab("Profile")
    awaitText("Sign out")
    repeat(4) {
        tap("Sign out")
        if (waitFor(UI_TIMEOUT) { signedOut() }) return
    }
    waitUntil(NETWORK_TIMEOUT) { signedOut() }
}

fun ComposeTestRule.signIn(email: String, password: String) {
    if (lastSignedInEmail == email && signedIn()) {
        settleAnimations()
        return
    }
    ensureSignedOut()

    repeat(2) { attempt ->
        settleAnimations()
        val fields = onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement(email)
        fields[1].performTextReplacement(password)
        tap("Sign in")
        if (waitFor(20_000) { signedIn() }) {
            lastSignedInEmail = email
            settleAnimations()
            return
        }
        if (attempt == 0 && textExists("Email or password is incorrect.")) {
            throw AssertionError("Supabase rejected the demo credentials for $email.")
        }
    }
    throw AssertionError("Sign in as $email never completed.")
}

/** Bottom navigation items are the only selectable nodes carrying the label. */
fun ComposeTestRule.clickTab(label: String, timeout: Long = UI_TIMEOUT) {
    val selected = hasText(label) and isSelectable() and isSelected()
    repeat(3) {
        settleAnimations()
        val tab = onAllNodes(hasText(label) and isSelectable())
        if (tab.fetchSemanticsNodes().isEmpty()) {
            throw AssertionError("No bottom tab labelled \"$label\".")
        }
        tab[0].performClick()
        settleAnimations()
        if (waitFor(timeout / 3) { onAllNodes(selected).fetchSemanticsNodes().isNotEmpty() }) {
            return
        }
    }
    throw AssertionError("Bottom tab \"$label\" never became selected.")
}

/**
 * Swipes the outer vertical container until [matcher] has real bounds. Chips
 * inside a nested horizontal scroller are composed but unplaced while below
 * the fold, so `performScrollTo` alone cannot reach them.
 */
private fun ComposeTestRule.scrollIntoView(matcher: SemanticsMatcher, maxSwipes: Int = 8) {
    repeat(maxSwipes) {
        val nodes = onAllNodes(matcher).fetchSemanticsNodes()
        if (nodes.isEmpty()) return
        val placed = nodes.firstOrNull {
            it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f
        }
        if (placed != null) {
            if (placed.boundsInRoot.top < 0f) {
                onRoot().performTouchInput { swipeDown(startY = 0.3f, endY = 0.8f) }
                settleAnimations(400)
            }
            return
        }
        // Start below the top bar and finish above the bottom nav so the
        // scrollable content (not the nav bar) receives the gesture.
        onRoot().performTouchInput { swipeUp(startY = 0.7f, endY = 0.2f) }
        settleAnimations(400)
    }
}

/**
 * Clicks the first node carrying [label] that is actually clickable, scrolling
 * it into view first when it lives inside a scrollable container.
 */
fun ComposeTestRule.tap(label: String, substring: Boolean = false, timeout: Long = UI_TIMEOUT) {
    settleAnimations()
    val matcher = hasText(label, substring = substring)
    // Matching content can arrive a beat after navigation (async data load).
    waitFor(timeout) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    scrollIntoView(matcher)
    val clickable = onAllNodes(hasText(label, substring = substring) and hasClickAction())
    if (clickable.fetchSemanticsNodes().isNotEmpty()) {
        clickable[0].scrollIfPossible().performClick()
        settleAnimations(250)
        return
    }
    val selectable = onAllNodes(hasText(label, substring = substring) and isSelectable())
    if (selectable.fetchSemanticsNodes().isNotEmpty()) {
        selectable[0].scrollIfPossible().performClick()
        settleAnimations(250)
        return
    }
    onAllNodes(hasText(label, substring = substring))[0].scrollIfPossible().performClick()
    settleAnimations(250)
}

/** Taps [label] and retries until [expected] appears. */
fun ComposeTestRule.tapAndAwait(
    label: String,
    expected: String,
    substring: Boolean = false,
    timeout: Long = NETWORK_TIMEOUT,
) {
    repeat(3) {
        tap(label)
        if (waitFor(timeout / 3) { textExists(expected, substring) }) return
    }
    throw AssertionError("Tapping \"$label\" never showed \"$expected\".")
}

fun ComposeTestRule.tapContentDescription(description: String) {
    settleAnimations()
    onAllNodes(hasContentDescription(description))[0].performClick()
    settleAnimations(250)
}

/** Scrolls the containing page so [label] is laid out on screen. */
fun ComposeTestRule.scrollToLabel(label: String) {
    settleAnimations()
    val nodes = onAllNodes(hasText(label))
    if (nodes.fetchSemanticsNodes().isEmpty()) return
    try {
        nodes[0].performScrollTo()
    } catch (_: Throwable) {
        // Node may already be fully visible or inside a nested scroller.
    }
    settleAnimations(400)
}

/**
 * Invokes the node's click semantics directly. Works for controls that are
 * composed but report zero bounds because they sit far outside the viewport,
 * which synthetic touch injection cannot reach.
 */
fun ComposeTestRule.invokeClick(label: String, substring: Boolean = false) {
    settleAnimations()
    val matcher = hasText(label, substring = substring) and hasClickAction()
    val nodes = onAllNodes(matcher)
    nodes[0].performSemanticsAction(SemanticsActions.OnClick)
    settleAnimations(500)
}

/**
 * Clicks the first node whose text or content description contains any of
 * [labels]. Extended FABs show a label while collapsed FABs only expose a
 * content description, so both forms need to work.
 */
fun ComposeTestRule.tapAny(vararg labels: String, timeout: Long = UI_TIMEOUT) {
    val deadline = System.currentTimeMillis() + timeout
    while (System.currentTimeMillis() < deadline) {
        settleAnimations(250)
        labels.forEach { label ->
            val matcher = (hasText(label, substring = true) or hasContentDescription(label)) and
                hasClickAction()
            val nodes = onAllNodes(matcher)
            if (nodes.fetchSemanticsNodes().isNotEmpty()) {
                scrollIntoView(matcher)
                nodes[0].scrollIfPossible().performClick()
                settleAnimations(250)
                return
            }
        }
    }
    throw AssertionError("None of ${labels.toList()} were clickable.")
}

fun ComposeTestRule.goBack() {
    tapContentDescription("Go back")
    settleAnimations()
}

/** Dismisses any active snackbar so it cannot swallow a tap aimed underneath it. */
fun ComposeTestRule.dismissSnackbars() {
    val dismissable = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
    repeat(3) {
        val nodes = onAllNodes(dismissable)
        if (nodes.fetchSemanticsNodes().isEmpty()) return
        nodes[0].performSemanticsAction(SemanticsActions.Dismiss)
        settleAnimations(500)
    }
}

/**
 * Taps the student enquiries floating action button.
 *
 * The collapsed FAB is rendered without a content description (reported as an
 * accessibility bug), so it has to be located by position: it is the only
 * clickable node in the bottom-end above the navigation bar.
 */
fun ComposeTestRule.tapEnquiryFab() {
    dismissSnackbars()
    fun isFab(node: androidx.compose.ui.semantics.SemanticsNode): Boolean {
        val bounds = node.boundsInRoot
        return bounds.left > 400f &&
            bounds.top > 1_500f &&
            bounds.top < 2_145f &&
            bounds.width < 600f &&
            bounds.height < 250f
    }
    repeat(10) {
        val fab = onAllNodes(hasClickAction())
        val index = fab.fetchSemanticsNodes().indexOfFirst(::isFab)
        if (index >= 0) {
            fab[index].performClick()
            settleAnimations()
            return
        }
        settleAnimations(400)
    }
    throw AssertionError("Enquiry floating action button was not found.")
}

/**
 * Clicks the first clickable node whose text matches [regex]. Used for
 * generated labels such as time-slot chips ("9:00 AM").
 */
fun ComposeTestRule.clickFirstClickableMatching(regex: Regex): Boolean {
    settleAnimations()
    val matcher = hasClickAction()
    val nodes = onAllNodes(matcher).fetchSemanticsNodes()
    val index = nodes.indexOfFirst { node ->
        val texts = node.config
            .firstOrNull { it.key == SemanticsProperties.Text }
            ?.value as? List<*>
        texts?.filterIsInstance<AnnotatedString>()
            ?.any { regex.matches(it.text.trim()) } == true
    }
    if (index < 0) return false
    onAllNodes(matcher)[index].scrollIfPossible().performClick()
    return true
}

private fun SemanticsNodeInteraction.scrollIfPossible(): SemanticsNodeInteraction =
    try {
        performScrollTo()
    } catch (_: Throwable) {
        this
    }
