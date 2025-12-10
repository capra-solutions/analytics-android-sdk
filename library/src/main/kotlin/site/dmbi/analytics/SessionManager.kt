package site.dmbi.analytics

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.*

/**
 * Manages session and user identifiers
 */
internal class SessionManager(
    private val context: Context,
    private val sessionTimeout: Long
) {
    private val prefs: SharedPreferences
    private val encryptedPrefs: SharedPreferences

    private var _sessionId: String? = null
    private var _userId: String? = null
    private var lastActiveTime: Long = 0L

    val sessionId: String
        get() {
            if (_sessionId == null || shouldStartNewSession()) {
                _sessionId = UUID.randomUUID().toString()
                prefs.edit().putString(SESSION_ID_KEY, _sessionId).apply()
            }
            return _sessionId!!
        }

    val userId: String
        get() {
            if (_userId == null) {
                _userId = encryptedPrefs.getString(USER_ID_KEY, null) ?: createAndStoreUserId()
            }
            return _userId!!
        }

    init {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        _sessionId = prefs.getString(SESSION_ID_KEY, null)
        lastActiveTime = prefs.getLong(LAST_ACTIVE_KEY, 0L)
    }

    /** Update last active time (called on each event) */
    fun updateActivity() {
        lastActiveTime = System.currentTimeMillis()
        prefs.edit().putLong(LAST_ACTIVE_KEY, lastActiveTime).apply()
    }

    /** Check if we should start a new session */
    private fun shouldStartNewSession(): Boolean {
        if (lastActiveTime == 0L) return true
        return System.currentTimeMillis() - lastActiveTime > sessionTimeout
    }

    /** Start a new session (called on app launch or resume) */
    fun startNewSession() {
        _sessionId = UUID.randomUUID().toString()
        prefs.edit().putString(SESSION_ID_KEY, _sessionId).apply()
        updateActivity()
    }

    private fun createAndStoreUserId(): String {
        val userId = UUID.randomUUID().toString()
        encryptedPrefs.edit().putString(USER_ID_KEY, userId).apply()
        return userId
    }

    /** Get device type (android_phone, android_tablet) */
    val deviceType: String
        get() {
            val screenLayout = context.resources.configuration.screenLayout
            val isTablet = (screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
            return if (isTablet) "android_tablet" else "android_phone"
        }

    /** Get user agent string */
    val userAgent: String
        get() {
            val sdkVersion = "1.0.0"
            val osVersion = Build.VERSION.RELEASE
            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            return "DMBIAnalytics/$sdkVersion Android/$osVersion ($deviceModel)"
        }

    companion object {
        private const val PREFS_NAME = "dmbi_analytics"
        private const val ENCRYPTED_PREFS_NAME = "dmbi_analytics_secure"
        private const val SESSION_ID_KEY = "session_id"
        private const val USER_ID_KEY = "user_id"
        private const val LAST_ACTIVE_KEY = "last_active"
    }
}
