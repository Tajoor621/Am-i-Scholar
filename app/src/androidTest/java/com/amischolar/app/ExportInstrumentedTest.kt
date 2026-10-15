package com.amischolar.app

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amischolar.app.feature.DocumentExporter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExportInstrumentedTest {
    @Test fun pdfOpensAndContainsMultiplePagesForLongDocument() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bytes = DocumentExporter.pdf((1..250).joinToString("\n") { "Research paragraph $it — citation and evidence" })
        assertTrue(bytes.size > 1000)
        val file = File(context.cacheDir, "export-check.pdf")
        try {
            file.writeBytes(bytes)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    assertTrue(renderer.pageCount >= 2)
                    renderer.openPage(0).use { page ->
                        assertTrue(page.width > 0)
                        assertTrue(page.height > 0)
                    }
                }
            }
        } finally { file.delete() }
    }

    @Test fun appLaunchesWithoutPaidAccountOrNetwork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent)
        try { assertNotNull(activity) }
        finally { activity.finish() }
    }
}
