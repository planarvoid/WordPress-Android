package com.soundcloud.android.adapter;

import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Friend;
import com.soundcloud.android.objects.User;

import java.util.ArrayList;

public class FriendFinderAdapter extends UserlistAdapter {

    public static final String TAG = "FriendFinderAdapter";

    public FriendFinderAdapter(ScActivity context, ArrayList<Parcelable> data, Class<?> model) {
        super(context, data, model);
    }

    @Override
    public User getUserAt(int index) {
        Log.i(TAG, "GET USER AT " + index + " " + getItem(index));
        if (getItem(index) instanceof Friend) return ((Friend) getItem(index)).user;
        return super.getUserAt(index);
    }
}
