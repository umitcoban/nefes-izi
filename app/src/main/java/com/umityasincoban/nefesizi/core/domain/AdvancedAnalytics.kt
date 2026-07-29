package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class AnalyticsDay(val date: LocalDate, val count: Int)

data class RankedMetric(
    val label: String,
    val count: Int,
    val knownCount: Int,
    val unknownCount: Int,
)

data class CurrencyAmount(
    val currencyCode: String,
    val micros: Long,
    val knownCount: Int,
)

data class PeriodComparison(
    val currentCount: Int,
    val previousCount: Int,
    val changePercent: Double?,
)

enum class AnalyticsInsightType {
    PERIOD_CHANGE,
    PEAK_HOUR,
    COMMON_TRIGGER,
    DATA_COVERAGE,
}

data class AnalyticsInsight(
    val type: AnalyticsInsightType,
    val minimumDataSatisfied: Boolean,
    val numerator: Int,
    val denominator: Int,
    val text: String,
    val caveat: String,
    val priority: Int,
)

data class AdvancedAnalytics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dayCount: Int,
    val recordedDayCount: Int,
    val totalCount: Int,
    val dailyAverage: Double,
    val daily: List<AnalyticsDay>,
    val highestDay: AnalyticsDay?,
    val lowestDay: AnalyticsDay?,
    val averageIntervalMinutes: Long?,
    val longestGapMinutes: Long?,
    val averageFirstRecordAfterWakeMinutes: Long?,
    val mostCommonProduct: RankedMetric?,
    val mostCommonTrigger: RankedMetric?,
    val mostCommonMood: RankedMetric?,
    val hourlyCounts: List<Int>,
    val weekdayCounts: Map<DayOfWeek, Int>,
    val nicotine: ExposureTotal,
    val tar: ExposureTotal,
    val carbonMonoxide: ExposureTotal,
    val costs: List<CurrencyAmount>,
    val unknownCostCount: Int,
    val comparison: PeriodComparison?,
    val annualCostProjection: List<CurrencyAmount>,
    val annualProjectionEligible: Boolean,
    val insights: List<AnalyticsInsight>,
)

fun calculateAdvancedAnalytics(
    allRecords: List<SmokingRecordEntity>,
    requestedStart: LocalDate?,
    requestedEnd: LocalDate,
    wakeTime: LocalTime? = null,
): AdvancedAnalytics {
    val dated = allRecords.map { it to it.localEventDate() }
    val start = requestedStart
        ?: dated.minOfOrNull { it.second }
        ?: requestedEnd
    val end = requestedEnd.coerceAtLeast(start)
    val records = dated.filter { (_, date) -> date in start..end }.map(Pair<SmokingRecordEntity, LocalDate>::first)
    val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
    val countsByDate = records.groupBy(SmokingRecordEntity::localEventDate)
        .mapValues { (_, values) -> values.sumOf(SmokingRecordEntity::quantity) }
    val daily = (0 until days).map { offset ->
        val date = start.plusDays(offset.toLong())
        AnalyticsDay(date, countsByDate[date] ?: 0)
    }
    val total = records.sumOf(SmokingRecordEntity::quantity)
    val chronological = records.sortedBy(SmokingRecordEntity::smokedAtEpochMillis)
    val intervals = chronological.zipWithNext { first, second ->
        Duration.ofMillis(
            (second.smokedAtEpochMillis - first.smokedAtEpochMillis).coerceAtLeast(0L),
        ).toMinutes()
    }
    val firstAfterWake = wakeTime?.let { wake ->
        records.groupBy(SmokingRecordEntity::localEventDate)
            .mapNotNull { (date, dayRecords) ->
                val first = dayRecords.minByOrNull(SmokingRecordEntity::smokedAtEpochMillis)
                    ?.localEventDateTime()
                    ?.toLocalDateTime()
                    ?: return@mapNotNull null
                ChronoUnit.MINUTES.between(date.atTime(wake), first).takeIf { it >= 0 }
            }
            .takeIf(List<Long>::isNotEmpty)
            ?.average()
            ?.toLong()
    }
    val periodDays = days.toLong()
    val previousStart = start.minusDays(periodDays)
    val previousEnd = start.minusDays(1)
    val previousRecords = dated.filter { (_, date) -> date in previousStart..previousEnd }
        .map(Pair<SmokingRecordEntity, LocalDate>::first)
    val previousTotal = previousRecords.sumOf(SmokingRecordEntity::quantity)
    val comparison = requestedStart?.let {
        PeriodComparison(
            currentCount = total,
            previousCount = previousTotal,
            changePercent = if (previousTotal == 0) {
                null
            } else {
                (total - previousTotal) * 100.0 / previousTotal
            },
        )
    }
    val hourly = MutableList(24) { 0 }
    val weekdays = DayOfWeek.entries.associateWith { 0 }.toMutableMap()
    records.forEach { record ->
        val local = record.localEventDateTime()
        hourly[local.hour] += record.quantity
        weekdays[local.dayOfWeek] = checkNotNull(weekdays[local.dayOfWeek]) + record.quantity
    }
    val nicotine = calculateExposure(records) { it.nicotineMicrogramsPerCigaretteSnapshot }
    val tar = calculateExposure(records) { it.tarMicrogramsPerCigaretteSnapshot }
    val carbonMonoxide = calculateExposure(records) {
        it.carbonMonoxideMicrogramsPerCigaretteSnapshot
    }
    val (costs, unknownCost) = records.costTotals()
    val recentStart = end.minusDays(29)
    val recentRecords = dated.filter { (_, date) -> date in recentStart..end }
        .map(Pair<SmokingRecordEntity, LocalDate>::first)
    val recentRecordedDays = recentRecords.map(SmokingRecordEntity::localEventDate).distinct().size
    val annualEligible = recentRecordedDays >= 7
    val annual = if (annualEligible) {
        recentRecords.costTotals().first.map { amount ->
            amount.copy(micros = (amount.micros / 30.0 * 365.0).toLong())
        }
    } else {
        emptyList()
    }
    val product = records.rankedMetric(
        value = SmokingRecordEntity::productNameSnapshot,
        isKnown = { productId != null },
    )
    val trigger = records.rankedMetric(
        value = { trigger },
        isKnown = { !trigger.isNullOrBlank() },
    )
    val mood = records.rankedMetric(
        value = { mood },
        isKnown = { !mood.isNullOrBlank() },
    )
    val insights = buildInsights(
        dayCount = days,
        total = total,
        comparison = comparison,
        hourly = hourly,
        trigger = trigger,
        exposureKnown = nicotine.knownCount,
        exposureUnknown = nicotine.unknownCount,
    )
    return AdvancedAnalytics(
        startDate = start,
        endDate = end,
        dayCount = days,
        recordedDayCount = countsByDate.count { it.value > 0 },
        totalCount = total,
        dailyAverage = total.toDouble() / days,
        daily = daily,
        highestDay = daily.maxByOrNull(AnalyticsDay::count),
        lowestDay = daily.minByOrNull(AnalyticsDay::count),
        averageIntervalMinutes = intervals.takeIf(List<Long>::isNotEmpty)?.average()?.toLong(),
        longestGapMinutes = intervals.maxOrNull(),
        averageFirstRecordAfterWakeMinutes = firstAfterWake,
        mostCommonProduct = product,
        mostCommonTrigger = trigger,
        mostCommonMood = mood,
        hourlyCounts = hourly,
        weekdayCounts = weekdays,
        nicotine = nicotine,
        tar = tar,
        carbonMonoxide = carbonMonoxide,
        costs = costs,
        unknownCostCount = unknownCost,
        comparison = comparison,
        annualCostProjection = annual,
        annualProjectionEligible = annualEligible,
        insights = insights,
    )
}

private fun List<SmokingRecordEntity>.rankedMetric(
    value: SmokingRecordEntity.() -> String?,
    isKnown: SmokingRecordEntity.() -> Boolean,
): RankedMetric? {
    val known = filter(isKnown)
    val unknownCount = filterNot(isKnown).sumOf(SmokingRecordEntity::quantity)
    val winner = known.groupBy { it.value().orEmpty() }
        .mapValues { (_, records) -> records.sumOf(SmokingRecordEntity::quantity) }
        .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key })
        ?: return null
    return RankedMetric(
        label = winner.key,
        count = winner.value,
        knownCount = known.sumOf(SmokingRecordEntity::quantity),
        unknownCount = unknownCount,
    )
}

private fun List<SmokingRecordEntity>.costTotals(): Pair<List<CurrencyAmount>, Int> {
    var unknown = 0
    val known = mutableMapOf<String, Pair<Long, Int>>()
    forEach { record ->
        val price = record.priceMicrosPerCigaretteSnapshot
        if (price == null) {
            unknown += record.quantity
        } else {
            val amount = price * record.quantity * record.consumedQuarter / 4
            val previous = known[record.currencyCodeSnapshot] ?: (0L to 0)
            known[record.currencyCodeSnapshot] =
                (previous.first + amount) to (previous.second + record.quantity)
        }
    }
    return known.map { (currency, value) ->
        CurrencyAmount(currency, value.first, value.second)
    }.sortedBy(CurrencyAmount::currencyCode) to unknown
}

private fun buildInsights(
    dayCount: Int,
    total: Int,
    comparison: PeriodComparison?,
    hourly: List<Int>,
    trigger: RankedMetric?,
    exposureKnown: Int,
    exposureUnknown: Int,
): List<AnalyticsInsight> {
    val insights = mutableListOf<AnalyticsInsight>()
    comparison?.changePercent?.takeIf { dayCount >= 7 }?.let { change ->
        val comparisonText = when {
            change > 0 -> "%${kotlin.math.abs(change).toInt()} daha fazla"
            change < 0 -> "%${kotlin.math.abs(change).toInt()} daha az"
            else -> "aynı sayıda"
        }
        insights += AnalyticsInsight(
            type = AnalyticsInsightType.PERIOD_CHANGE,
            minimumDataSatisfied = true,
            numerator = comparison.currentCount,
            denominator = comparison.previousCount,
            text = "Bu dönemde önceki eş döneme göre $comparisonText kayıt var.",
            caveat = "Karşılaştırma yalnızca kaydettiğin gün ve adetleri özetler.",
            priority = 100,
        )
    }
    val peakHour = hourly.indices.maxByOrNull { hourly[it] }
    if (peakHour != null && total >= 5 && hourly[peakHour] > 0) {
        insights += AnalyticsInsight(
            type = AnalyticsInsightType.PEAK_HOUR,
            minimumDataSatisfied = true,
            numerator = hourly[peakHour],
            denominator = total,
            text = "Kayıtların en sık ${"%02d:00".format(peakHour)}–${"%02d:00".format((peakHour + 1) % 24)} aralığında.",
            caveat = "Saatler her kaydın olay anındaki yerel saat dilimine göredir.",
            priority = 80,
        )
    }
    if (trigger != null && trigger.knownCount >= 5) {
        val percentage = trigger.count * 100 / trigger.knownCount
        insights += AnalyticsInsight(
            type = AnalyticsInsightType.COMMON_TRIGGER,
            minimumDataSatisfied = true,
            numerator = trigger.count,
            denominator = trigger.knownCount,
            text = "Tetikleyicisi girilen kayıtların %$percentage kadarında “${trigger.label}” seçilmiş.",
            caveat = "${trigger.unknownCount} adet kayıtta tetikleyici belirtilmemiştir.",
            priority = 70,
        )
    }
    if (exposureUnknown > 0) {
        insights += AnalyticsInsight(
            type = AnalyticsInsightType.DATA_COVERAGE,
            minimumDataSatisfied = exposureKnown > 0,
            numerator = exposureKnown,
            denominator = exposureKnown + exposureUnknown,
            text = "Tahmini emisyon toplamı $exposureKnown adet kaydı kapsıyor.",
            caveat = "$exposureUnknown adet kayıtta ürün değerleri eksik; eksikler sıfır kabul edilmez.",
            priority = 60,
        )
    }
    return insights.sortedByDescending(AnalyticsInsight::priority)
}

private fun SmokingRecordEntity.localEventDateTime() =
    Instant.ofEpochMilli(smokedAtEpochMillis).atZone(
        runCatching { ZoneId.of(zoneIdSnapshot) }.getOrDefault(ZoneId.systemDefault()),
    )

private fun SmokingRecordEntity.localEventDate(): LocalDate = localEventDateTime().toLocalDate()
