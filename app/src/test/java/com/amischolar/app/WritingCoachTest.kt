package com.amischolar.app
import com.amischolar.app.feature.WritingCoach
import org.junit.Assert.*
import org.junit.Test
class WritingCoachTest {
 @Test fun flagsOverclaims(){assertTrue(WritingCoach.review("This always proves everything.").any{it.label=="Overclaiming"})}
 @Test fun emptyDraftIsNotInvented(){assertEquals("Empty draft",WritingCoach.review(" ").first().label)}
}
