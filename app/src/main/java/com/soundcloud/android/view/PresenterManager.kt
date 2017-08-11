package com.soundcloud.android.view

import android.util.LongSparseArray

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class PresenterManager @Inject
constructor()
{

    private val presenters = LongSparseArray<BasePresenter>()
    private var runningId: Long = 0

    open fun save(presenter: BasePresenter): Long {
        presenters.put(runningId, presenter)
        return runningId++
    }

    open fun <T : BasePresenter> get(id: Long): T? {
        return presenters.get(id) as T?
    }

    open fun remove(id: Long) {
        presenters.remove(id)
    }

}
