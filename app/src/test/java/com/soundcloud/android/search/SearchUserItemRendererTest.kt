package com.soundcloud.android.search

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ToggleButton
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.R
import com.soundcloud.android.analytics.EngagementsTracking
import com.soundcloud.android.analytics.ScreenProvider
import com.soundcloud.android.api.model.ApiUser
import com.soundcloud.android.associations.FollowingOperations
import com.soundcloud.android.main.Screen
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.testsupport.UserFixtures
import com.soundcloud.android.view.adapters.UserItemRenderer
import com.soundcloud.java.optional.Optional
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

class SearchUserItemRendererTest : AndroidUnitTest() {

    private lateinit var renderer: SearchUserItemRenderer

    @Mock private lateinit var userItemRenderer: UserItemRenderer
    @Mock private lateinit var followingOperations: FollowingOperations
    @Mock private lateinit var engagementsTracking: EngagementsTracking
    @Mock private lateinit var screenProvider: ScreenProvider

    private lateinit var itemView: View
    private lateinit var user: ApiUser

    private val followToggleButton: ToggleButton
        get() = itemView.findViewById<View>(R.id.toggle_btn_follow) as ToggleButton

    @Before
    fun setup() {
        whenever(screenProvider.lastScreen).thenReturn(Screen.USER_MAIN)

        renderer = SearchUserItemRenderer(userItemRenderer,
                                          followingOperations,
                                          engagementsTracking,
                                          screenProvider)

        itemView = LayoutInflater.from(AndroidUnitTest.context()).inflate(R.layout.user_list_item, FrameLayout(AndroidUnitTest.context()), false)
        user = UserFixtures.apiUser()
    }

    @Test
    fun shouldSetFollowedToggleButton() {
        val followedUserItem = UserFixtures.userItem(user).copyWithFollowing(true)
        renderer.bindItemView(0, itemView, listOf(SearchUserItem(followedUserItem, Optional.absent())))

        val followButton = followToggleButton
        assertThat(followButton.isChecked).isTrue()
        assertThat(followButton.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun shouldNotSetFollowedToggleButton() {
        val unfollowedUserItem = UserFixtures.userItem(user).copyWithFollowing(false)
        renderer.bindItemView(0, itemView, listOf(SearchUserItem(unfollowedUserItem, Optional.absent())))

        val followButton = followToggleButton
        assertThat(followButton.isChecked).isFalse()
        assertThat(followButton.visibility).isEqualTo(View.VISIBLE)
    }
}
