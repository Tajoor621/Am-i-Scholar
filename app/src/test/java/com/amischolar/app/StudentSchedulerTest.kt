package com.amischolar.app

import com.amischolar.app.data.StudentCard
import com.amischolar.app.feature.StudentScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentSchedulerTest {
    @Test fun goodReviewsIncreaseIntervalsAndAgainSchedulesShortRetry() {
        val now = 1_800_000_000_000L
        val card = StudentCard(deckId = 1, front = "Question", back = "Answer")
        val first = StudentScheduler.review(card, "good", now)
        assertEquals(1, first.repetitions)
        assertEquals(1.0, first.intervalDays, 0.001)
        val second = StudentScheduler.review(first, "good", now)
        assertEquals(2, second.repetitions)
        assertEquals(6.0, second.intervalDays, 0.001)
        val retry = StudentScheduler.review(second, "again", now)
        assertEquals(0, retry.repetitions)
        assertTrue(retry.dueAtEpochMillis - now in 599_000..601_000)
    }
}
