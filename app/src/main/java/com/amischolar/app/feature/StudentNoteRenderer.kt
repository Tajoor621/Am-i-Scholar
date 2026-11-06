package com.amischolar.app.feature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val MAX_NOTE_IMAGE_BYTES = 320_000
private const val MAX_NOTE_IMAGE_EDGE = 1400
private val NOTE_CODE_FENCE = String(CharArray(3) { 96.toChar() })

fun encodeStudentNoteImage(context: Context, uri: Uri): String {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = context.contentResolver.openInputStream(uri) ?: error("Could not open the selected image.")
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected image could not be read." }
    var sample = 1
    while (bounds.outWidth / sample > MAX_NOTE_IMAGE_EDGE * 2 || bounds.outHeight / sample > MAX_NOTE_IMAGE_EDGE * 2) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("Could not decode the selected image.")
    val scale = minOf(1f, MAX_NOTE_IMAGE_EDGE.toFloat() / maxOf(decoded.width, decoded.height))
    val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true) else decoded
    if (bitmap !== decoded) decoded.recycle()
    try {
        for (quality in listOf(72, 60, 50)) {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            if (output.size() <= MAX_NOTE_IMAGE_BYTES) return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
        error("Choose a smaller image so it fits in a portable note.")
    } finally { bitmap.recycle() }
}

@Composable
fun StudentNotePreview(content: String) {
    val lines = content.lines()
    Column {
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            if (line.trim().startsWith(NOTE_CODE_FENCE)) {
                val code = mutableListOf<String>()
                index++
                while (index < lines.size && !lines[index].trim().startsWith(NOTE_CODE_FENCE)) { code += lines[index]; index++ }
                if (index < lines.size) index++
                Text(code.joinToString("\n"), modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(10.dp), fontFamily = FontFamily.Monospace)
                continue
            }
            val image = Regex("^!\\[([^\\]]*)\\]\\(data:image/jpeg;base64,([A-Za-z0-9+/=]+)\\)$").matchEntire(line)
            if (image != null) {
                NoteImage(image.groupValues[1], image.groupValues[2])
            } else if (line.isBlank()) {
                Spacer(Modifier.height(4.dp))
            } else if (line.matches(Regex("^\\s*[-*]\\s+.*"))) {
                Text("• " + markdownInline(line.trim().replaceFirst(Regex("^[-*]\\s+"), "")))
            } else if (line.matches(Regex("^\\s*\\d+[.)]\\s+.*"))) {
                Text(markdownInline(line.trim()))
            } else {
                val heading = Regex("^(#{1,3})\\s+(.*)$").matchEntire(line)
                if (heading != null) Text(markdownInline(heading.groupValues[2]), style = MaterialTheme.typography.titleMedium)
                else Text(markdownInline(line))
            }
            index++
        }
    }
}

@Composable
private fun NoteImage(alt: String, encoded: String) {
    var bitmap by remember(encoded) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(encoded) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { val bytes = Base64.decode(encoded, Base64.DEFAULT); BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        }
    }
    val loadedBitmap = bitmap\n    if (loadedBitmap != null) Image(loadedBitmap.asImageBitmap(), contentDescription = alt.ifBlank { "Note image" }, modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp), contentScale = ContentScale.Fit)
}

@Composable
private fun markdownInline(text: String): AnnotatedString {
    val pattern = Regex("(\\*\\*[^*]+\\*\\*|==[^=]+==)")
    val highlightColor = MaterialTheme.colorScheme.tertiaryContainer
    return buildAnnotatedString {
        var cursor = 0
        for (match in pattern.findAll(text)) {
            append(text.substring(cursor, match.range.first))
            val token = match.value
            if (token.startsWith("**")) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(token.substring(2, token.length - 2)) }
            else withStyle(SpanStyle(background = highlightColor)) { append(token.substring(2, token.length - 2)) }
            cursor = match.range.last + 1
        }
        append(text.substring(cursor))
    }
}
