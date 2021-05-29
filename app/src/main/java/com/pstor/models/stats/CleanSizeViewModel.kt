package com.pstor.models.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.pstor.ImageStatus
import com.pstor.R
import com.pstor.Tagged
import com.pstor.db.PStorDatabase

class CleanSizeViewModel(application: Application) : AndroidViewModel(application), Tagged {
    val toBeRemovedSize: LiveData<Long>

    init {
        val toBeRemovedQueueDAO = PStorDatabase.getDatabase(application).toBeRemovedQueueDAO()
        toBeRemovedSize = toBeRemovedQueueDAO.obsSize().map { it ?: 0L }
    }
}