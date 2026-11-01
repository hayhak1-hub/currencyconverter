package com.hayhak.currencyconverter.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.hayhak.currencyconverter.domain.model.HistoricalRate
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    /**
     * Geçmiş kur verisini Downloads klasörüne CSV olarak yazar.
     * @return Kullanıcıya gösterilecek dosya yolu veya görünen ad; başarısızsa null.
     */
    fun exportHistoricalRates(
        context: Context,
        baseCurrency: String,
        targetCurrency: String,
        data: List<HistoricalRate>
    ): String? {
        if (data.isEmpty()) return null

        val fileName = "Kur_${baseCurrency}_${targetCurrency}_${System.currentTimeMillis()}.csv"
        val csv = buildCsv(baseCurrency, targetCurrency, data, context.getString(com.hayhak.currencyconverter.R.string.csv_header))

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeViaMediaStore(context, fileName, csv)
            } else {
                writeViaLegacyDownloads(fileName, csv)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildCsv(
        baseCurrency: String,
        targetCurrency: String,
        data: List<HistoricalRate>,
        header: String
    ): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append(header).append('\n')
        data.forEach { rate ->
            sb.append("${sdf.format(Date(rate.date))},${rate.rate},$baseCurrency,$targetCurrency\n")
        }
        return sb.toString()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun writeViaMediaStore(context: Context, fileName: String, csv: String): String? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null

        resolver.openOutputStream(uri)?.use { output ->
            output.write(csv.toByteArray(Charsets.UTF_8))
        } ?: return null

        return "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
    }

    @Suppress("DEPRECATION")
    private fun writeViaLegacyDownloads(fileName: String, csv: String): String? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = File(downloadsDir, fileName)
        file.writeText(csv, Charsets.UTF_8)
        return file.absolutePath
    }
}
