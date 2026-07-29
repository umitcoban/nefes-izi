package com.umityasincoban.nefesizi.core.domain

enum class SmokingRecordValidationError {
    PRODUCT_REQUIRED,
    INVALID_QUANTITY,
    INVALID_CONSUMED_RATIO,
    INVALID_CRAVING_LEVEL,
    FUTURE_DATE,
}

fun validateSmokingRecordDraft(
    draft: SmokingRecordDraft,
    nowEpochMillis: Long,
): SmokingRecordValidationError? = when {
    draft.productId.isBlank() -> SmokingRecordValidationError.PRODUCT_REQUIRED
    draft.quantity !in 1..99 -> SmokingRecordValidationError.INVALID_QUANTITY
    draft.consumedQuarter !in 1..4 -> SmokingRecordValidationError.INVALID_CONSUMED_RATIO
    draft.cravingLevel != null && draft.cravingLevel !in 1..5 ->
        SmokingRecordValidationError.INVALID_CRAVING_LEVEL
    draft.smokedAtEpochMillis > nowEpochMillis -> SmokingRecordValidationError.FUTURE_DATE
    else -> null
}

fun SmokingRecordValidationError.userMessage(): String = when (this) {
    SmokingRecordValidationError.PRODUCT_REQUIRED -> "Lütfen bir ürün seç."
    SmokingRecordValidationError.INVALID_QUANTITY -> "Adet 1–99 arasında olmalı."
    SmokingRecordValidationError.INVALID_CONSUMED_RATIO -> "İçilen oran geçersiz."
    SmokingRecordValidationError.INVALID_CRAVING_LEVEL -> "İstek seviyesi 1–5 arasında olmalı."
    SmokingRecordValidationError.FUTURE_DATE -> "Gelecek zamana kayıt eklenemez."
}
