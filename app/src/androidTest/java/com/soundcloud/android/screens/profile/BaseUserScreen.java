package com.soundcloud.android.screens.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;

public abstract class BaseUserScreen extends Screen {
    public BaseUserScreen(Han testDriver) {
        super(testDriver);
    }

    protected RecyclerViewElement playableRecyclerView() {
        return testDriver
                .findOnScreenElement(With.id(R.id.ak_recycler_view))
                .toRecyclerView();
    }
}
