package com.soundcloud.android.view;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.CloudUtils.GraphicsSizes;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.io.IOException;

public class UserlistSectionedRow extends UserlistRow {

    private static final String TAG = "UserlistSectionedRow";

    public UserlistSectionedRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.user_list_item_sectioned;
    }

}
