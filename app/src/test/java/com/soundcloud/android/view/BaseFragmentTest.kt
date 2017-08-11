package com.soundcloud.android.view

import android.os.Bundle
import com.nhaarman.mockito_kotlin.*
import com.soundcloud.android.testsupport.AndroidUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil

class BaseFragmentTest : AndroidUnitTest() {

    private lateinit var baseFragment: TestBaseFragment

    internal val basePresenter: TestBasePresenter = TestBasePresenter()

    val presenterManager: PresenterManager = mock()

    @Before
    fun setUp() {
        baseFragment = TestBaseFragment(presenterManager, basePresenter)

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

    private fun getBundle(): Bundle {
        val savedInstanceState = Bundle()
        savedInstanceState.putLong(BaseFragment.PRESENTER_KEY, 123)
        return savedInstanceState
    }

    internal class TestBaseFragment(presenterManager: PresenterManager, val basePresenter: TestBasePresenter) : BaseFragment<TestBasePresenter>(presenterManager) {

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

class TestBasePresenter : BasePresenter()
