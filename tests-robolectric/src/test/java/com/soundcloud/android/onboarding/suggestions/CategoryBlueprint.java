package com.soundcloud.android.onboarding.suggestions;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.MappedList;

import java.util.List;

@Blueprint(Category.class)
public class CategoryBlueprint {

    @MappedList(force = true, target = SuggestedUser.class, size = 3)
    List<SuggestedUser> users;

}
