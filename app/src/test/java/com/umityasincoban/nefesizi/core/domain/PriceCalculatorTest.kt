package com.umityasincoban.nefesizi.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PriceCalculatorTest {
    @Test
    fun `pack price is divided into micros per cigarette`() {
        val result = calculatePricePerCigaretteMicros(
            packPriceMicros = 100_000_000,
            cigarettesPerPack = 20,
        )

        assertEquals(5_000_000L, result)
    }

    @Test
    fun `fractional micros are rounded half up`() {
        val result = calculatePricePerCigaretteMicros(
            packPriceMicros = 10,
            cigarettesPerPack = 4,
        )

        assertEquals(3L, result)
    }

    @Test
    fun `missing price information stays unknown`() {
        assertNull(calculatePricePerCigaretteMicros(null, 20))
        assertNull(calculatePricePerCigaretteMicros(100_000_000, null))
    }

    @Test
    fun `negative price is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculatePricePerCigaretteMicros(-1, 20)
        }
    }

    @Test
    fun `non positive pack size is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculatePricePerCigaretteMicros(100_000_000, 0)
        }
    }
}
