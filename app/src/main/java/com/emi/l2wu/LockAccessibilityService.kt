package com.emi.l2wu

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

class LockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {

    }

    override fun onInterrupt() {

    }

    companion object {
        var instance: LockAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    fun lockScreen() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)

        // delay() keeps the screen off after LOCK, if the user keeps the phone still in the
        // "lift" position
        runBlocking {
            delay(4000.milliseconds)
        }
    }
}