package com.soundcloud.android.suggestedcreators;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreator;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.optional.Optional;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Date;
import java.util.List;

public class SuggestedCreatorsStorageTest extends StorageIntegrationTest {

    private static final Condition<Optional<Date>> PRESENT = new Condition<Optional<Date>>() {
        @Override
        public boolean matches(Optional<Date> value) {
            return value.isPresent();
        }
    };
    private SuggestedCreatorsStorage suggestedCreatorsStorage;
    private TestSubscriber<List<SuggestedCreator>> subscriber;
    private final long NOW = 1;
    private TestDateProvider dateProvider;

    @Before
    public void setup() {
        dateProvider = new TestDateProvider(NOW);
        suggestedCreatorsStorage = new SuggestedCreatorsStorage(propellerRx(), dateProvider);
        subscriber = new TestSubscriber<>();
    }

    @Test
    public void returnsSuggestedCreators() {
        final ApiSuggestedCreator apiSuggestedCreator = SuggestedCreatorsFixtures.createApiSuggestedCreator();
        testFixtures().insertSuggestedCreator(apiSuggestedCreator);

        suggestedCreatorsStorage.suggestedCreators().subscribe(subscriber);

        subscriber.assertValueCount(1);
        List<SuggestedCreator> actual = subscriber.getOnNextEvents().get(0);
        SuggestedCreator suggestedCreator = actual.get(0);
        assertThat(suggestedCreator.getCreator().urn()).isEqualTo(apiSuggestedCreator.getSeedUser().getUrn());
        assertThat(suggestedCreator.getRelation().value()).isEqualTo(apiSuggestedCreator.getRelationKey());
        assertThat(suggestedCreator.followedAt()).isNot(PRESENT);
    }

    @Test
    public void updatesSuggestedCreatorFollowedAt() {
        final ApiSuggestedCreator apiSuggestedCreator = SuggestedCreatorsFixtures.createApiSuggestedCreator();
        testFixtures().insertSuggestedCreator(apiSuggestedCreator);

        suggestedCreatorsStorage.toggleFollowSuggestedCreator(apiSuggestedCreator.getSuggestedUser().getUrn(), true)
                                .subscribe();

        suggestedCreatorsStorage.suggestedCreators().subscribe(this.subscriber);

        this.subscriber.assertValueCount(1);
        List<SuggestedCreator> actual = this.subscriber.getOnNextEvents().get(0);
        SuggestedCreator suggestedCreator = actual.get(0);
        assertThat(suggestedCreator.getCreator().urn()).isEqualTo(apiSuggestedCreator.getSeedUser().getUrn());
        assertThat(suggestedCreator.followedAt()).is(PRESENT);
        assertThat(suggestedCreator.followedAt().get().getTime()).isEqualTo(NOW);
    }
}
