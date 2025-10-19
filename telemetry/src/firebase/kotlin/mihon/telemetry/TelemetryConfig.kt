package mihon.telemetry

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

object TelemetryConfig {
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null

    fun init(context: Context) {
        // To stop forks/test builds from polluting our data
        if (!context.isTaisonProductionApp()) return

        analytics = FirebaseAnalytics.getInstance(context)
        FirebaseApp.initializeApp(context)
        crashlytics = FirebaseCrashlytics.getInstance()
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
