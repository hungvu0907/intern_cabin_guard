package com.example.cabinguard

import com.example.cabinguard.widget.WidgetUpdatePolicy
import org.junit.Assert.*
import org.junit.Test

class WidgetPolicyTest {
    @Test fun throttlesOrdinaryMeasurementsButDetectsWarningTransitions() {
        val policy = WidgetUpdatePolicy()
        assertTrue(policy.isDue(0))
        policy.updated(0)
        assertFalse(policy.isDue(29999))
        assertTrue(policy.isDue(30000))
        assertTrue(policy.warningChanged(false))
        policy.updated(30_000)
        assertFalse(policy.isDue(30_001))
        policy.failed()
        assertTrue(policy.isDue(30_001))
        assertFalse(policy.warningChanged(false))
        assertTrue(policy.warningChanged(true))
        assertFalse(policy.warningChanged(true))
        assertTrue(policy.warningChanged(false))
    }
}
