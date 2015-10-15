package com.soundcloud.android.you;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.view.View;

public class YouPresenterTest extends AndroidUnitTest {

    private static final PropertySet USER = TestPropertySets.user();
    private static final Urn USER_URN = USER.get(UserProperty.URN);

    private YouPresenter presenter;

    @Mock private YouViewFactory youViewFactory;
    @Mock private YouFragment fragment;
    @Mock private View fragmentView;
    @Mock private YouView youView;
    @Mock private UserRepository userRepository;
    @Mock private AccountOperations accountOperations;
    @Mock private ImageOperations imageOperations;

    @Before
    public void setUp() throws Exception {
        presenter = new YouPresenter(youViewFactory, userRepository, accountOperations, imageOperations, resources());
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
        when(youViewFactory.create(fragmentView)).thenReturn(youView);
    }

    @Test
    public void onCreateDoesNothingWithNoView() {
        when(userRepository.localAndSyncedUserInfo(USER_URN)).thenReturn(Observable.just(USER));

        presenter.onCreate(fragment, null);

        verifyZeroInteractions(youViewFactory);
        verifyZeroInteractions(youView);
    }

    @Test
    public void onViewCreatedBindsLoadedUserToView() {
        when(userRepository.localAndSyncedUserInfo(USER_URN)).thenReturn(Observable.just(USER));

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);

        verifyUserBound();
    }

    @Test
    public void onViewCreatedBindsUserToViewWhenLoadedAfterViewCreated() {
        final PublishSubject<PropertySet> subject = PublishSubject.<PropertySet>create();
        when(userRepository.localAndSyncedUserInfo(USER_URN)).thenReturn(subject);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);

        subject.onNext(USER);

        verifyUserBound();
    }

    @Test
    public void unbindsHeaderViewInOnDestroyView() {
        final PublishSubject<PropertySet> subject = PublishSubject.<PropertySet>create();
        when(userRepository.localAndSyncedUserInfo(USER_URN)).thenReturn(subject);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);
        presenter.onDestroyView(fragment);

        verify(youView).unbind();
    }

    private void verifyUserBound() {
        verify(youView).setUrn(USER_URN);
        verify(youView).setUsername(USER.get(UserProperty.USERNAME));
    }
}
