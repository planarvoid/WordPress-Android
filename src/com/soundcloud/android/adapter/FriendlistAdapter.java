
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Friend;
import com.soundcloud.android.objects.User;

import android.os.Parcelable;

import java.util.ArrayList;

public class FriendlistAdapter extends UserlistAdapter {

    public static final String TAG = "FriendlistAdapter";

    public FriendlistAdapter(ScActivity context, ArrayList<Parcelable> data, Class<?> model) {
        super(context, data, model);
    }

    @Override
    public User getUserAt(int index) {
        return ((Friend) getItem(index)).user;
    }
}
