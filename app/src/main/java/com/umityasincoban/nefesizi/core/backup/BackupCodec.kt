package com.umityasincoban.nefesizi.core.backup

import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.CigaretteProductRevisionEntity
import com.umityasincoban.nefesizi.core.database.DailyHealthEntryEntity
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

const val BACKUP_SCHEMA_VERSION = 1

data class BackupData(
    val products: List<CigaretteProductEntity>,
    val revisions: List<CigaretteProductRevisionEntity>,
    val records: List<SmokingRecordEntity>,
    val healthEntries: List<DailyHealthEntryEntity>,
    val preferences: Map<String, String>,
    val exportedAtEpochMillis: Long,
    val appVersion: String,
)

data class BackupValidation(
    val errors: List<String>,
    val productCount: Int,
    val revisionCount: Int,
    val recordCount: Int,
    val healthEntryCount: Int,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

object BackupCodec {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = false }

    fun encode(data: BackupData): String = json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("schemaVersion", JsonPrimitive(BACKUP_SCHEMA_VERSION))
            put("exportedAtEpochMillis", JsonPrimitive(data.exportedAtEpochMillis))
            put("appVersion", JsonPrimitive(data.appVersion))
            put("products", buildJsonArray { data.products.forEach { add(it.toJson()) } })
            put("revisions", buildJsonArray { data.revisions.forEach { add(it.toJson()) } })
            put("records", buildJsonArray { data.records.forEach { add(it.toJson()) } })
            put("healthEntries", buildJsonArray { data.healthEntries.forEach { add(it.toJson()) } })
            put(
                "preferences",
                JsonObject(data.preferences.mapValues { JsonPrimitive(it.value) }),
            )
        },
    )

    fun decode(value: String): Result<BackupData> = runCatching {
        val root = json.parseToJsonElement(value).jsonObject
        require(root.long("schemaVersion") == BACKUP_SCHEMA_VERSION.toLong()) {
            "Desteklenmeyen yedek sürümü."
        }
        BackupData(
            products = root.array("products").map { it.jsonObject.toProduct() },
            revisions = root.array("revisions").map { it.jsonObject.toRevision() },
            records = root.array("records").map { it.jsonObject.toRecord() },
            healthEntries = root.array("healthEntries").map { it.jsonObject.toHealthEntry() },
            preferences = root["preferences"]?.jsonObject?.mapValues {
                it.value.jsonPrimitive.content
            }.orEmpty(),
            exportedAtEpochMillis = root.long("exportedAtEpochMillis"),
            appVersion = root.string("appVersion"),
        )
    }

    fun validate(data: BackupData): BackupValidation {
        val errors = mutableListOf<String>()
        fun duplicateIds(values: List<String>, label: String) {
            if (values.size != values.distinct().size) errors += "$label içinde yinelenen kimlik var."
        }
        duplicateIds(data.products.map(CigaretteProductEntity::id), "Ürünler")
        duplicateIds(data.revisions.map(CigaretteProductRevisionEntity::id), "Revizyonlar")
        duplicateIds(data.records.map(SmokingRecordEntity::id), "Kayıtlar")
        duplicateIds(data.healthEntries.map(DailyHealthEntryEntity::entryDate), "Sağlık günleri")
        if (data.revisions.map { it.productId to it.effectiveFromEpochMillis }.distinct().size !=
            data.revisions.size
        ) {
            errors += "Aynı ürün ve yürürlük anı için birden fazla revizyon var."
        }
        if (data.products.count(CigaretteProductEntity::isDefault) > 1) {
            errors += "Birden fazla varsayılan ürün var."
        }
        if (data.products.any { it.isDefault && it.isArchived }) {
            errors += "Arşivlenmiş ürün varsayılan olamaz."
        }
        val productIds = data.products.map(CigaretteProductEntity::id).toSet()
        val revisionsById = data.revisions.associateBy(CigaretteProductRevisionEntity::id)
        data.revisions.filter { it.productId !in productIds }.forEach {
            errors += "${it.id} revizyonunun ürünü bulunmuyor."
        }
        data.records.filter { it.productId != null && it.productId !in productIds }.forEach {
            errors += "${it.id} kaydının ürünü bulunmuyor."
        }
        data.records.forEach { record ->
            record.productRevisionIdSnapshot?.let { revisionId ->
                val revision = revisionsById[revisionId]
                if (revision == null) {
                    errors += "${record.id} kaydının revizyonu bulunmuyor."
                } else if (record.productId == null || revision.productId != record.productId) {
                    errors += "${record.id} kaydının ürün ve revizyon ilişkisi geçersiz."
                }
            }
        }
        data.records.filter { it.quantity !in 1..99 || it.consumedQuarter !in 1..4 }.forEach {
            errors += "${it.id} kaydının miktar/oran değeri geçersiz."
        }
        data.healthEntries.forEach { entry ->
            if (entry.energyLevel != null && entry.energyLevel !in 1..5 ||
                entry.stressLevel != null && entry.stressLevel !in 1..5 ||
                entry.sleepQuality != null && entry.sleepQuality !in 1..5
            ) {
                errors += "${entry.entryDate} sağlık gününün ölçek değeri geçersiz."
            }
            if (entry.restingHeartRate != null && entry.restingHeartRate <= 0 ||
                entry.exerciseMinutes != null && entry.exerciseMinutes < 0 ||
                entry.weightGrams != null && entry.weightGrams <= 0
            ) {
                errors += "${entry.entryDate} sağlık gününün ölçüm değeri geçersiz."
            }
            val systolic = entry.systolicBloodPressure
            val diastolic = entry.diastolicBloodPressure
            if ((systolic == null) != (diastolic == null) ||
                systolic != null && diastolic != null &&
                (systolic <= 0 || diastolic <= 0 || systolic <= diastolic)
            ) {
                errors += "${entry.entryDate} sağlık gününün tansiyon değeri geçersiz."
            }
        }
        data.preferences["preferredCurrency"]?.let { code ->
            if (runCatching { java.util.Currency.getInstance(code) }.isFailure) {
                errors += "Tercih edilen para birimi geçersiz."
            }
        }
        data.preferences["dayStartHour"]?.let { value ->
            if (value.toIntOrNull() !in 0..23) errors += "Gün başlangıç saati geçersiz."
        }
        data.preferences["firstDayOfWeek"]?.let { value ->
            if (value !in setOf("MONDAY", "SUNDAY")) {
                errors += "Haftanın ilk günü tercihi geçersiz."
            }
        }
        data.products.filter { it.name.isBlank() }.forEach { errors += "Boş ürün adı var." }
        return BackupValidation(
            errors.distinct(),
            data.products.size,
            data.revisions.size,
            data.records.size,
            data.healthEntries.size,
        )
    }
}

fun csvEscape(value: String?): String {
    if (value == null) return ""
    val safe = if (value.firstOrNull() in listOf('=', '+', '-', '@')) "'$value" else value
    return "\"${safe.replace("\"", "\"\"")}\""
}

fun BackupData.toCsvFiles(): Map<String, String> = mapOf(
    "products.csv" to buildString {
        appendLine(
            "id,name,brand,variant,nicotineMicrogramsPerCigarette,tarMicrogramsPerCigarette," +
                "carbonMonoxideMicrogramsPerCigarette,priceMicrosPerCigarette,currencyCode," +
                "valueSource,isDefault,isArchived,createdAtEpochMillis,updatedAtEpochMillis",
        )
        products.forEach {
            appendLine(
                listOf(
                    it.id, it.name, it.brand, it.variant, it.nicotineMicrogramsPerCigarette,
                    it.tarMicrogramsPerCigarette, it.carbonMonoxideMicrogramsPerCigarette,
                    it.priceMicrosPerCigarette, it.currencyCode, it.valueSource, it.isDefault,
                    it.isArchived, it.createdAtEpochMillis, it.updatedAtEpochMillis,
                )
                    .joinToString(",") { value -> csvEscape(value?.toString()) },
            )
        }
    },
    "product_revisions.csv" to buildString {
        appendLine(
            "id,productId,effectiveFromEpochMillis,nicotineMicrogramsPerCigarette," +
                "tarMicrogramsPerCigarette,carbonMonoxideMicrogramsPerCigarette,packPriceMicros," +
                "cigarettesPerPack,priceMicrosPerCigarette,currencyCode,valueSource,createdAtEpochMillis",
        )
        revisions.forEach {
            appendLine(
                listOf(
                    it.id, it.productId, it.effectiveFromEpochMillis,
                    it.nicotineMicrogramsPerCigarette, it.tarMicrogramsPerCigarette,
                    it.carbonMonoxideMicrogramsPerCigarette, it.packPriceMicros,
                    it.cigarettesPerPack, it.priceMicrosPerCigarette, it.currencyCode,
                    it.valueSource, it.createdAtEpochMillis,
                )
                    .joinToString(",") { value -> csvEscape(value?.toString()) },
            )
        }
    },
    "smoking_records.csv" to buildString {
        appendLine(
            "id,smokedAtEpochMillis,zoneIdSnapshot,quantity,consumedQuarter,productId," +
                "productRevisionIdSnapshot,productNameSnapshot,nicotineMicrogramsPerCigaretteSnapshot," +
                "tarMicrogramsPerCigaretteSnapshot,carbonMonoxideMicrogramsPerCigaretteSnapshot," +
                "priceMicrosPerCigaretteSnapshot,currencyCodeSnapshot,valueSourceSnapshot,cravingLevel," +
                "trigger,mood,locationType,note,createdAtEpochMillis,updatedAtEpochMillis",
        )
        records.forEach {
            appendLine(
                listOf(
                    it.id, it.smokedAtEpochMillis, it.zoneIdSnapshot, it.quantity,
                    it.consumedQuarter, it.productId, it.productRevisionIdSnapshot,
                    it.productNameSnapshot, it.nicotineMicrogramsPerCigaretteSnapshot,
                    it.tarMicrogramsPerCigaretteSnapshot,
                    it.carbonMonoxideMicrogramsPerCigaretteSnapshot,
                    it.priceMicrosPerCigaretteSnapshot, it.currencyCodeSnapshot,
                    it.valueSourceSnapshot, it.cravingLevel, it.trigger, it.mood,
                    it.locationType, it.note, it.createdAtEpochMillis, it.updatedAtEpochMillis,
                )
                    .joinToString(",") { value -> csvEscape(value?.toString()) },
            )
        }
    },
    "health_entries.csv" to buildString {
        appendLine(
            "entryDate,zoneId,energyLevel,stressLevel,sleepQuality,morningCough,headache," +
                "shortnessOfBreath,chestDiscomfort,restingHeartRate,exerciseMinutes," +
                "systolicBloodPressure,diastolicBloodPressure,weightGrams,note," +
                "createdAtEpochMillis,updatedAtEpochMillis",
        )
        healthEntries.forEach {
            appendLine(
                listOf(
                    it.entryDate, it.zoneId, it.energyLevel, it.stressLevel, it.sleepQuality,
                    it.morningCough, it.headache, it.shortnessOfBreath, it.chestDiscomfort,
                    it.restingHeartRate, it.exerciseMinutes, it.systolicBloodPressure,
                    it.diastolicBloodPressure, it.weightGrams, it.note,
                    it.createdAtEpochMillis, it.updatedAtEpochMillis,
                ).joinToString(",") { value -> csvEscape(value?.toString()) },
            )
        }
    },
)

private fun CigaretteProductEntity.toJson() = buildJsonObject {
    put("id", id); put("name", name); putNullable("brand", brand); putNullable("variant", variant)
    putNullable("nicotine", nicotineMicrogramsPerCigarette); putNullable("tar", tarMicrogramsPerCigarette)
    putNullable("co", carbonMonoxideMicrogramsPerCigarette); putNullable("price", priceMicrosPerCigarette)
    put("currency", currencyCode); put("source", valueSource); put("default", isDefault)
    put("archived", isArchived); put("created", createdAtEpochMillis); put("updated", updatedAtEpochMillis)
}

private fun CigaretteProductRevisionEntity.toJson() = buildJsonObject {
    put("id", id); put("productId", productId); put("effective", effectiveFromEpochMillis)
    putNullable("nicotine", nicotineMicrogramsPerCigarette); putNullable("tar", tarMicrogramsPerCigarette)
    putNullable("co", carbonMonoxideMicrogramsPerCigarette); putNullable("packPrice", packPriceMicros)
    putNullable("packCount", cigarettesPerPack?.toLong()); putNullable("price", priceMicrosPerCigarette)
    put("currency", currencyCode); put("source", valueSource); put("created", createdAtEpochMillis)
}

private fun SmokingRecordEntity.toJson() = buildJsonObject {
    put("id", id); put("at", smokedAtEpochMillis); put("zone", zoneIdSnapshot); put("quantity", quantity)
    put("quarter", consumedQuarter); putNullable("productId", productId); putNullable("revisionId", productRevisionIdSnapshot)
    put("productName", productNameSnapshot); putNullable("nicotine", nicotineMicrogramsPerCigaretteSnapshot)
    putNullable("tar", tarMicrogramsPerCigaretteSnapshot); putNullable("co", carbonMonoxideMicrogramsPerCigaretteSnapshot)
    putNullable("price", priceMicrosPerCigaretteSnapshot); put("currency", currencyCodeSnapshot)
    putNullable("source", valueSourceSnapshot); putNullable("craving", cravingLevel?.toLong())
    putNullable("trigger", trigger); putNullable("mood", mood); putNullable("location", locationType)
    putNullable("note", note); put("created", createdAtEpochMillis); put("updated", updatedAtEpochMillis)
}

private fun DailyHealthEntryEntity.toJson() = buildJsonObject {
    put("date", entryDate); put("zone", zoneId); putNullable("energy", energyLevel?.toLong())
    putNullable("stress", stressLevel?.toLong()); putNullable("sleep", sleepQuality?.toLong())
    putNullable("cough", morningCough); putNullable("headache", headache)
    putNullable("breath", shortnessOfBreath); putNullable("chest", chestDiscomfort)
    putNullable("heartRate", restingHeartRate?.toLong()); putNullable("exercise", exerciseMinutes?.toLong())
    putNullable("systolic", systolicBloodPressure?.toLong()); putNullable("diastolic", diastolicBloodPressure?.toLong())
    putNullable("weight", weightGrams); putNullable("note", note); put("created", createdAtEpochMillis)
    put("updated", updatedAtEpochMillis)
}

private fun JsonObject.toProduct() = CigaretteProductEntity(
    id = string("id"), name = string("name"), brand = nullableString("brand"), variant = nullableString("variant"),
    nicotineMicrogramsPerCigarette = nullableLong("nicotine"), tarMicrogramsPerCigarette = nullableLong("tar"),
    carbonMonoxideMicrogramsPerCigarette = nullableLong("co"), priceMicrosPerCigarette = nullableLong("price"),
    currencyCode = string("currency"), valueSource = string("source"), isDefault = bool("default"),
    isArchived = bool("archived"), createdAtEpochMillis = long("created"), updatedAtEpochMillis = long("updated"),
)

private fun JsonObject.toRevision() = CigaretteProductRevisionEntity(
    id = string("id"), productId = string("productId"), effectiveFromEpochMillis = long("effective"),
    nicotineMicrogramsPerCigarette = nullableLong("nicotine"), tarMicrogramsPerCigarette = nullableLong("tar"),
    carbonMonoxideMicrogramsPerCigarette = nullableLong("co"), packPriceMicros = nullableLong("packPrice"),
    cigarettesPerPack = nullableLong("packCount")?.toInt(), priceMicrosPerCigarette = nullableLong("price"),
    currencyCode = string("currency"), valueSource = string("source"), createdAtEpochMillis = long("created"),
)

private fun JsonObject.toRecord() = SmokingRecordEntity(
    id = string("id"), smokedAtEpochMillis = long("at"), zoneIdSnapshot = string("zone"),
    quantity = long("quantity").toInt(), consumedQuarter = long("quarter").toInt(),
    productId = nullableString("productId"), productRevisionIdSnapshot = nullableString("revisionId"),
    productNameSnapshot = string("productName"), nicotineMicrogramsPerCigaretteSnapshot = nullableLong("nicotine"),
    tarMicrogramsPerCigaretteSnapshot = nullableLong("tar"), carbonMonoxideMicrogramsPerCigaretteSnapshot = nullableLong("co"),
    priceMicrosPerCigaretteSnapshot = nullableLong("price"), currencyCodeSnapshot = string("currency"),
    valueSourceSnapshot = nullableString("source"), cravingLevel = nullableLong("craving")?.toInt(),
    trigger = nullableString("trigger"), mood = nullableString("mood"), locationType = nullableString("location"),
    note = nullableString("note"), createdAtEpochMillis = long("created"), updatedAtEpochMillis = long("updated"),
)

private fun JsonObject.toHealthEntry() = DailyHealthEntryEntity(
    entryDate = string("date"), zoneId = string("zone"), energyLevel = nullableLong("energy")?.toInt(),
    stressLevel = nullableLong("stress")?.toInt(), sleepQuality = nullableLong("sleep")?.toInt(),
    morningCough = nullableBool("cough"), headache = nullableBool("headache"),
    shortnessOfBreath = nullableBool("breath"), chestDiscomfort = nullableBool("chest"),
    restingHeartRate = nullableLong("heartRate")?.toInt(), exerciseMinutes = nullableLong("exercise")?.toInt(),
    systolicBloodPressure = nullableLong("systolic")?.toInt(), diastolicBloodPressure = nullableLong("diastolic")?.toInt(),
    weightGrams = nullableLong("weight"), note = nullableString("note"), createdAtEpochMillis = long("created"),
    updatedAtEpochMillis = long("updated"),
)

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?) {
    put(key, value?.let(::JsonPrimitive) ?: JsonNull)
}
private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Long?) {
    put(key, value?.let(::JsonPrimitive) ?: JsonNull)
}
private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Boolean?) {
    put(key, value?.let(::JsonPrimitive) ?: JsonNull)
}
private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
private fun JsonObject.long(key: String) = getValue(key).jsonPrimitive.longOrNull ?: error("$key geçersiz")
private fun JsonObject.bool(key: String) = getValue(key).jsonPrimitive.booleanOrNull ?: error("$key geçersiz")
private fun JsonObject.nullableString(key: String) = get(key)?.jsonPrimitive?.contentOrNull
private fun JsonObject.nullableLong(key: String) = get(key)?.jsonPrimitive?.longOrNull
private fun JsonObject.nullableBool(key: String) = get(key)?.jsonPrimitive?.booleanOrNull
private fun JsonObject.array(key: String): JsonArray = getValue(key).jsonArray
