package com.soundcloud.android.view

import com.soundcloud.android.testsupport.AndroidUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class PresenterManagerTest : AndroidUnitTest() {

    private lateinit var presenterManager: PresenterManager

    @Before
    fun setUp() {
        presenterManager = PresenterManager()
    }

    @Test
    fun savesAndLoadsPresenters() {
        val presenter1 = TestBasePresenter()
        val presenter2 = TestBasePresenter()

        val id1 = presenterManager.save(presenter1)
        val id2 = presenterManager.save(presenter2)

        assertThat(presenterManager.get<TestBasePresenter>(id1)).isSameAs(presenter1);
        assertThat(presenterManager.get<TestBasePresenter>(id2)).isSameAs(presenter2);
    }

    @Test
    fun removesPresenter() {
        val presenter1 = TestBasePresenter()

        val id1 = presenterManager.save(presenter1)

        presenterManager.remove(id1)

        assertThat(presenterManager.get<TestBasePresenter>(id1)).isNull();
    }
}



