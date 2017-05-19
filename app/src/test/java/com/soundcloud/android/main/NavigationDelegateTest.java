package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.deeplinks.IntentResolver;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.subjects.PublishSubject;
import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableCauseMatcher;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class NavigationDelegateTest {

    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Mock private Activity activity;
    @Mock private IntentResolver intentResolver;
    @Mock private FeedbackController feedbackController;

    private NavigationDelegate navigationDelegate;

    @Before
    public void setUp() throws Exception {
        navigationDelegate = new NavigationDelegate(intentResolver);
    }

    @SuppressLint("CheckResult")
    @Test
    public void callsNavigationActions() throws Exception {
        Action action = mock(Action.class);
        NavigationTarget target = NavigationTarget.forNavigation(activity, "target", Optional.absent(), Screen.DISCOVER);
        when(intentResolver.resolveNavigationResult(any())).thenReturn(Single.just(NavigationResult.create(target, action)));

        navigationDelegate.listenToNavigation().subscribeWith(new NavigationDelegate.Observer(feedbackController));

        navigationDelegate.navigateTo(target);

        verify(action).run();
        verify(intentResolver).resolveNavigationResult(target);
    }

    @SuppressLint("CheckResult")
    @Test
    public void doesNotCrashDuringActionExecution() throws Exception {
        Action action = mock(Action.class);
        doThrow(new ActivityNotFoundException("Test")).when(action).run();
        NavigationTarget target = NavigationTarget.forNavigation(activity, "target", Optional.absent(), Screen.DISCOVER);
        when(intentResolver.resolveNavigationResult(any())).thenReturn(Single.just(NavigationResult.create(target, action)));

        navigationDelegate.listenToNavigation().subscribeWith(new NavigationDelegate.Observer(feedbackController));

        navigationDelegate.navigateTo(target);

        verify(action).run();
        verify(intentResolver).resolveNavigationResult(target);
        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackController).showFeedback(feedbackCaptor.capture());
        assertThat(feedbackCaptor.getValue().getMessage()).isEqualTo(R.string.error_unknown_navigation);
    }

    @Test
    public void crashesOnResolverError() throws Exception {
        NavigationTarget target = NavigationTarget.forNavigation(activity, "target", Optional.absent(), Screen.DISCOVER);
        IOException exception = new IOException();
        when(intentResolver.resolveNavigationResult(any())).thenReturn(Single.error(exception));

        navigationDelegate.listenToNavigation().subscribeWith(new NavigationDelegate.Observer(feedbackController));
        expectedException.expect(NullPointerException.class);
        expectedException.expectCause(allOf(instanceOf(IllegalStateException.class),
                                            hasCause(equalTo(exception)),
                                            hasMessage(containsString("Complete in Navigation Subscription. This should never happen since navigation won\'t work in the app anymore. Thus we\'ll force close the app."))));
        navigationDelegate.navigateTo(target);
    }

    @Test
    public void crashOnComplete() throws Exception {
        PublishSubject<NavigationResult> testSubject = PublishSubject.create();

        testSubject.subscribeWith(new NavigationDelegate.Observer(feedbackController));
        expectedException.expect(IllegalStateException.class);
        testSubject.onComplete();
    }
}
