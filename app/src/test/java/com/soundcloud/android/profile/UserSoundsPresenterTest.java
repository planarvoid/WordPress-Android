package com.soundcloud.android.profile;

import static com.soundcloud.android.R.layout.default_recyclerview_with_refresh;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class UserSoundsPresenterTest extends AndroidUnitTest {

    private static final Urn USER_URN = Urn.forUser(123L);

    @Rule public final FragmentRule fragmentRule = new FragmentRule(default_recyclerview_with_refresh, new Bundle());

    private UserSoundsPresenter presenter;
    private ApiUserProfile userProfileResponse;
    Bundle fragmentArgs;

    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacker;
    @Mock private UserSoundsAdapter adapter;
    @Mock private UserProfileOperations operations;
    @Mock private MixedItemClickListener.Factory itemClickListenerFactory;
    @Mock private UserSoundsMapper userSoundsMapper;

    @Mock private UserSoundsItem userSoundsItem;

    @Before
    public void setUp() throws Exception {
        userProfileResponse = new UserProfileFixtures.Builder().build();
        fragmentArgs = new Bundle();
        fragmentArgs.putParcelable(ProfileArguments.USER_URN_KEY, USER_URN);
        fragmentRule.setFragmentArguments(fragmentArgs);

        presenter = new UserSoundsPresenter(imagePauseOnScrollListener, swipeRefreshAttacker, adapter, operations,
                itemClickListenerFactory, userSoundsMapper);

        doReturn(Observable.just(userProfileResponse)).when(operations).userProfile(USER_URN);
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() throws Exception {
        when(userSoundsMapper.call(userProfileResponse)).thenReturn(singletonList(userSoundsItem));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(adapter).onNext(singletonList(userSoundsItem));
    }

    @Test
    public void configuresEmptyViewInOnViewCreatedForOthersProfile() throws Exception {
        EmptyView emptyView = mock(EmptyView.class);

        fragmentArgs.putBoolean(UserSoundsFragment.IS_CURRENT_USER, false);
        fragmentRule.setFragmentArguments(fragmentArgs);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), view(emptyView), null);

        verify(emptyView).setImage(R.drawable.empty_stream);
        verify(emptyView).setMessageText(R.string.empty_user_sounds_message);
    }

    @Test
    public void configuresEmptyViewInOnViewCreatedForOwnProfile() throws Exception {
        EmptyView emptyView = mock(EmptyView.class);

        fragmentArgs.putBoolean(UserSoundsFragment.IS_CURRENT_USER, true);
        fragmentRule.setFragmentArguments(fragmentArgs);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), view(emptyView), null);

        verify(emptyView).setImage(R.drawable.empty_stream);
        verify(emptyView).setMessageText(R.string.empty_you_sounds_message);
    }

    private View view(EmptyView emptyView) {
        View view = mock(View.class);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.findViewById(R.id.ak_recycler_view)).thenReturn(mock(RecyclerView.class));
        return view;
    }
}