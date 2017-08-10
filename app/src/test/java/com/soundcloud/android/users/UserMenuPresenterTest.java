package com.soundcloud.android.users;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import io.reactivex.Maybe;
import io.reactivex.subjects.CompletableSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;
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
        when(userRepository.localUserInfo(any(Urn.class))).thenReturn(Maybe.just(USER));

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
        final CompletableSubject followObservable = CompletableSubject.create();

        when(followingOperations.toggleFollowing(USER.urn(), !USER.isFollowing())).thenReturn(followObservable);
        presenter.show(button, USER.urn(), EVENT_CONTEXT_METADATA);

        presenter.handleToggleFollow(USER);

        verify(engagementsTracking).followUserUrn(USER.urn(), !USER.isFollowing(), EVENT_CONTEXT_METADATA);

        assertThat(followObservable.hasObservers()).isTrue();
    }

    @Test
    public void startsStation() {
        AppCompatActivity activity = activity();
        presenter.show(button, USER.urn(), EVENT_CONTEXT_METADATA);

        presenter.handleOpenStation(activity, USER);

        verify(stationHandler).startStation(Urn.forArtistStation(USER.urn().getNumericId()));
    }

}
