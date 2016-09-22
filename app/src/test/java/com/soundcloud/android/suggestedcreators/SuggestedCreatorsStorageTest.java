package com.soundcloud.android.suggestedcreators;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreator;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.List;

public class SuggestedCreatorsStorageTest extends StorageIntegrationTest {

    private SuggestedCreatorsStorage suggestedCreatorsStorage;
    private TestSubscriber<List<SuggestedCreator>> subscriber;

    @Before
    public void setup() {
        suggestedCreatorsStorage = new SuggestedCreatorsStorage(propellerRx());
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
    }
}
