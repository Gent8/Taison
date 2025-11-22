package eu.kanade.tachiyomi.util.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.domain.library.service.LibraryPreferences

/**
 * Stores the last used category id in memory so all tabs can react immediately while still
 * persisting the value via preferences.
 */
class LastUsedCategoryState(
    libraryPreferences: LibraryPreferences,
) {

    private val preference = libraryPreferences.lastUsedCategoryId()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(preference.get())

    val state: StateFlow<Long> = _state.asStateFlow()

    val current: Long
        get() = state.value

    init {
        preference.changes()
            .onEach { value -> _state.value = value }
            .launchIn(scope)
    }

    fun set(categoryId: Long) {
        if (_state.value == categoryId) return
        _state.value = categoryId
        preference.set(categoryId)
    }
}
