package com.soundcloud.android.suggestedcreators;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreator;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.optional.Optional;
import io.reactivex.observers.TestObserver;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;

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
    private final long NOW = 1;

    @Before
    public void setup() {
        suggestedCreatorsStorage = new SuggestedCreatorsStorage(propellerRxV2(), propeller(), new TestDateProvider(NOW));
    }

    @Test
    public void returnsSuggestedCreators() {
        final ApiSuggestedCreator apiSuggestedCreator = SuggestedCreatorsFixtures.createApiSuggestedCreator();
        testFixtures().insertSuggestedCreator(apiSuggestedCreator);

        final TestObserver<List<SuggestedCreator>> subscriber = suggestedCreatorsStorage.suggestedCreators().test().assertValueCount(1);
        List<SuggestedCreator> actual = subscriber.values().get(0);
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

        final TestObserver<List<SuggestedCreator>> subscriber = suggestedCreatorsStorage.suggestedCreators().test().assertValueCount(1);
        List<SuggestedCreator> actual = subscriber.values().get(0);
        SuggestedCreator suggestedCreator = actual.get(0);
        assertThat(suggestedCreator.getCreator().urn()).isEqualTo(apiSuggestedCreator.getSeedUser().getUrn());
        assertThat(suggestedCreator.followedAt()).is(PRESENT);
        assertThat(suggestedCreator.followedAt().get().getTime()).isEqualTo(NOW);
    }

    @Test
    public void clearsAllDataFromTheTable() {
        final ApiSuggestedCreator apiSuggestedCreator = SuggestedCreatorsFixtures.createApiSuggestedCreator();
        testFixtures().insertSuggestedCreator(apiSuggestedCreator);

        suggestedCreatorsStorage.clear();

        databaseAssertions().assertNoSuggestedCreators();
    }
}
