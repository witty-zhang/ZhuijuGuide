package com.lzzhang.zhuijuguide

import com.lzzhang.zhuijuguide.data.AvailabilityRules
import com.lzzhang.zhuijuguide.model.Availability
import org.junit.Assert.assertEquals
import org.junit.Test

class AvailabilityRulesTest {
    @Test
    fun classifiesReachableResponsesAsAvailable() {
        assertEquals(Availability.AVAILABLE, AvailabilityRules.classify(200))
        assertEquals(Availability.AVAILABLE, AvailabilityRules.classify(206))
        assertEquals(Availability.AVAILABLE, AvailabilityRules.classify(301))
        assertEquals(Availability.AVAILABLE, AvailabilityRules.classify(302))
    }

    @Test
    fun classifiesBlockedResponsesAsRestricted() {
        assertEquals(Availability.RESTRICTED, AvailabilityRules.classify(401))
        assertEquals(Availability.RESTRICTED, AvailabilityRules.classify(403))
        assertEquals(Availability.RESTRICTED, AvailabilityRules.classify(429))
        assertEquals(Availability.RESTRICTED, AvailabilityRules.classify(451))
    }

    @Test
    fun classifiesClientAndServerErrorsAsOffline() {
        assertEquals(Availability.OFFLINE, AvailabilityRules.classify(404))
        assertEquals(Availability.OFFLINE, AvailabilityRules.classify(500))
        assertEquals(Availability.OFFLINE, AvailabilityRules.classify(503))
    }
}
