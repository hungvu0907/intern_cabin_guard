package com.example.cabinguard

import com.example.cabinguard.service.CabinNotification
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationStatusTest {
    @Test fun waitingForTheFirstMeasurementIsDifferentFromSafe() {
        assertEquals("CHỜ DỮ LIỆU", CabinNotification.statusLabel(null))
        assertEquals("AN TOÀN", CabinNotification.statusLabel(false))
        assertEquals("CẢNH BÁO", CabinNotification.statusLabel(true))
    }
}
