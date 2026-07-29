package com.umityasincoban.nefesizi.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmokingRecordValidatorTest {
    private val now = 10_000L

    @Test
    fun `valid boundary values are accepted`() {
        assertNull(validateSmokingRecordDraft(draft(quantity = 1, quarter = 1, craving = 1), now))
        assertNull(validateSmokingRecordDraft(draft(quantity = 99, quarter = 4, craving = 5), now))
        assertNull(validateSmokingRecordDraft(draft(craving = null, at = now), now))
    }

    @Test
    fun `blank product is rejected`() {
        assertEquals(
            SmokingRecordValidationError.PRODUCT_REQUIRED,
            validateSmokingRecordDraft(draft(productId = " "), now),
        )
    }

    @Test
    fun `quantity outside one to ninety nine is rejected`() {
        assertEquals(
            SmokingRecordValidationError.INVALID_QUANTITY,
            validateSmokingRecordDraft(draft(quantity = 0), now),
        )
        assertEquals(
            SmokingRecordValidationError.INVALID_QUANTITY,
            validateSmokingRecordDraft(draft(quantity = 100), now),
        )
    }

    @Test
    fun `consumed quarter outside supported ratios is rejected`() {
        assertEquals(
            SmokingRecordValidationError.INVALID_CONSUMED_RATIO,
            validateSmokingRecordDraft(draft(quarter = 0), now),
        )
        assertEquals(
            SmokingRecordValidationError.INVALID_CONSUMED_RATIO,
            validateSmokingRecordDraft(draft(quarter = 5), now),
        )
    }

    @Test
    fun `craving outside one to five is rejected while null is allowed`() {
        assertEquals(
            SmokingRecordValidationError.INVALID_CRAVING_LEVEL,
            validateSmokingRecordDraft(draft(craving = 0), now),
        )
        assertEquals(
            SmokingRecordValidationError.INVALID_CRAVING_LEVEL,
            validateSmokingRecordDraft(draft(craving = 6), now),
        )
        assertNull(validateSmokingRecordDraft(draft(craving = null), now))
    }

    @Test
    fun `future timestamp is rejected but exact now is accepted`() {
        assertEquals(
            SmokingRecordValidationError.FUTURE_DATE,
            validateSmokingRecordDraft(draft(at = now + 1), now),
        )
        assertNull(validateSmokingRecordDraft(draft(at = now), now))
    }

    private fun draft(
        productId: String = "product",
        quantity: Int = 1,
        quarter: Int = 4,
        craving: Int? = null,
        at: Long = now,
    ) = SmokingRecordDraft(
        productId = productId,
        smokedAtEpochMillis = at,
        quantity = quantity,
        consumedQuarter = quarter,
        cravingLevel = craving,
    )
}
