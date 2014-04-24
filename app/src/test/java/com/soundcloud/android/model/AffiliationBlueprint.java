package com.soundcloud.android.model;

import com.soundcloud.android.model.activities.AffiliationActivity;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;

import java.util.Date;

@Blueprint(AffiliationActivity.class)
public class AffiliationBlueprint {

    @Mapped
    User user;

    @Default()
    Date createdAt = new Date();

}
