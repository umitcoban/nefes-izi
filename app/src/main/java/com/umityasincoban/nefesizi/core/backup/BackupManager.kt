package com.umityasincoban.nefesizi.core.backup

import com.umityasincoban.nefesizi.BuildConfig
import com.umityasincoban.nefesizi.core.data.AppPreferences
import com.umityasincoban.nefesizi.core.database.NefesIziDao
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

enum class ImportMode { MERGE, REPLACE }

data class ImportPreview(
    val data: BackupData?,
    val validation: BackupValidation?,
    val duplicateCount: Int = 0,
    val conflictCount: Int = 0,
    val error: String? = null,
) {
    val canReplace: Boolean
        get() = data != null && validation?.isValid == true && error == null
    val canMerge: Boolean get() = canReplace && conflictCount == 0
}

data class ImportComparison(val duplicateCount: Int, val conflictCount: Int)

fun compareBackups(incoming: BackupData, existing: BackupData): ImportComparison {
    var duplicate = 0
    var conflict = 0
    fun <T> compare(values: List<T>, current: Map<String, T>, id: (T) -> String) {
        values.forEach { item ->
            current[id(item)]?.let { stored ->
                if (stored == item) duplicate++ else conflict++
            }
        }
    }
    compare(incoming.products, existing.products.associateBy { it.id }) { it.id }
    compare(incoming.revisions, existing.revisions.associateBy { it.id }) { it.id }
    compare(incoming.records, existing.records.associateBy { it.id }) { it.id }
    compare(incoming.healthEntries, existing.healthEntries.associateBy { it.entryDate }) {
        it.entryDate
    }
    return ImportComparison(duplicate, conflict)
}

@Singleton
class BackupManager @Inject constructor(
    private val dao: NefesIziDao,
    private val preferences: AppPreferences,
    private val clock: Clock,
) {
    suspend fun createBackup(): BackupData {
        val personalization = preferences.personalization.first()
        val display = preferences.todayDisplayPreferences.first()
        val theme = preferences.themeMode.first()
        return BackupData(
            products = dao.getAllProducts(),
            revisions = dao.getAllProductRevisions(),
            records = dao.getAllRecords(),
            healthEntries = dao.getAllHealthEntries(),
            preferences = mapOf(
                "themeMode" to theme.name,
                "dynamicColor" to personalization.dynamicColor.toString(),
                "preferredCurrency" to personalization.preferredCurrency,
                "dayStartHour" to personalization.dayStartHour.toString(),
                "firstDayOfWeek" to personalization.firstDayOfWeek,
                "showHealthTab" to personalization.showHealthTab.toString(),
                "showCost" to display.showCost.toString(),
                "showExposure" to display.showExposure.toString(),
            ),
            exportedAtEpochMillis = clock.millis(),
            appVersion = BuildConfig.VERSION_NAME,
        )
    }

    suspend fun createJson(): String = BackupCodec.encode(createBackup())

    suspend fun createCsvZip(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            createBackup().toCsvFiles().forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    suspend fun preview(value: String): ImportPreview {
        val decoded = BackupCodec.decode(value).getOrElse {
            return ImportPreview(null, null, error = it.message ?: "Yedek okunamadı.")
        }
        val validation = BackupCodec.validate(decoded)
        if (!validation.isValid) return ImportPreview(decoded, validation)
        val comparison = compareBackups(
            decoded,
            BackupData(
                products = dao.getAllProducts(),
                revisions = dao.getAllProductRevisions(),
                records = dao.getAllRecords(),
                healthEntries = dao.getAllHealthEntries(),
                preferences = emptyMap(),
                exportedAtEpochMillis = 0,
                appVersion = "",
            ),
        )
        return ImportPreview(
            decoded,
            validation,
            comparison.duplicateCount,
            comparison.conflictCount,
        )
    }

    suspend fun import(preview: ImportPreview, mode: ImportMode) {
        require(if (mode == ImportMode.REPLACE) preview.canReplace else preview.canMerge)
        val data = checkNotNull(preview.data)
        if (mode == ImportMode.REPLACE) {
            dao.replaceAllData(data.products, data.revisions, data.records, data.healthEntries)
        } else {
            val productExisting = dao.getAllProducts().associateBy { it.id }
            val revisionExisting = dao.getAllProductRevisions().associateBy { it.id }
            val recordExisting = dao.getAllRecords().associateBy { it.id }
            val healthExisting = dao.getAllHealthEntries().associateBy { it.entryDate }
            dao.mergeData(
                data.products.filter { productExisting[it.id] == null },
                data.revisions.filter { revisionExisting[it.id] == null },
                data.records.filter { recordExisting[it.id] == null },
                data.healthEntries.filter { healthExisting[it.entryDate] == null },
            )
        }
        applyPortablePreferences(data.preferences)
    }

    private suspend fun applyPortablePreferences(values: Map<String, String>) {
        values["themeMode"]?.let { stored ->
            com.umityasincoban.nefesizi.core.data.ThemeMode.entries
                .firstOrNull { it.name == stored }
                ?.let { preferences.setThemeMode(it) }
        }
        values["dynamicColor"]?.toBooleanStrictOrNull()?.let { preferences.setDynamicColor(it) }
        values["preferredCurrency"]?.let { preferences.setPreferredCurrency(it) }
        values["dayStartHour"]?.toIntOrNull()?.let { preferences.setDayStartHour(it) }
        values["firstDayOfWeek"]?.let { preferences.setFirstDayOfWeek(it) }
        values["showHealthTab"]?.toBooleanStrictOrNull()?.let { preferences.setShowHealthTab(it) }
        values["showCost"]?.toBooleanStrictOrNull()?.let { preferences.setShowTodayCost(it) }
        values["showExposure"]?.toBooleanStrictOrNull()?.let { preferences.setShowTodayExposure(it) }
    }
}
