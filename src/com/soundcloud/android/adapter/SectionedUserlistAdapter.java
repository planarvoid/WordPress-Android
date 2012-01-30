package com.soundcloud.android.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistSectionedRow;

public class SectionedUserlistAdapter extends SectionedAdapter implements IUserlistAdapter, FollowStatus.Listener {
    public SectionedUserlistAdapter(Context context) {
        super(context);
        FollowStatus.get().requestUserFollowings(SoundCloudApplication.fromContext(context), this, false);
    }

    public User getUserAt(int index) {
        Object obj = getItem(index);
        if (obj instanceof Friend) {
            return ((Friend) obj).user;
        } else if (obj instanceof User) {
            return (User) obj;
        } else {
            throw new IllegalStateException("Unknown row type found: " + obj + " at " + index);
        }
    }

    @Override public void onChange(boolean success, FollowStatus status) {
        notifyDataSetChanged();
    }

    @Override protected LazyRow createRow(int position) {
        return new UserlistSectionedRow(mContext, this);
    }
}
