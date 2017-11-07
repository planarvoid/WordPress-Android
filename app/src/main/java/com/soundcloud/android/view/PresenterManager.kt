package com.soundcloud.android.view

import com.soundcloud.android.utils.OpenForTesting
import javax.inject.Inject
import javax.inject.Singleton

@OpenForTesting
@Singleton
class PresenterManager
@Inject
constructor() {

    private val presenters = mutableMapOf<Long, Destroyable>()
    //Offset to differentiate between different instances of PresenterManager as ID in saved instance outlives this singleton
    private var runningId: Long = System.currentTimeMillis()

    fun save(presenter: Destroyable): Long {
        presenters.put(runningId, presenter)
        return runningId++
    }

    //Default cast implementation is unsafe
    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    fun <T : Destroyable> get(id: Long): T? = presenters[id] as T?

    fun remove(id: Long) {
        presenters.remove(id)
    }

}
