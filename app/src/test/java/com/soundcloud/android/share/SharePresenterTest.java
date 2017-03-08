package com.soundcloud.android.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.firebase.FirebaseDynamicLinksApi;
import com.soundcloud.android.configuration.experiments.DynamicLinkSharingConfig;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowDialog;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class SharePresenterTest extends AndroidUnitTest {

    private static final String SCREEN_TAG = "screen_tag";
    private static final String PAGE_NAME = "page_name";
    private static final Urn PAGE_URN = Urn.forPlaylist(234L);
    private static final TrackItem TRACK = PlayableFixtures.expectedTrackForPlayer();
    private static final TrackItem PRIVATE_TRACK = PlayableFixtures.expectedPrivateTrackForPlayer();
    private static final PlaylistItem PLAYLIST = PlayableFixtures.expectedPostedPlaylistsForPostedPlaylistsScreen();
    public static final String PROMOTED_TRACK_URN = "soundcloud:tracks:12345";
    public static final String AD_URN = "ad:urn:123";
    private static final TrackItem PROMOTED_TRACK = PlayableFixtures.expectedPromotedTrackBuilder(new Urn(PROMOTED_TRACK_URN), AD_URN).build();
    private static final PromotedSourceInfo PROMOTED_SOURCE_INFO = PromotedSourceInfo.fromItem(PROMOTED_TRACK);

    private SharePresenter operations;
    private Activity activityContext;
    private TestEventBus eventBus = new TestEventBus();
    @Mock private DynamicLinkSharingConfig config;
    @Mock private EventTracker tracker;
    @Mock private FirebaseDynamicLinksApi firebaseApi;
    @Captor ArgumentCaptor<UIEvent> uiEventCaptor;

    @Before
    public void setUp() {
        activityContext = activity();
        operations = new SharePresenter(config, tracker, firebaseApi);
    }

    @Test
    public void sharePlayableStartsShareActivity() throws Exception {
        operations.share(activityContext, PLAYLIST, eventContext(), PROMOTED_SOURCE_INFO);
        assertShareActivityStarted(PLAYLIST, "http://permalink.url");
    }

    @Test
    public void shareTrackPublishesTrackingEventForRequest() throws Exception {
        operations.share(activityContext, TRACK, eventContext(), null);

        verify(tracker, atLeastOnce()).trackEngagement(uiEventCaptor.capture());

        final UIEvent shareRequestEvent = uiEventCaptor.getAllValues().get(0);

        assertThat(shareRequestEvent.kind()).isSameAs(UIEvent.Kind.SHARE);
        assertThat(shareRequestEvent.clickName().get()).isSameAs(UIEvent.ClickName.SHARE_REQUEST);
        assertThat(shareRequestEvent.contextScreen().get()).isEqualTo(SCREEN_TAG);
        assertThat(shareRequestEvent.originScreen().get()).isEqualTo(PAGE_NAME);
        assertThat(shareRequestEvent.clickObjectUrn().get().toString()).isEqualTo("soundcloud:tracks:123");
    }

    @Test
    public void sharePromotedTrackPublishesTrackingEventForRequest() throws Exception {
        operations.share(activityContext, PROMOTED_TRACK, eventContext(), PROMOTED_SOURCE_INFO);

        verify(tracker, atLeastOnce()).trackEngagement(uiEventCaptor.capture());

        final UIEvent shareRequestEvent = uiEventCaptor.getAllValues().get(0);

        assertThat(shareRequestEvent.kind()).isSameAs(UIEvent.Kind.SHARE);
        assertThat(shareRequestEvent.clickName().get()).isSameAs(UIEvent.ClickName.SHARE_REQUEST);
        assertThat(shareRequestEvent.contextScreen().get()).isEqualTo(SCREEN_TAG);
        assertThat(shareRequestEvent.originScreen().get()).isEqualTo(PAGE_NAME);
        assertThat(shareRequestEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(shareRequestEvent.clickObjectUrn().get().toString()).isEqualTo(PROMOTED_TRACK_URN);
        assertThat(shareRequestEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(shareRequestEvent.monetizationType().get()).isEqualTo(AdData.MonetizationType.PROMOTED);
        assertThat(shareRequestEvent.promoterUrn().get().toString()).isEqualTo("soundcloud:users:193");
    }

    @Test
    public void shouldNotSharePrivateTracks() throws Exception {
        operations.share(activityContext, PRIVATE_TRACK, eventContext(), PROMOTED_SOURCE_INFO);

        Assertions.assertThat(activityContext).hasNoNextStartedIntent();
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void shareStartsShareActivityWithDynamicLinkWhenFeatureEnabled() throws Exception {
        when(config.isEnabled()).thenReturn(true);
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(Observable.just("http://goo.gl/foo"));
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertShareActivityStarted(TRACK, "http://goo.gl/foo?/somepath");
    }

    @Test
    public void shareStartsShareActivityWithNonDynamicLinkWhenFeatureDisabled() throws Exception {
        when(config.isEnabled()).thenReturn(false);
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertShareActivityStarted(TRACK, "http://foo.com/somepath");
    }

    @Test
    public void sharePublishesTrackingEventForPromptWhenDynamicLinkFeatureDisabled() throws Exception {
        when(config.isEnabled()).thenReturn(false);
        operations.share(activityContext, TRACK, eventContext(), PROMOTED_SOURCE_INFO);
        verify(tracker, times(2)).trackEngagement(uiEventCaptor.capture());
        UIEvent sharePromptEvent = uiEventCaptor.getAllValues().get(1);
        assertThat(sharePromptEvent.clickName().get()).isSameAs(UIEvent.ClickName.SHARE_PROMPT);
        assertThat(sharePromptEvent.shareLinkType().get()).isSameAs(UIEvent.ShareLinkType.SOUNDCLOUD);
    }

    @Test
    public void sharePublishesTrackingEventForPromptWhenDynamicLinkFeatureEnabledAndFirebaseRequestSucceeds() throws Exception {
        when(config.isEnabled()).thenReturn(true);
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(Observable.just("http://goo.gl/foo?/somepath"));
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        verify(tracker, times(2)).trackEngagement(uiEventCaptor.capture());
        UIEvent sharePromptEvent = uiEventCaptor.getAllValues().get(1);
        assertThat(sharePromptEvent.clickName().get()).isSameAs(UIEvent.ClickName.SHARE_PROMPT);
        assertThat(sharePromptEvent.shareLinkType().get()).isSameAs(UIEvent.ShareLinkType.FIREBASE);
    }

    @Test
    public void sharePublishesTrackingEventForPromptWhenDynamicLinkFeatureEnabledAndFirebaseRequestFails() throws Exception {
        when(config.isEnabled()).thenReturn(true);
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(Observable.error(new IOException()));
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        verify(tracker, times(2)).trackEngagement(uiEventCaptor.capture());
        UIEvent sharePromptEvent = uiEventCaptor.getAllValues().get(1);
        assertThat(sharePromptEvent.clickName().get()).isSameAs(UIEvent.ClickName.SHARE_PROMPT);
        assertThat(sharePromptEvent.shareLinkType().get()).isSameAs(UIEvent.ShareLinkType.SOUNDCLOUD);
    }

    @Test
    public void sharePublishesTrackingEventForCancelWhenFirebaseRequestCanceled() throws Exception {
        when(config.isEnabled()).thenReturn(true);
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(PublishSubject.create());
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        ShadowDialog.getLatestDialog().cancel();
        verify(tracker, times(2)).trackEngagement(uiEventCaptor.capture());
        UIEvent sharePromptEvent = uiEventCaptor.getAllValues().get(1);
        assertThat(sharePromptEvent.clickName().get()).isSameAs(UIEvent.ClickName.SHARE_CANCEL);
    }

    @Test
    public void shareStartsShareActivityWithNonDynamicLinkWhenLookupFailsDueToNetworkIssue() throws Exception {
        when(config.isEnabled()).thenReturn(true);
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(Observable.error(new IOException()));
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertShareActivityStarted(TRACK, "http://foo.com/somepath");
    }

    @Test
    public void shareDisplaysShareDialogWhileGeneratingDynamicLink() throws Exception {
        when(config.isEnabled()).thenReturn(true);
        PublishSubject<String> observable = PublishSubject.create();
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(observable);
        assertShareDialogNotShowing();
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertShareDialogShowing();
    }

    @Test
    public void shareDismissesShareDialogOnErrorGeneratingDynamicLink() throws Exception {
        when(config.isEnabled()).thenReturn(true);
        PublishSubject<String> observable = PublishSubject.create();
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(observable);
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertShareDialogShowing();
        observable.onError(new IOException());
        assertShareDialogNotShowing();
    }

    @Test
    public void shareDismissesShareDialogWhenDynamicLinkGenerated() throws Exception {
        when(config.isEnabled()).thenReturn(true);
        PublishSubject<String> observable = PublishSubject.create();
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(observable);
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertShareDialogShowing();
        observable.onNext("http://goo.gl/foo?/somepath");
        observable.onCompleted();
        assertShareDialogNotShowing();
    }

    @Test
    public void shareIgnoresDynamicLinkResultWhenDialogCanceled() throws Exception {
        when(config.isEnabled()).thenReturn(true);
        PublishSubject<String> observable = PublishSubject.create();
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(observable);
        assertShareDialogNotShowing();
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertShareDialogShowing();
        ShadowDialog.getLatestDialog().cancel();
        assertShareDialogNotShowing();
        observable.onNext("http://goo.gl/foo?/somepath");
        observable.onCompleted();
        assertShareActivityNotStarted();
    }

    @Test
    public void shareCrashesAppWhenLookupFailsDueToAppBug() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread.setDefaultUncaughtExceptionHandler((t, throwable) -> error.set(throwable));
        when(config.isEnabled()).thenReturn(true);
        NullPointerException expectedException = new NullPointerException();
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(Observable.error(expectedException));
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertThat(error.get()).isNotNull();
        assertThat(getRootCause(error.get())).isSameAs(expectedException);
    }

    private Throwable getRootCause(Throwable throwable) {
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable;
    }

    private EventContextMetadata eventContext() {
        return EventContextMetadata.builder()
                                   .contextScreen(SCREEN_TAG)
                                   .pageName(PAGE_NAME)
                                   .pageUrn(PAGE_URN).build();
    }

    private void assertShareActivityNotStarted() {
        Assertions.assertThat(activityContext)
                  .hasNoNextStartedIntent();
    }

    private void assertShareActivityStarted(PlayableItem playable, String url) {
        Assertions.assertThat(activityContext)
                  .nextStartedIntent()
                  .containsAction(Intent.ACTION_CHOOSER)
                  .wrappedIntent()
                  .containsAction(Intent.ACTION_SEND)
                  .containsExtra(Intent.EXTRA_SUBJECT, playable.title() + " - SoundCloud")
                  .containsExtra(Intent.EXTRA_TEXT,
                                 "Listen to " + playable.title() + " by " + playable.creatorName() + " #np on #SoundCloud\n" + url);
    }

    private void assertShareDialogShowing() {
        assertThat(ShadowDialog.getLatestDialog()).isNotNull();
        assertThat(ShadowDialog.getLatestDialog().isShowing()).isTrue();
        assertThat(ShadowDialog.getLatestDialog().findViewById(R.id.share_dialog_progress)).isNotNull();
    }

    private void assertShareDialogNotShowing() {
        Dialog latestDialog = ShadowDialog.getLatestDialog();
        if (latestDialog == null) {
            return;
        }
        assertThat(latestDialog.findViewById(R.id.share_dialog_progress)).isNotNull();
        assertThat(latestDialog.isShowing()).isFalse();
    }
}
