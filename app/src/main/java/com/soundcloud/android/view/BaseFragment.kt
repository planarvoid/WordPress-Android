package com.soundcloud.android.view

import android.os.Bundle
import android.view.View
import com.soundcloud.android.utils.LightCycleLogger
import com.soundcloud.lightcycle.LightCycle
import com.soundcloud.lightcycle.LightCycleSupportFragment
import javax.inject.Inject

abstract class BaseFragment<T : Destroyable> : LightCycleSupportFragment<BaseFragment<T>> {

    internal var presenter: T? = null
    private var presenterId: Long = 0

    @Inject lateinit var presenterManager: PresenterManager
    @LightCycle internal var logger = LightCycleLogger.forSupportFragment(javaClass.name)

    constructor() : super()

    internal constructor(presenterManager: PresenterManager) : super() {
        this.presenterManager = presenterManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            presenter = createPresenter()
            presenterId = presenterManager.save(presenter as T)
        } else {
            presenterId = savedInstanceState.getLong(Companion.PRESENTER_KEY)
            presenter = presenterManager.get<T>(presenterId)
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
        if (!activity.isChangingConfigurations) {
            presenterManager.remove(presenterId)
            presenter!!.destroy()
        }
        super.onDestroy()
    }

    protected abstract fun disconnectPresenter(presenter: T)

    protected abstract fun connectPresenter(presenter: T)

    protected abstract fun createPresenter(): T

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(Companion.PRESENTER_KEY, presenterId)
        super.onSaveInstanceState(outState)
    }

    companion object {
        val PRESENTER_KEY = "presenter_key"
    }
}
