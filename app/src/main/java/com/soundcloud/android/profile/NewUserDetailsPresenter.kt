package com.soundcloud.android.profile

import com.soundcloud.android.analytics.ScreenProvider
import com.soundcloud.android.analytics.SearchQuerySourceInfo
import com.soundcloud.android.model.Urn
import com.soundcloud.android.navigation.NavigationTarget
import com.soundcloud.android.navigation.Navigator
import com.soundcloud.android.playback.DiscoverySource
import com.soundcloud.android.rx.RxJava
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.users.SocialMediaLinkItem
import com.soundcloud.android.users.UserProfileInfo
import com.soundcloud.android.utils.collection.AsyncLoader
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.utils.extensions.plusAssign
import com.soundcloud.android.view.BasePresenter
import com.soundcloud.android.view.BaseView
import com.soundcloud.java.optional.Optional
import io.reactivex.Observable
import javax.inject.Inject

class NewUserDetailsPresenter
@Inject
internal constructor(val profileOperations: UserProfileOperations,
                     val navigator: Navigator,
                     val screenProvider: ScreenProvider) : BasePresenter<List<UserDetailItem>, RxSignal, UserDetailsParams, NewUserDetailsView>() {

    override fun firstPageFunc(pageParams: UserDetailsParams): Observable<AsyncLoader.PageResult<List<UserDetailItem>, RxSignal>> {
        return RxJava.toV2Observable(profileOperations.userProfileInfo(pageParams.userUrn))
                .map { UserDetailItem.from(it, pageParams.searchQuerySourceInfo) }
                .map { AsyncLoader.PageResult<List<UserDetailItem>, RxSignal>(it) }
    }

    override fun refreshFunc(pageParams: UserDetailsParams) = firstPageFunc(pageParams)

    override fun attachView(view: NewUserDetailsView) {
        super.attachView(view)
        compositeDisposable += view.followersClickListener.subscribe { navigator.navigateTo(NavigationTarget.forFollowers(it.userUrn, Optional.fromNullable(it.searchQuerySourceInfo))) }
        compositeDisposable += view.followingsClickListener.subscribe { navigator.navigateTo(NavigationTarget.forFollowings(it.userUrn, Optional.fromNullable(it.searchQuerySourceInfo))) }
        compositeDisposable += view.linkClickListener.subscribe {
            navigator.navigateTo(NavigationTarget.forNavigation(it, Optional.absent(), screenProvider.lastScreen, Optional.of(DiscoverySource.RECOMMENDATIONS)))
        }
    }
}

interface NewUserDetailsView : BaseView<AsyncLoaderState<List<UserDetailItem>, RxSignal>, RxSignal, UserDetailsParams> {
    val linkClickListener: Observable<String>
    val followersClickListener: Observable<UserFollowsItem>
    val followingsClickListener: Observable<UserFollowsItem>
}

sealed class UserDetailItem {
    companion object {
        fun from(profile: UserProfileInfo, searchQuerySourceInfo: SearchQuerySourceInfo?) = follows(profile, searchQuerySourceInfo) + bio(profile) + links(profile)

        fun empty(userDetailsParams: UserDetailsParams) = listOf(UserFollowsItem(userDetailsParams.userUrn, userDetailsParams.searchQuerySourceInfo))

        private fun links(profile: UserProfileInfo) = with(profile.socialMediaLinks) {
            if (collection.isEmpty()) emptyList() else listOf(UserLinksItem(collection))
        }

        private fun bio(profile: UserProfileInfo) = profile.description.filter(String::isNotBlank).transform { listOf(UserBioItem(it)) }.or(emptyList())

        private fun follows(profile: UserProfileInfo, searchQuerySourceInfo: SearchQuerySourceInfo?) = with(profile) {
            listOf(UserFollowsItem(user.urn(), searchQuerySourceInfo, user.followersCount(), user.followingsCount()))
        }
    }
}

data class UserBioItem(val bio: String) : UserDetailItem()
data class UserFollowsItem(val userUrn: Urn, val searchQuerySourceInfo: SearchQuerySourceInfo?, val followersCount: Int? = null, val followingCount: Int? = null) : UserDetailItem()
data class UserLinksItem(val socialMediaLinks: List<SocialMediaLinkItem>) : UserDetailItem()
object UserLoadingItem : UserDetailItem()

data class UserDetailsParams(val userUrn: Urn, val searchQuerySourceInfo: SearchQuerySourceInfo?)
