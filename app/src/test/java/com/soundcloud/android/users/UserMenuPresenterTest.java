package com.soundcloud.android.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.view.View;

public class UserMenuPresenterTest extends AndroidUnitTest {
    private static final PropertySet USER_PROPERTY_SET = TestPropertySets.user();
    private static final UserItem USER = UserItem.from(USER_PROPERTY_SET);

    @Mock private UserRepository userRepository;
    @Mock private FollowingOperations followingOperations;
    @Mock private StartStationHandler stationHandler;
    @Mock private UserMenuRendererFactory userMenuRenderFactory;
    @Mock private UserMenuRenderer userMenuRenderer;
    @Mock private View button;

    private UserMenuPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(userRepository.localUserInfo(any(Urn.class))).thenReturn(Observable.just(USER_PROPERTY_SET));
        when(followingOperations.toggleFollowing(any(Urn.class), anyBoolean())).thenReturn(Observable.just(
                USER_PROPERTY_SET));

        presenter = new UserMenuPresenter(userMenuRenderFactory, followingOperations, userRepository, stationHandler);

        when(userMenuRenderFactory.create(presenter, button)).thenReturn(userMenuRenderer);
    }

    @Test
    public void togglesFollowStatus() {
        final PublishSubject<PropertySet> followObservable = PublishSubject.create();

        when(followingOperations.toggleFollowing(USER.getUrn(), !USER.isFollowedByMe()))
                .thenReturn(followObservable);
        presenter.show(button, USER.getUrn());

        presenter.handleToggleFollow(USER);

        assertThat(followObservable.hasObservers()).isTrue();
    }

    @Test
    public void startsStation() {
        presenter.show(button, USER.getUrn());

        presenter.handleOpenStation(context(), USER);

        verify(stationHandler).startStation(context(), Urn.forArtistStation(USER.getUrn().getNumericId()));
    }

}
