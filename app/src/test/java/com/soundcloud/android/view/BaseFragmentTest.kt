package com.soundcloud.android.view

import android.annotation.SuppressLint
import android.os.Bundle
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.main.Screen
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.utils.CurrentDateProvider
import com.soundcloud.android.utils.TestDateProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil
import java.util.concurrent.TimeUnit

@Suppress("IllegalIdentifier")
class BaseFragmentTest : AndroidUnitTest() {

    private lateinit var baseFragment: TestBaseFragment

    private val basePresenter = TestBasePresenter()
    private val presenterManager: PresenterManager = mock()
    private val testDateProvider = TestDateProvider()

    @Before
    fun setUp() {
        baseFragment = TestBaseFragment(presenterManager, basePresenter, testDateProvider)

        whenever(presenterManager.save(basePresenter)).thenReturn(123L)
    }

    @Test
    fun `connects Presenter in on create`() {
        SupportFragmentTestUtil.startFragment(baseFragment)

        baseFragment.onCreate(null)
        baseFragment.onViewCreated(mock(), null)

        assertThat(baseFragment.connectedPresenter).isSameAs(basePresenter)
    }

    @Test
    fun `disconnects Presenter in on destroy`() {
        SupportFragmentTestUtil.startFragment(baseFragment)

        baseFragment.onCreate(null)
        baseFragment.onViewCreated(mock(), null)

        assertThat(baseFragment.connectedPresenter).isSameAs(basePresenter)
        baseFragment.activity.finish()
        baseFragment.onDestroyView()
        baseFragment.onDestroy()

        assertThat(baseFragment.connectedPresenter).isNull()

        verify(presenterManager).remove(123L)
    }

    @Test
    fun `uses presenter from manager`() {
        val reusedPresenter = TestBasePresenter()
        whenever(presenterManager.get<TestBasePresenter>(123L)).thenReturn(reusedPresenter)

        val savedInstanceState = getBundle()
        baseFragment.onCreate(savedInstanceState)
        baseFragment.onViewCreated(mock(), savedInstanceState)

        assertThat(baseFragment.connectedPresenter).isSameAs(reusedPresenter)
        verify(presenterManager, never()).save(any())
    }

    @Test
    fun `sends enter timestamp on resume`() {
        testDateProvider.setTime(123, TimeUnit.DAYS)
        val test = baseFragment.enterScreenTimestamp.test()

        SupportFragmentTestUtil.startFragment(baseFragment)

        test.assertValue(Pair(testDateProvider.currentTime, Screen.UNKNOWN))
    }

    @Test
    fun `does not send enter timestamp on pause`() {
        testDateProvider.setTime(123, TimeUnit.DAYS)
        val test = baseFragment.enterScreenTimestamp.test()

        SupportFragmentTestUtil.startFragment(baseFragment)

        baseFragment.onPause()

        test.assertNoErrors()
        test.assertValueCount(1)
    }

    private fun getBundle(): Bundle {
        val savedInstanceState = Bundle()
        savedInstanceState.putLong("TestBasePresenterKey", 123)
        return savedInstanceState
    }

    @SuppressLint("ValidFragment")
    internal class TestBaseFragment(presenterManager: PresenterManager,
                                    val basePresenter: TestBasePresenter,
                                    dateProvider: CurrentDateProvider) : BaseFragment<TestBasePresenter>(presenterManager, dateProvider) {

        override fun getScreen(): Screen = Screen.UNKNOWN

        override val presenterKey: String = "TestBasePresenterKey"

        var connectedPresenter: TestBasePresenter? = null

        override fun disconnectPresenter(presenter: TestBasePresenter) {
            if (connectedPresenter == presenter) connectedPresenter = null
        }

        override fun connectPresenter(presenter: TestBasePresenter) {
            connectedPresenter = presenter
        }

        override fun createPresenter(): TestBasePresenter {
            return basePresenter
        }
    }

}

class TestBasePresenter : Destroyable()
