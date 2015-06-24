package com.soundcloud.android.onboarding.suggestions;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;
import com.tobedevoured.modelcitizen.callback.FieldCallback;

@Blueprint(SuggestedUser.class)
public class SuggestedUserBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new SuggestedUser("soundcloud:users:" + runningId++);
        }
    };

    @Default
    FieldCallback username = new FieldCallback() {
        @Override
        public Object get(Object referenceModel) {
            return "user" + ((SuggestedUser) referenceModel).getId();
        }
    };

    @Default
    String city = "Berlin";

    @Default
    String country = "Germany";

    @Default
    String token = "gUgTzUrZoWnuuDVHdrJNoCyGAls2fW3BWDdYfUxCPFMI4tGe3RiP4/7j9x7f\\nOVKNVtHd3BIXWzlwonKt5oAjMw==\\n";
}
