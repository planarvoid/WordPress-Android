package com.soundcloud.android.profile

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.SoundCloudApplication
import com.soundcloud.android.analytics.SearchQuerySourceInfo
import com.soundcloud.android.main.Screen
import com.soundcloud.android.model.AsyncLoadingState
import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.RecyclerItemAdapter
import com.soundcloud.android.utils.Urns
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.view.BaseFragment
import com.soundcloud.android.view.collection.CollectionRenderer
import com.soundcloud.android.view.collection.CollectionRendererState
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class UserDetailsFragment : BaseFragment<UserDetailsPresenter>(), UserDetailsView {

    override val presenterKey: String = "UserDetailsPresenterKey"
    override val linkClickListener: PublishSubject<String> = PublishSubject.create()
    override val followersClickListener: PublishSubject<UserFollowsItem> = PublishSubject.create()
    override val followingsClickListener: PublishSubject<UserFollowsItem> = PublishSubject.create()

    @Inject lateinit var presenterLazy: Lazy<UserDetailsPresenter>
    @Inject lateinit var adapterFactory: UserDetailAdapter.Factory

    private lateinit var collectionRenderer: CollectionRenderer<UserDetailItem, RecyclerItemAdapter.ViewHolder>

    init {
        SoundCloudApplication.getObjectGraph().inject(this)
    }

    override fun getScreen(): Screen = Screen.USER_INFO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionRenderer = CollectionRenderer(adapterFactory.create(followersClickListener, followingsClickListener, linkClickListener),
                                                sameIdentity = { firstItem, secondItem -> firstItem.javaClass == secondItem.javaClass },
                                                emptyStateProvider = null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        collectionRenderer.attach(view, false, LinearLayoutManager(view.context))
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        collectionRenderer.detach()
        super.onDestroyView()
    }

    override fun requestContent(): Observable<UserDetailsParams> {
        return Observable.just(userDetailsParams())
    }

    private fun userDetailsParams(): UserDetailsParams {
        return UserDetailsParams(
                userUrn = requireNotNull(Urns.urnFromBundle(arguments, ProfileArguments.USER_URN_KEY)) { "Missing required param ${ProfileArguments.USER_URN_KEY}" },
                searchQuerySourceInfo = arguments.getParcelable(ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY)
        )
    }

    override fun accept(loaderState: AsyncLoaderState<List<UserDetailItem>>) = with(loaderState) {
        val alwaysShowFollows = (data ?: UserDetailItem.Companion.empty(userDetailsParams())) + listOf(UserLoadingItem).filter { asyncLoadingState.isLoadingNextPage }
        collectionRenderer.render(CollectionRendererState(AsyncLoadingState(), alwaysShowFollows))
    }

    override fun disconnectPresenter(presenter: UserDetailsPresenter) = presenter.detachView()

    override fun connectPresenter(presenter: UserDetailsPresenter) = presenter.attachView(this)

    override fun createPresenter() = presenterLazy.get()

    override fun refreshSignal() = collectionRenderer.onRefresh().map { userDetailsParams() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.recyclerview_with_refresh_without_empty, container, false)

    companion object {
        fun create(userUrn: Urn, searchQuerySourceInfo: SearchQuerySourceInfo?): UserDetailsFragment {
            return UserDetailsFragment().apply {
                arguments = Bundle().apply {
                    Urns.writeToBundle(this, ProfileArguments.USER_URN_KEY, userUrn)
                    putParcelable(ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo)
                }
            }
        }
    }
}
