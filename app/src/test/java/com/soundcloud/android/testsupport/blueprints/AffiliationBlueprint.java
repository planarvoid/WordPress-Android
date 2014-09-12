package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.activities.AffiliationActivity;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;

import java.util.Date;

@Blueprint(AffiliationActivity.class)
public class AffiliationBlueprint {

    @Mapped
    PublicApiUser user;

    @Default()
    Date createdAt = new Date();

}
