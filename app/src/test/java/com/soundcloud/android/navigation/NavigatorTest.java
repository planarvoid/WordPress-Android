package com.soundcloud.android.navigation;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.ExpandPlayerSingleObserver;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class NavigatorTest {

    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Mock private Activity activity;
    @Mock private NavigationResolver navigationResolver;
    @Mock private FeedbackController feedbackController;
    @Mock private ExpandPlayerSingleObserver expandPlayerSingleObserver;

    private Navigator navigator;

    @Before
    public void setUp() throws Exception {
        navigator = new Navigator(navigationResolver);
    }

    @SuppressLint("CheckResult")
    @Test
    public void callsNavigationActions() throws Exception {
        Intent intent = mock(Intent.class);
        NavigationTarget target = NavigationTarget.forNavigation("target", Optional.absent(), Screen.DISCOVER, Optional.of(DiscoverySource.RECOMMENDATIONS));
        when(navigationResolver.resolveNavigationResult(any())).thenReturn(Single.just(NavigationResult.create(target, intent)));

        navigator.listenToNavigation().subscribeWith(new Navigator.Observer(activity, feedbackController, expandPlayerSingleObserver));

        navigator.navigateTo(target);

        verify(navigationResolver).resolveNavigationResult(target);
        verify(activity).startActivity(intent);

    }

    @SuppressLint("CheckResult")
    @Test
    public void doesNotCrashDuringActionExecution() throws Exception {
        Intent intent = mock(Intent.class);
        doThrow(ActivityNotFoundException.class).when(activity).startActivity(intent);
        NavigationTarget target = NavigationTarget.forNavigation("target", Optional.absent(), Screen.DISCOVER, Optional.of(DiscoverySource.RECOMMENDATIONS));
        when(navigationResolver.resolveNavigationResult(any())).thenReturn(Single.just(NavigationResult.create(target, intent)));

        navigator.listenToNavigation().subscribeWith(new Navigator.Observer(activity, feedbackController, expandPlayerSingleObserver));

        navigator.navigateTo(target);

        verify(activity).startActivity(intent);
        verify(navigationResolver).resolveNavigationResult(target);
        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackController).showFeedback(feedbackCaptor.capture());
        assertThat(feedbackCaptor.getValue().getMessage()).isEqualTo(R.string.error_unknown_navigation);
    }

    @Test
    public void crashesOnResolverError() throws Exception {
        NavigationTarget target = NavigationTarget.forNavigation("target", Optional.absent(), Screen.DISCOVER, Optional.of(DiscoverySource.RECOMMENDATIONS));
        IOException exception = new IOException();
        when(navigationResolver.resolveNavigationResult(any())).thenReturn(Single.error(exception));

        navigator.listenToNavigation().subscribeWith(new Navigator.Observer(activity, feedbackController, expandPlayerSingleObserver));
        expectedException.expect(NullPointerException.class);
        expectedException.expectCause(allOf(instanceOf(IllegalStateException.class),
                                            hasCause(equalTo(exception)),
                                            hasMessage(containsString(
                                                    "Complete in Navigation Subscription. This should never happen since navigation won\'t work in the app anymore. Thus we\'ll force close the app."))));
        navigator.navigateTo(target);
    }

    @Test
    public void crashOnComplete() throws Exception {
        PublishSubject<NavigationResult> testSubject = PublishSubject.create();

        testSubject.subscribeWith(new Navigator.Observer(activity, feedbackController, expandPlayerSingleObserver));
        expectedException.expect(IllegalStateException.class);
        testSubject.onComplete();
    }

    @Test
    public void expandsPlayerWithSuccessfulPlaybackResult() throws Exception {
        NavigationTarget target = NavigationTarget.forNavigation("target", Optional.absent(), Screen.DISCOVER, Optional.of(DiscoverySource.RECOMMENDATIONS));
        PlaybackResult playbackResult = PlaybackResult.success();
        when(navigationResolver.resolveNavigationResult(any())).thenReturn(Single.just(NavigationResult.create(target, playbackResult)));

        navigator.listenToNavigation().subscribeWith(new Navigator.Observer(activity, feedbackController, expandPlayerSingleObserver));

        navigator.navigateTo(target);

        verifyZeroInteractions(activity);
        verify(expandPlayerSingleObserver).onSuccess(playbackResult);
    }

    @Test
    public void doesNotExpandPlayerWithUnsuccessfulPlaybackResult() throws Exception {
        NavigationTarget target = NavigationTarget.forNavigation("target", Optional.absent(), Screen.DISCOVER, Optional.of(DiscoverySource.RECOMMENDATIONS));
        PlaybackResult playbackResult = PlaybackResult.error(PlaybackResult.ErrorReason.NONE);
        when(navigationResolver.resolveNavigationResult(any())).thenReturn(Single.just(NavigationResult.create(target, playbackResult)));

        navigator.listenToNavigation().subscribeWith(new Navigator.Observer(activity, feedbackController, expandPlayerSingleObserver));

        navigator.navigateTo(target);

        verifyZeroInteractions(activity);
        verifyZeroInteractions(expandPlayerSingleObserver);
    }

    @Test
    public void showsToastForUnsuccessfulResult() throws Exception {
        NavigationTarget target = NavigationTarget.forNavigation("target", Optional.absent(), Screen.DISCOVER, Optional.of(DiscoverySource.RECOMMENDATIONS));
        when(navigationResolver.resolveNavigationResult(any())).thenReturn(Single.just(NavigationResult.error(target)));

        navigator.listenToNavigation().subscribeWith(new Navigator.Observer(activity, feedbackController, expandPlayerSingleObserver));

        navigator.navigateTo(target);

        verifyZeroInteractions(activity);
        verifyZeroInteractions(expandPlayerSingleObserver);
        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackController).showFeedback(feedbackCaptor.capture());
        assertThat(feedbackCaptor.getValue().getMessage()).isEqualTo(R.string.error_unknown_navigation);
    }
}
