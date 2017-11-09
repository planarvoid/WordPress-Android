package com.soundcloud.android.view

import android.os.Bundle
import android.util.Log
import android.view.View
import com.soundcloud.android.utils.ErrorUtils
import com.soundcloud.android.utils.LightCycleLogger
import com.soundcloud.lightcycle.LightCycle
import com.soundcloud.lightcycle.LightCycleSupportFragment
import javax.inject.Inject

abstract class BaseFragment<T : Destroyable> : LightCycleSupportFragment<BaseFragment<T>> {

    internal var presenter: T? = null
    private var presenterId: Long = 0

    protected abstract val presenterKey: String

    @Inject lateinit var presenterManager: PresenterManager
    @LightCycle internal var logger = LightCycleLogger.forSupportFragment(javaClass.name)

    constructor() : super()

    internal constructor(presenterManager: PresenterManager) : super() {
        this.presenterManager = presenterManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            initializePresenter()
            Log.d("vojta", "Saved state is null: $presenterKey, PresenterId: $presenterId, Presenter: ${presenter.toString()}")
        } else {
            presenterId = savedInstanceState.getLong(presenterKey)
            presenter = presenterManager.get(presenterId)
            Log.d("vojta", "Presenter key: $presenterKey, PresenterId: $presenterId, Presenter: ${presenter.toString()}")
            if (presenter == null) {
                ErrorUtils.log(Log.INFO, "com.soundcloud.android.view.BaseFragment.onCreate", "Reinitializing empty presenter")
                initializePresenter()
            }
        }
    }

    private fun initializePresenter() {
        presenter = createPresenter()
        presenter?.let {
            presenterId = presenterManager.save(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter?.let {
            connectPresenter(it)
        }
    }

    override fun onDestroyView() {
        presenter?.let {
            disconnectPresenter(it)
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (!activity.isChangingConfigurations && activity.isFinishing) {
            presenterManager.remove(presenterId)
            presenter?.destroy()
        }
        super.onDestroy()
    }

    protected abstract fun disconnectPresenter(presenter: T)

    protected abstract fun connectPresenter(presenter: T)

    protected abstract fun createPresenter(): T

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(presenterKey, presenterId)
        super.onSaveInstanceState(outState)
    }
}
