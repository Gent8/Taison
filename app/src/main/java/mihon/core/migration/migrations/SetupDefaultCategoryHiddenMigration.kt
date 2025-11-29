package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.manga.interactor.GetLibraryManga

class SetupDefaultCategoryHiddenMigration : Migration {
    override val version: Float = 11f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val getCategories = migrationContext.get<GetCategories>() ?: return@withIOContext false
        val getLibraryManga = migrationContext.get<GetLibraryManga>() ?: return@withIOContext false
        val categoryRepository = migrationContext.get<CategoryRepository>() ?: return@withIOContext false

        val categories = getCategories.await()
        val defaultCategory = categories.find { it.isSystemCategory } ?: return@withIOContext false

        val libraryManga = getLibraryManga.await()
        val hasMangaInDefault = libraryManga.any { manga ->
            manga.categories.isEmpty() || manga.categories.contains(Category.UNCATEGORIZED_ID)
        }

        val shouldBeHidden = !hasMangaInDefault

        if (shouldBeHidden && !defaultCategory.hidden) {
            categoryRepository.updatePartial(
                CategoryUpdate(
                    id = Category.UNCATEGORIZED_ID,
                    hidden = true,
                ),
            )
        }

        return@withIOContext true
    }
}
