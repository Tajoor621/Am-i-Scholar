package com.amischolar.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineCslAssetsInstrumentedTest {
    @Test fun bundledProcessorStylesAndLocaleArePresent() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val names = listOf("index.html", "citeproc.js", "locales-en-US.xml",
            "styles/apa.csl", "styles/modern-language-association.csl",
            "styles/chicago-author-date.csl", "styles/chicago-notes-bibliography-17th-edition.csl",
            "styles/ieee.csl", "styles/harvard-cite-them-right.csl", "styles/elsevier-vancouver.csl")
        for (name in names) {
            val bytes = assets.open("csl/" + name).use { it.readBytes() }
            assertTrue("$name missing or empty", bytes.isNotEmpty())
        }
    }
}
