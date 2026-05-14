package tachiyomi.domain.collection.model

data class CollectionWithEntryCount(
    val collection: Collection,
    val entryCount: Long,
)
