package com.soundcloud.android.suggestedcreators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stream.SoundStreamItem;
import com.soundcloud.android.stream.SoundStreamItem.Kind;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;

@RunWith(MockitoJUnitRunner.class)
public class SuggestedCreatorsOperationsTest {
    @Mock private FeatureFlags featureFlags;
    @Mock private MyProfileOperations myProfileOperations;

    private SuggestedCreatorsOperations operations;
    private TestSubscriber<SoundStreamItem> subscriber;

    @Before
    public void setup() {
        operations = new SuggestedCreatorsOperations(featureFlags, myProfileOperations);
        when(featureFlags.isEnabled(Flag.SUGGESTED_CREATORS)).thenReturn(true);
        subscriber = new TestSubscriber<>();
    }

    @Test
    public void returnsNotificationItemIfNumberOfFollowingsLowerEqualThanFive() {
        when(myProfileOperations.numberOfFollowings()).thenReturn(Observable.just(5));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SoundStreamItem notificationItem = subscriber.getOnNextEvents().get(0);

        assertThat(notificationItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void returnsEmptyIfNumberOfFollowingsGreaterThanFive() {
        when(myProfileOperations.numberOfFollowings()).thenReturn(Observable.just(6));

        operations.suggestedCreators().subscribe(subscriber);

        subscriber.assertNoValues();
    }
}
