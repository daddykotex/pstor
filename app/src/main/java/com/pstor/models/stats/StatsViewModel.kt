package com.pstor.models.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.pstor.ImageStatus
import com.pstor.Tagged
import com.pstor.db.PStorDatabase

class StatsViewModel(application: Application) : AndroidViewModel(application), Tagged {
    val allCount: LiveData<Long>
    val uploadedCount: LiveData<Long>
    val uploadedAndRemovedCount: LiveData<Long>
    val failedCount: LiveData<Long>

    init {
        val db = PStorDatabase.getDatabase(application).queueDAO()

        allCount = db.obsCount()
        uploadedCount = db.obsCountByStatus(ImageStatus.UPLOADED.toString())
        uploadedAndRemovedCount = db.obsCountByStatus(ImageStatus.UPLOADED_AND_REMOVED.toString())
        failedCount = db.obsCountByStatus(ImageStatus.FAILED_TO_PROCESS.toString())
    }
}