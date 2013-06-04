package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.User;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.MappedList;

import java.util.List;

@Blueprint(Category.class)
public class CategoryBlueprint {

    @MappedList(target = User.class, size = 3)
    List<User> users;

}
