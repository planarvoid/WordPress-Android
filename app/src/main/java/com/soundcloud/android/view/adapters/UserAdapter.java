package com.soundcloud.android.view.adapters;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.associations.ToggleFollowSubscriber;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.model.UserHolder;
import com.soundcloud.android.model.UserProperty;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.propeller.PropertySet;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Temporarily used to adapt ScListFragment lists that use public API models to PropertySets and the new cell design
 */
public class UserAdapter extends ScBaseAdapter<ScResource> implements FollowingOperations.FollowStatusChangedListener, UserItemPresenter.OnToggleFollowListener {

    @Inject UserItemPresenter presenter;
    @Inject FollowingOperations followingOperations;

    private final List<PropertySet> users = new ArrayList<PropertySet>(Consts.LIST_PAGE_SIZE);

    public UserAdapter(Uri uri) {
        super(uri);
        SoundCloudApplication.getObjectGraph().inject(this);
        init();
    }

    @VisibleForTesting
    UserAdapter(Uri uri, UserItemPresenter userItemPresenter,
                 FollowingOperations followingOperations) {
        super(uri);
        this.presenter = userItemPresenter;
        this.followingOperations = followingOperations;
        init();
    }

    private void init() {
        followingOperations.requestUserFollowings(this);
        presenter.setToggleFollowListener(this);
    }

    @Override
    public void onToggleFollowClicked(int position, ToggleButton toggleButton) {
        followingOperations.toggleFollowing(((UserHolder) getItem(position)).getUser())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ToggleFollowSubscriber(toggleButton));
    }

    @Override
    protected View createRow(Context context, int position, ViewGroup parent) {
        return presenter.createItemView(position, parent);
    }

    @Override
    protected void bindRow(int position, View rowView) {
        presenter.bindItemView(position, rowView, users);
    }

    @Override
    public void addItems(List<ScResource> newItems) {
        super.addItems(newItems);
        this.users.addAll(toPropertySets(newItems));
    }

    @Override
    public void clearData() {
        super.clearData();
        users.clear();
    }

    public void updateItems(Map<Urn, ScResource> updatedItems){
        for (int i = 0; i < users.size(); i++) {
            final Urn key = users.get(i).get(UserProperty.URN);
            if (updatedItems.containsKey(key)){
                users.set(i, ((User) updatedItems.get(key)).toPropertySet());
            }
        }
        notifyDataSetChanged();
    }

    private List<PropertySet> toPropertySets(List<ScResource> items) {
        List<PropertySet> newItems = new ArrayList<PropertySet>(items.size());
        for (ScResource item : items) {
            final User user = getUser(item);
            newItems.add(user
                    .toPropertySet()
                    .put(UserProperty.IS_FOLLOWED_BY_ME, followingOperations.isFollowing(user.getUrn())));
        }
        return newItems;
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER, getUser(getItem(position))));
        return ItemClickResults.LEAVING;
    }

    private User getUser(ScResource item) {
        return item instanceof UserAssociation ? ((UserAssociation) item).getUser() : (User) item;
    }

    @Override
    public void onFollowChanged() {
        notifyDataSetChanged();
    }
}
