package com.soundcloud.android.view

import android.util.LongSparseArray
import com.soundcloud.android.utils.OpenForTesting

import javax.inject.Inject
import javax.inject.Singleton

@OpenForTesting
@Singleton
class PresenterManager
@Inject
constructor()
{

    private val presenters = LongSparseArray<Destroyable>()
    private var runningId: Long = 0

    fun save(presenter: Destroyable): Long {
        presenters.put(runningId, presenter)
        return runningId++
    }

    fun <T : Destroyable> get(id: Long): T? {
        return presenters.get(id) as T?
    }

    fun remove(id: Long) {
        presenters.remove(id)
    }

}
