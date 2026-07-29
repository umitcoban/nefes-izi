package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.DailyHealthEntryEntity
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HealthMeasurementDraft(
    val restingHeartRate: Int?,
    val exerciseMinutes: Int?,
    val systolicBloodPressure: Int?,
    val diastolicBloodPressure: Int?,
    val weightGrams: Long?,
)

data class HealthValidation(
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    val canSave: Boolean get() = errors.isEmpty()
}

fun validateHealthMeasurements(draft: HealthMeasurementDraft): HealthValidation {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    draft.restingHeartRate?.let {
        if (it <= 0) errors += "Dinlenme nabzı sıfırdan büyük olmalı."
        else if (it !in 35..220) warnings += "Dinlenme nabzını tekrar kontrol et."
    }
    draft.exerciseMinutes?.let {
        if (it < 0) errors += "Egzersiz süresi negatif olamaz."
        else if (it > 600) warnings += "Egzersiz süresini tekrar kontrol et."
    }
    val systolic = draft.systolicBloodPressure
    val diastolic = draft.diastolicBloodPressure
    if ((systolic == null) != (diastolic == null)) {
        errors += "Tansiyon için büyük ve küçük değeri birlikte gir."
    }
    systolic?.let {
        if (it <= 0) errors += "Büyük tansiyon sıfırdan büyük olmalı."
        else if (it !in 60..250) warnings += "Büyük tansiyon değerini tekrar kontrol et."
    }
    diastolic?.let {
        if (it <= 0) errors += "Küçük tansiyon sıfırdan büyük olmalı."
        else if (it !in 30..150) warnings += "Küçük tansiyon değerini tekrar kontrol et."
    }
    if (systolic != null && diastolic != null && systolic <= diastolic) {
        errors += "Büyük tansiyon küçük tansiyondan yüksek olmalı."
    }
    draft.weightGrams?.let {
        if (it <= 0) errors += "Kilo sıfırdan büyük olmalı."
        else if (it !in 20_000L..500_000L) warnings += "Kilo değerini tekrar kontrol et."
    }
    return HealthValidation(errors.distinct(), warnings.distinct())
}

data class HealthAssociation(
    val label: String,
    val yesDays: Int,
    val noDays: Int,
    val missingDays: Int,
    val yesAverageSmoking: Double,
    val noAverageSmoking: Double,
    val text: String,
    val caveat: String = "Bu yalnızca kayıtların birlikte değişimini gösterir; neden-sonuç veya tıbbi değerlendirme değildir.",
)

data class HealthJournalSummary(
    val recordedDays7: Int,
    val recordedDays14: Int,
    val recordedDays30: Int,
    val associations: List<HealthAssociation>,
)

fun calculateHealthJournalSummary(
    entries: List<DailyHealthEntryEntity>,
    records: List<SmokingRecordEntity>,
    today: LocalDate,
): HealthJournalSummary {
    val smokingByDate = records.groupBy { it.localDate() }
        .mapValues { (_, values) -> values.sumOf(SmokingRecordEntity::quantity) }
    val recent = entries.filter {
        val date = runCatching { LocalDate.parse(it.entryDate) }.getOrNull()
        date != null && date in today.minusDays(29)..today
    }
    val commonDayCount = recent.count { smokingByDate.containsKey(LocalDate.parse(it.entryDate)) }
    val associations = if (commonDayCount >= 14) {
        listOfNotNull(
            scaleAssociation("Enerji", recent, smokingByDate) { energyLevel },
            scaleAssociation("Stres", recent, smokingByDate) { stressLevel },
            scaleAssociation("Uyku kalitesi", recent, smokingByDate) { sleepQuality },
            association("Sabah öksürüğü", recent, smokingByDate) { morningCough },
            association("Baş ağrısı", recent, smokingByDate) { headache },
            association("Nefes darlığı hissi", recent, smokingByDate) { shortnessOfBreath },
            association("Göğüs rahatsızlığı", recent, smokingByDate) { chestDiscomfort },
        )
    } else {
        emptyList()
    }
    return HealthJournalSummary(
        recordedDays7 = recent.count { LocalDate.parse(it.entryDate) >= today.minusDays(6) },
        recordedDays14 = recent.count { LocalDate.parse(it.entryDate) >= today.minusDays(13) },
        recordedDays30 = recent.size,
        associations = associations,
    )
}

private fun scaleAssociation(
    label: String,
    entries: List<DailyHealthEntryEntity>,
    smokingByDate: Map<LocalDate, Int>,
    value: DailyHealthEntryEntity.() -> Int?,
): HealthAssociation? {
    val paired = entries.mapNotNull { entry ->
        val smoking = smokingByDate[LocalDate.parse(entry.entryDate)] ?: return@mapNotNull null
        entry.value()?.let { it to smoking }
    }
    val high = paired.filter { it.first >= 4 }.map(Pair<Int, Int>::second)
    val low = paired.filter { it.first <= 2 }.map(Pair<Int, Int>::second)
    if (high.size < 5 || low.size < 5) return null
    val highAverage = high.average()
    val lowAverage = low.average()
    return HealthAssociation(
        label = label,
        yesDays = high.size,
        noDays = low.size,
        missingDays = entries.size - paired.size,
        yesAverageSmoking = highAverage,
        noAverageSmoking = lowAverage,
        text = "$label 4–5 seçilen ${high.size} günde ortalama ${oneDecimal(highAverage)}, " +
            "1–2 seçilen ${low.size} günde ${oneDecimal(lowAverage)} kayıt birlikte görülüyor.",
    )
}

private fun association(
    label: String,
    entries: List<DailyHealthEntryEntity>,
    smokingByDate: Map<LocalDate, Int>,
    symptom: DailyHealthEntryEntity.() -> Boolean?,
): HealthAssociation? {
    val paired = entries.mapNotNull { entry ->
        val count = smokingByDate[LocalDate.parse(entry.entryDate)] ?: return@mapNotNull null
        entry.symptom()?.let { it to count }
    }
    val yes = paired.filter(Pair<Boolean, Int>::first).map(Pair<Boolean, Int>::second)
    val no = paired.filterNot(Pair<Boolean, Int>::first).map(Pair<Boolean, Int>::second)
    if (yes.size < 5 || no.size < 5) return null
    val yesAverage = yes.average()
    val noAverage = no.average()
    return HealthAssociation(
        label = label,
        yesDays = yes.size,
        noDays = no.size,
        missingDays = entries.size - paired.size,
        yesAverageSmoking = yesAverage,
        noAverageSmoking = noAverage,
        text = "$label işaretlenen ${yes.size} günde ortalama ${oneDecimal(yesAverage)}, işaretlenmeyen ${no.size} günde ${oneDecimal(noAverage)} kayıt birlikte görülüyor.",
    )
}

private fun oneDecimal(value: Double): String =
    java.text.DecimalFormat("0.0", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US))
        .format(value)

private fun SmokingRecordEntity.localDate(): LocalDate =
    Instant.ofEpochMilli(smokedAtEpochMillis)
        .atZone(runCatching { ZoneId.of(zoneIdSnapshot) }.getOrDefault(ZoneId.systemDefault()))
        .toLocalDate()
