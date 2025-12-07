package eu.kanade.domain.extension.interactor

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.domain.source.service.SourcePreferences
import mihon.domain.extensionrepo.repository.ExtensionRepoRepository
import tachiyomi.core.common.preference.getAndSet

class TrustExtension(
    private val extensionRepoRepository: ExtensionRepoRepository,
    private val preferences: SourcePreferences,
) {

    suspend fun isTrusted(pkgInfo: PackageInfo, fingerprints: List<String>): Boolean {
        val trustedFingerprints = extensionRepoRepository.getAll().map { it.signingKeyFingerprint }.toHashSet()
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
        val signatureHash = fingerprints.last()
        val key = key(pkgInfo.packageName, versionCode, signatureHash)
        if (key in preferences.pendingExtensionsToTrust().get()) {
            return false
        }
        return trustedFingerprints.any { fingerprints.contains(it) } || key in preferences.trustedExtensions().get()
    }

    fun trust(pkgName: String, versionCode: Long, signatureHash: String) {
        preferences.trustedExtensions().getAndSet { exts ->
            // Remove previously trusted versions
            val removed = exts.filterNot { it.startsWith("$pkgName:") }.toMutableSet()

            removed.also { it += "$pkgName:$versionCode:$signatureHash" }
        }
        clearPending(pkgName)
    }

    fun markPending(pkgName: String, versionCode: Long, signatureHash: String) {
        preferences.pendingExtensionsToTrust().getAndSet { pending ->
            pending + key(pkgName, versionCode, signatureHash)
        }
    }

    fun clearPending(pkgName: String) {
        preferences.pendingExtensionsToTrust().getAndSet { pending ->
            pending.filterNot { it.startsWith("$pkgName:") }.toSet()
        }
    }

    fun revokeAll() {
        preferences.trustedExtensions().delete()
        preferences.pendingExtensionsToTrust().delete()
    }

    private fun key(pkgName: String, versionCode: Long, signatureHash: String): String {
        return "$pkgName:$versionCode:$signatureHash"
    }
}
