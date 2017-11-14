package com.soundcloud.android.view

import android.os.Bundle
import android.util.Log
import android.view.View
import com.soundcloud.android.main.MainActivity
import com.soundcloud.android.main.Screen
import com.soundcloud.android.utils.CurrentDateProvider
import com.soundcloud.android.utils.ErrorUtils
import com.soundcloud.android.utils.LightCycleLogger
import com.soundcloud.java.optional.Optional
import com.soundcloud.lightcycle.LightCycle
import com.soundcloud.lightcycle.LightCycleSupportFragment
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

abstract class BaseFragment<T : Destroyable> : LightCycleSupportFragment<BaseFragment<T>> {

    internal var presenter: T? = null
    private var presenterId: Long = 0

    protected abstract val presenterKey: String

    @Inject lateinit var presenterManager: PresenterManager
    @Inject lateinit var dateProvider: CurrentDateProvider
    @LightCycle internal var logger = LightCycleLogger.forSupportFragment(javaClass.name)

    private val resume = BehaviorSubject.create<Optional<Long>>()

    constructor() : super()

    internal constructor(presenterManager: PresenterManager, dateProvider: CurrentDateProvider) : super() {
        this.presenterManager = presenterManager
        this.dateProvider = dateProvider
    }

    val enterScreenTimestamp: Observable<Pair<Long, Screen>> by lazy {
        val onResume = resume.filter { it.isPresent }.map { Pair(it.get(), getScreen()) }
        val act = activity
        when (act) {
            is MainActivity -> act.pageSelectedTimestampWithScreen().flatMap { onResume }
            else -> onResume
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            initializePresenter()
        } else {
            presenterId = savedInstanceState.getLong(presenterKey)
            presenter = presenterManager.get(presenterId)
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

    override fun onResume() {
        super.onResume()
        resume.onNext(Optional.of(dateProvider.currentTime))
    }

    override fun onPause() {
        super.onPause()
        resume.onNext(Optional.absent())
    }

    protected abstract fun getScreen(): Screen

    protected abstract fun disconnectPresenter(presenter: T)

    protected abstract fun connectPresenter(presenter: T)

    protected abstract fun createPresenter(): T

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(presenterKey, presenterId)
        super.onSaveInstanceState(outState)
    }
}
