package mihon.telemetry

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

object TelemetryConfig {
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null

    fun init(context: Context) {
        // To stop forks/test builds from polluting our data
        if (!context.isTaisonProductionApp()) return

        // Check if Google Play Services is available before initializing Firebase
        if (!isGooglePlayServicesAvailable(context)) {
            logcat(LogPriority.WARN) { "Google Play Services not available, skipping Firebase initialization" }
            return
        }

        try {
            analytics = FirebaseAnalytics.getInstance(context)
            FirebaseApp.initializeApp(context)
            crashlytics = FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to initialize Firebase" }
        }
    }

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        return try {
            val availability = GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(context)
            resultCode == ConnectionResult.SUCCESS
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Unable to check Google Play Services availability" }
            false
        }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        analytics?.setAnalyticsCollectionEnabled(enabled)
    }

    fun setCrashlyticsEnabled(enabled: Boolean) {
        crashlytics?.isCrashlyticsCollectionEnabled = enabled
    }

    private fun Context.isTaisonProductionApp(): Boolean {
        if (packageName !in TAISON_PACKAGES) return false

        return packageManager.getPackageInfo(packageName, SignatureFlags)
            .getCertificateFingerprints()
            .any { it == TAISON_CERTIFICATE_FINGERPRINT }
    }
}

private val TAISON_PACKAGES = hashSetOf("com.gent8.taison", "com.gent8.taison.debug")
private const val TAISON_CERTIFICATE_FINGERPRINT = ""
