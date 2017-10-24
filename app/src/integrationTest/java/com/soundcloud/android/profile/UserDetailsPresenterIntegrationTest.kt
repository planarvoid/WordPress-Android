package com.soundcloud.android.profile

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import com.soundcloud.android.BaseIntegrationTest
import com.soundcloud.android.SoundCloudApplication
import com.soundcloud.android.analytics.SearchQuerySourceInfo
import com.soundcloud.android.api.ApiEndpoints
import com.soundcloud.android.framework.TestUser
import com.soundcloud.android.hamcrest.TestAsyncState
import com.soundcloud.android.model.Urn
import com.soundcloud.android.users.Network
import com.soundcloud.android.users.SocialMediaLinkItem
import com.soundcloud.android.utils.Supplier
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.view.ViewError.CONNECTION_ERROR
import com.soundcloud.java.optional.Optional
import io.reactivex.subjects.PublishSubject
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class UserDetailsPresenterIntegrationTest : BaseIntegrationTest(TestUser.testUser) {
    val userUrn = Urn.forUser(123)
    val searchQuerySourceInfo = SearchQuerySourceInfo(Urn("soundcloud:queries:456"), "queryString")
    val userDetailsParams = UserDetailsParams(userUrn, searchQuerySourceInfo)
    val socialMediaLinkItem = SocialMediaLinkItem(Optional.of("foo"), Network.MIXCLOUD, "https://www.mixcloud.com/discover/tycho/")

    @Test
    fun presenterDoesNotEmitWhenNotConnected() {
        noNetwork()

        val testView = UserDetailsPresenterIntegrationTest.TestView()

        testView.assertState(Matchers.empty<AsyncLoaderState<List<UserDetailItem>>>())
    }

    @Test
    fun presenterEmitsLoadingStateWhenConnected() {
        unrespondingNetwork()

        val testView = UserDetailsPresenterIntegrationTest.TestView()

        testView.initialLoadSignal.onNext(userDetailsParams)

        testView.assertState(Matchers.contains(loadingState()))
    }

    @Test
    fun presenterShowsNetworkError() {
        noNetwork()

        val testView = UserDetailsPresenterIntegrationTest.TestView()

        testView.initialLoadSignal.onNext(userDetailsParams)

        testView.assertState(Matchers.contains(loadingState()))
        testView.assertLastState({ it.data }, Matchers.nullValue())
        testView.assertLastState({ it.asyncLoadingState.nextPageError }, Matchers.equalTo(CONNECTION_ERROR))
    }

    @Test
    fun presenterShowsUserInfo() {
        addMockedResponse(ApiEndpoints.PROFILE_INFO.path(userUrn), "user-info.json")

        val testView = UserDetailsPresenterIntegrationTest.TestView()

        testView.initialLoadSignal.onNext(userDetailsParams)

        println(testView.states().get())
        testView.assertLastState({ it.data }, Matchers.notNullValue())
        testView.assertLastState({ it.data?.size }, Matchers.equalTo(3))
        testView.assertLastState({ it.data?.get(0) }, Matchers.equalTo<UserDetailItem>(UserFollowsItem(userUrn, searchQuerySourceInfo, 121, 340)))
        testView.assertLastState({ it.data?.get(1) }, Matchers.equalTo<UserDetailItem>(UserBioItem("I'm here to make friends")))
        testView.assertLastState({ it.data?.get(2) }, Matchers.equalTo<UserDetailItem>(UserLinksItem(listOf(socialMediaLinkItem))))
    }

    private fun loadingState(): AsyncLoaderState<List<UserDetailItem>> =
            AsyncLoaderState.loadingNextPage()

    internal class TestView : TestAsyncState<AsyncLoaderState<List<UserDetailItem>>>(), UserDetailsView {

        val presenter: UserDetailsPresenter = SoundCloudApplication.getObjectGraph().newUserDetailsPresenter()
        val models: MutableList<AsyncLoaderState<List<UserDetailItem>>> = mutableListOf()
        val initialLoadSignal = PublishSubject.create<UserDetailsParams>()
        override val linkClickListener = PublishSubject.create<String>()
        override val followersClickListener = PublishSubject.create<UserFollowsItem>()
        override val followingsClickListener = PublishSubject.create<UserFollowsItem>()

        override fun requestContent() = initialLoadSignal
        override fun states() = Supplier { models }
        override fun accept(model: AsyncLoaderState<List<UserDetailItem>>) {
            models.add(model)
        }

        init {
            presenter.attachView(this)
        }
    }
}
