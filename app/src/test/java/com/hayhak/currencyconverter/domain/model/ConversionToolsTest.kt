package com.hayhak.currencyconverter.domain.model

import org.junit.Assert.*
import org.junit.Test

class ConversionToolsTest {
    @Test fun percentageAndFixedFeeAreDeductedInDestinationCurrency() {
        assertEquals(970.0, netAfterFees(1000.0, "2", "10")!!, 0.000001)
    }
    @Test fun emptyFeesMeanZeroAndCommaDecimalIsAccepted() {
        assertEquals(100.0, netAfterFees(100.0, "", "")!!, 0.0)
        assertEquals(98.5, netAfterFees(100.0, "1,5", "")!!, 0.000001)
    }
    @Test fun invalidFeesAndNonFiniteAmountsAreRejected() {
        assertNull(netAfterFees(Double.NaN, "0", "0"))
        assertNull(netAfterFees(Double.POSITIVE_INFINITY, "0", "0"))
        assertNull(netAfterFees(-1.0, "0", "0"))
        assertNull(netAfterFees(100.0, "101", "0"))
        assertNull(netAfterFees(100.0, "-1", "0"))
        assertNull(netAfterFees(100.0, "0", "101"))
        assertNull(netAfterFees(100.0, "0", "-1"))
        assertNull(netAfterFees(100.0, "abc", "0"))
    }
    @Test fun fullFeeCanProduceZero() {
        assertEquals(0.0, netAfterFees(100.0, "100", "0")!!, 0.0)
    }
    @Test fun gramsUseTroyNotOrdinaryOunces() {
        assertEquals(100.0, 3110.34768 / GRAMS_PER_TROY_OUNCE, 0.000001)
    }
}
