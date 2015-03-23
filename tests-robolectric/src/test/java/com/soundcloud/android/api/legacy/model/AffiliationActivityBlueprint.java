package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.api.legacy.model.activities.AffiliationActivity;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;

import java.util.Date;

@Blueprint(AffiliationActivity.class)
public class AffiliationActivityBlueprint {

    @Mapped
    PublicApiUser user;

    @Default()
    Date createdAt = new Date();

}
