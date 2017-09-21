package com.soundcloud.android.navigation

import com.soundcloud.android.main.Screen
import com.soundcloud.android.testsupport.AndroidUnitTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

@Suppress("IllegalIdentifier")
class NavigationStateControllerTest : AndroidUnitTest() {

    private lateinit var navigationStateController: NavigationStateController

    @Before
    fun setUp() {
        navigationStateController = NavigationStateController(sharedPreferences())
    }

    @Test
    fun `it stores and retrieves the expected screen`() {
        val screen = Screen.DISCOVER

        navigationStateController.setState(screen)
        Assertions.assertThat(navigationStateController.getState()).isEqualTo(screen)
    }

    @Test
    fun `it returns Unknown screen when there is no value for the given key`() {
        Assertions.assertThat(navigationStateController.getState()).isEqualTo(Screen.UNKNOWN)
    }
}
