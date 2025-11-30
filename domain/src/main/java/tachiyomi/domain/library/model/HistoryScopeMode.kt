package tachiyomi.domain.library.model

enum class HistoryScopeMode {
    BY_CATEGORY,

    BY_SOURCE,

    BY_STATUS,

    UNGROUPED,
    ;

    companion object {
        fun fromLegacyBoolean(scopeEnabled: Boolean): HistoryScopeMode {
            return if (scopeEnabled) BY_CATEGORY else UNGROUPED
        }

        fun fromLibraryGroup(groupType: Int): HistoryScopeMode {
            return when (groupType) {
                LibraryGroup.BY_SOURCE -> BY_SOURCE
                LibraryGroup.BY_STATUS -> BY_STATUS
                LibraryGroup.UNGROUPED -> UNGROUPED
                else -> BY_CATEGORY
            }
        }
    }
}
