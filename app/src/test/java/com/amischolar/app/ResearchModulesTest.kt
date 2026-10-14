package com.amischolar.app

import com.amischolar.app.feature.ResearchModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchModulesTest {
    @Test fun moduleRoutesAreUnique() {
        val routes = ResearchModules.all.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test fun coreWorkflowAreasAreListed() {
        val titles = ResearchModules.all.map { it.title }
        assertTrue("Topic Lab" in titles)
        assertTrue("Evidence Matrix" in titles)
        assertTrue("Citation Center" in titles)
        assertTrue("Archive & Reproducibility" in titles)
    }

    @Test fun allModulesHaveContent() {
        assertTrue(ResearchModules.all.all {
            it.title.isNotBlank() && it.description.isNotBlank() && it.route.isNotBlank()
        })
    }
}
