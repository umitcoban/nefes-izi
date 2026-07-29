package com.umityasincoban.nefesizi.core.domain

data class SmokingRecordDraft(
    val productId: String,
    val smokedAtEpochMillis: Long,
    val quantity: Int = 1,
    val consumedQuarter: Int = 4,
    val cravingLevel: Int? = null,
    val trigger: String? = null,
    val mood: String? = null,
    val locationType: String? = null,
    val note: String? = null,
)
