package com.hrshd1eux.expensetracker

import com.hrshd1eux.expensetracker.security.AppLockManager
import com.hrshd1eux.expensetracker.security.LockTimeout
import com.hrshd1eux.expensetracker.security.PinSecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

class SecurityTest {

    private lateinit var appLockManager: AppLockManager

    @Before
    fun setup() {
        appLockManager = AppLockManager()
    }

    @Test
    fun testGenerateSalt_producesRandomNonEmptyUniqueStrings() {
        val salt1 = PinSecurityHelper.generateSalt()
        val salt2 = PinSecurityHelper.generateSalt()

        assertTrue(salt1.isNotEmpty())
        assertTrue(salt2.isNotEmpty())
        assertNotEquals("Consecutive salts must be cryptographically distinct", salt1, salt2)

        val decoded1 = Base64.getDecoder().decode(salt1)
        assertEquals(16, decoded1.size)
    }

    @Test
    fun testHashPin_deterministicWithSameSalt() {
        val pin = "1234"
        val salt = PinSecurityHelper.generateSalt()

        val hash1 = PinSecurityHelper.hashPin(pin, salt)
        val hash2 = PinSecurityHelper.hashPin(pin, salt)

        assertEquals("Same PIN and salt must yield identical hash", hash1, hash2)
        assertNotEquals("PIN must never match raw hash", pin, hash1)
    }

    @Test
    fun testHashPin_differentPinsOrSaltsProduceDifferentHashes() {
        val salt = PinSecurityHelper.generateSalt()
        val hash1 = PinSecurityHelper.hashPin("1234", salt)
        val hash2 = PinSecurityHelper.hashPin("5678", salt)

        assertNotEquals(hash1, hash2)

        val salt2 = PinSecurityHelper.generateSalt()
        val hash3 = PinSecurityHelper.hashPin("1234", salt2)
        assertNotEquals(hash1, hash3)
    }

    @Test
    fun testVerifyPin_correctAndIncorrectAttempts() {
        val pin = "4321"
        val salt = PinSecurityHelper.generateSalt()
        val storedHash = PinSecurityHelper.hashPin(pin, salt)

        assertTrue("Correct PIN verification must return true", PinSecurityHelper.verifyPin("4321", salt, storedHash))
        assertFalse("Wrong PIN verification must return false", PinSecurityHelper.verifyPin("0000", salt, storedHash))
        assertFalse("Empty PIN verification must return false", PinSecurityHelper.verifyPin("", salt, storedHash))
        assertFalse("Off-by-one PIN verification must return false", PinSecurityHelper.verifyPin("4322", salt, storedHash))
    }

    @Test
    fun testConstantTimeEquals() {
        assertTrue(PinSecurityHelper.constantTimeEquals("abcdef", "abcdef"))
        assertFalse(PinSecurityHelper.constantTimeEquals("abcdef", "abcdeg"))
        assertFalse(PinSecurityHelper.constantTimeEquals("abcdef", "abcde"))
        assertFalse(PinSecurityHelper.constantTimeEquals("", "a"))
        assertTrue(PinSecurityHelper.constantTimeEquals("", ""))
    }

    @Test
    fun testAppLockManager_disabledLock_neverLocks() {
        appLockManager.onAppEnteredForeground(appLockEnabled = false, timeoutMinutes = 0)
        assertFalse(appLockManager.isLocked.value)

        appLockManager.onAppEnteredBackground()
        appLockManager.onAppEnteredForeground(appLockEnabled = false, timeoutMinutes = 5)
        assertFalse(appLockManager.isLocked.value)
    }

    @Test
    fun testAppLockManager_firstLaunch_locksWhenEnabled() {
        appLockManager.onAppEnteredForeground(appLockEnabled = true, timeoutMinutes = 0)
        assertTrue(appLockManager.isLocked.value)

        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)
    }

    @Test
    fun testAppLockManager_immediateTimeout_locksOnBackgroundAndForeground() {
        appLockManager.unlock()
        appLockManager.onAppEnteredBackground()
        // Immediately timeout (0 minutes)
        appLockManager.onAppEnteredForeground(appLockEnabled = true, timeoutMinutes = LockTimeout.IMMEDIATELY.minutes)
        assertTrue(appLockManager.isLocked.value)
    }

    @Test
    fun testAppLockManager_unlockResetsLockState() {
        appLockManager.setLocked(true)
        assertTrue(appLockManager.isLocked.value)

        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)
    }
}
