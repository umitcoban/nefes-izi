package com.umityasincoban.nefesizi.core.data

data class ProductIdentityDraft(
    val name: String,
    val brand: String?,
    val variant: String?,
)

data class ProductRevisionDraft(
    val effectiveFromEpochMillis: Long,
    val nicotineMicrogramsPerCigarette: Long?,
    val tarMicrogramsPerCigarette: Long?,
    val carbonMonoxideMicrogramsPerCigarette: Long?,
    val packPriceMicros: Long?,
    val cigarettesPerPack: Int?,
    val priceMicrosPerCigarette: Long?,
    val currencyCode: String,
    val valueSource: String = "USER_ENTERED",
)
