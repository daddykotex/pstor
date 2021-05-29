package com.pstor.models.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.pstor.ImageStatus
import com.pstor.R
import com.pstor.Tagged
import com.pstor.db.PStorDatabase
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

class StatsViewModel(application: Application) : AndroidViewModel(application), Tagged {
    val stats: Map<String, LiveData<String>>

    init {
        val queueDAO = PStorDatabase.getDatabase(application).queueDAO()

        val allCount = queueDAO.obsCount()
        val uploadedCount = queueDAO.obsCountByStatus(ImageStatus.UPLOADED.toString())
        val uploadedAndRemovedCount = queueDAO.obsCountByStatus(ImageStatus.UPLOADED_AND_REMOVED.toString())
        val failedCount = queueDAO.obsCountByStatus(ImageStatus.FAILED_TO_PROCESS.toString())
//
        val toBeRemovedQueueDAO = PStorDatabase.getDatabase(application).toBeRemovedQueueDAO()
        val sizeRemove = toBeRemovedQueueDAO.obsSize()



        stats = mapOf(
            application.getString(R.string.settings_app_stats_count_scanned) to allCount.map { it.toString() },
            application.getString(R.string.settings_app_stats_count_uploaded) to uploadedCount.map { it.toString() },
            application.getString(R.string.settings_app_stats_count_uploaded_and_removed) to uploadedAndRemovedCount.map { it.toString() },
            application.getString(R.string.settings_app_stats_count_error) to failedCount.map { it.toString() },
            application.getString(R.string.settings_app_stats_count_toremove_size) to sizeRemove.map { readableFileSize(it ?: 0L) },
        )
    }

    //https://stackoverflow.com/a/5599842/666562
    fun readableFileSize(size: Long): String {
        return if (size <= 0)  {
            "0"
        } else {
            val units = arrayOf("B", "kB", "MB", "GB", "TB")
            val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
            DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())).toString() + " " + units[digitGroups]
        }
    }
}