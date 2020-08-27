package com.pstor.models.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.pstor.ImageStatus
import com.pstor.db.PStorDatabase

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    val allCount: LiveData<Long>
    val succeedCount: LiveData<Long>
    val failedCount: LiveData<Long>

    init {
        val db = PStorDatabase.getDatabase(application).queueDAO()

        allCount = db.obsCount()
        succeedCount = db.obsCountByStatus(ImageStatus.UPLOADED.toString())
        failedCount = db.obsCountByStatus(ImageStatus.FAILED_TO_PROCESS.toString())
    }
}