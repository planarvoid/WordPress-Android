package com.soundcloud.android.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.view.View;

public class UserMenuPresenterTest extends AndroidUnitTest {
    private static final EventContextMetadata EVENT_CONTEXT_METADATA = EventContextMetadata.builder().build();
    private static final User USER = ModelFixtures.user();

    @Mock private UserRepository userRepository;
    @Mock private FollowingOperations followingOperations;
    @Mock private StartStationHandler stationHandler;
    @Mock private UserMenuRendererFactory userMenuRenderFactory;
    @Mock private UserMenuRenderer userMenuRenderer;
    @Mock private EngagementsTracking engagementsTracking;
    @Mock private AccountOperations accountOperations;
    @Mock private View button;

    private UserMenuPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(userRepository.localUserInfo(any(Urn.class))).thenReturn(Observable.just(USER));
        when(followingOperations.toggleFollowing(any(Urn.class), anyBoolean())).thenReturn(Observable.empty());

        presenter = new UserMenuPresenter(userMenuRenderFactory,
                                          followingOperations,
                                          userRepository,
                                          stationHandler,
                                          engagementsTracking,
                                          accountOperations);

        when(userMenuRenderFactory.create(presenter, button)).thenReturn(userMenuRenderer);
    }

    @Test
    public void togglesFollowStatus() {
        final PublishSubject<FollowingStatusEvent> followObservable = PublishSubject.create();

        when(followingOperations.toggleFollowing(USER.urn(), !USER.isFollowing())).thenReturn(followObservable);
        presenter.show(button, USER.urn(), EVENT_CONTEXT_METADATA);

        presenter.handleToggleFollow(USER);

        verify(engagementsTracking).followUserUrn(USER.urn(), !USER.isFollowing(), EVENT_CONTEXT_METADATA);

        assertThat(followObservable.hasObservers()).isTrue();
    }

    @Test
    public void startsStation() {
        presenter.show(button, USER.urn(), EVENT_CONTEXT_METADATA);

        presenter.handleOpenStation(context(), USER);

        verify(stationHandler).startStation(context(), Urn.forArtistStation(USER.urn().getNumericId()));
    }

}
