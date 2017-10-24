package com.soundcloud.android.suggestedcreators;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreator;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreatorItem;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreators;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SuggestedCreatorsFixtures {
    static ApiSuggestedCreatorItem createApiSuggestedCreatorItem() {
        ApiSuggestedCreator apiSuggestedCreator = createApiSuggestedCreator();
        return ApiSuggestedCreatorItem.create(apiSuggestedCreator);
    }

    static ApiSuggestedCreator createApiSuggestedCreator() {
        final ApiUser seedUser = UserFixtures.apiUser();
        final ApiUser suggestedUser = UserFixtures.apiUser();
        return ApiSuggestedCreator.create(seedUser, suggestedUser, "liked");
    }

    static ApiSuggestedCreators createApiSuggestedCreators() {
        return new ApiSuggestedCreators(Lists.newArrayList(createApiSuggestedCreatorItem()));
    }

    static List<SuggestedCreator> createSuggestedCreators(int count, SuggestedCreatorRelation relation) {
        return createSuggestedCreators(count, relation, null);
    }

    static List<SuggestedCreator> createSuggestedCreators(int count, SuggestedCreatorRelation relation, Date followedAt) {
        final ArrayList<SuggestedCreator> result = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            result.add(SuggestedCreator.create(UserFixtures.userItem(), relation, Optional.fromNullable(followedAt)));
        }
        return result;
    }

    static List<SuggestedCreatorItem> createSuggestedCreatorItems(int count) {
        final List<SuggestedCreatorItem> result = new ArrayList<>(count);

        final List<SuggestedCreator> suggestedCreators = createSuggestedCreators(count, SuggestedCreatorRelation.LIKED);
        for (SuggestedCreator suggestedCreator : suggestedCreators) {
            result.add(SuggestedCreatorItem.fromSuggestedCreator(suggestedCreator));
        }

        return result;
    }
}
