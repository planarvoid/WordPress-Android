package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.suggestedcreators.SuggestedCreatorsFixtures.createApiSuggestedCreatorItem;

import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreatorItem;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreators;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.Lists;
import org.junit.Before;
import org.junit.Test;

public class StoreSuggestedCreatorsCommandTest extends StorageIntegrationTest {

    private StoreSuggestedCreatorsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new StoreSuggestedCreatorsCommand(propeller(), new StoreUsersCommand(propeller()));
    }

    @Test
    public void shouldStoreSuggestedCreator() {
        final ApiSuggestedCreatorItem suggestedCreator = createApiSuggestedCreatorItem();
        final ApiSuggestedCreators suggestedCreators = new ApiSuggestedCreators(Lists.newArrayList(suggestedCreator));

        command.call(suggestedCreators);

        databaseAssertions().assertSuggestedCreatorInserted(suggestedCreator.getSuggestedCreator().get());
    }

    @Test
    public void shouldDeleteOldSuggestedCreator() {
        final ApiSuggestedCreatorItem oldSuggestedCreator = createApiSuggestedCreatorItem();
        final ApiSuggestedCreators oldSuggestedCreators = new ApiSuggestedCreators(Lists.newArrayList(oldSuggestedCreator));

        command.call(oldSuggestedCreators);

        final ApiSuggestedCreatorItem newSuggestedCreator = createApiSuggestedCreatorItem();
        final ApiSuggestedCreators newSuggestedCreators = new ApiSuggestedCreators(Lists.newArrayList(newSuggestedCreator));

        command.call(newSuggestedCreators);

        databaseAssertions().assertSuggestedCreatorRemoved(oldSuggestedCreator.getSuggestedCreator().get());
        databaseAssertions().assertSuggestedCreatorInserted(newSuggestedCreator.getSuggestedCreator().get());
    }
}
