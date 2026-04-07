package tachiyomi.domain.collection.model

object RelationshipLabel {
    const val NONE = ""
    const val PREQUEL = "prequel"
    const val SEQUEL = "sequel"
    const val SPIN_OFF = "spin-off"
    const val SIDE_STORY = "side story"
    const val ALTERNATE = "alternate"

    val PREDEFINED = listOf(NONE, PREQUEL, SEQUEL, SPIN_OFF, SIDE_STORY, ALTERNATE)
}
