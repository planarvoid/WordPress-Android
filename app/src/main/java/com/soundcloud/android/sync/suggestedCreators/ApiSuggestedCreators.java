package com.soundcloud.android.sync.suggestedCreators;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.List;

public class ApiSuggestedCreators extends ModelCollection<ApiSuggestedCreatorItem> {
    public ApiSuggestedCreators() {
    }

    @VisibleForTesting
    public ApiSuggestedCreators(List<ApiSuggestedCreatorItem> apiSuggestedCreatorItems) {
        super(apiSuggestedCreatorItems);
    }

    public Iterable<ApiUser> getAllUsers() {
        final List<ApiSuggestedCreatorItem> collection = getCollection();
        final List<ApiUser> apiUsers = new ArrayList<>();

        for (ApiSuggestedCreatorItem apiSuggestedCreatorItem : collection) {
            final Optional<ApiSuggestedCreator> suggestedCreator = apiSuggestedCreatorItem.getSuggestedCreator();

            if (suggestedCreator.isPresent()) {
                apiUsers.add(suggestedCreator.get().getSeedUser());
                apiUsers.add(suggestedCreator.get().getSuggestedUser());
            }
        }

        return apiUsers;
    }
}
