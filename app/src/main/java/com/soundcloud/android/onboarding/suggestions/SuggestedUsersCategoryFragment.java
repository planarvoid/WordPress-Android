package com.soundcloud.android.onboarding.suggestions;

import static rx.android.observables.AndroidObservable.fromFragment;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.view.GridViewCompat;
import rx.Observable;
import rx.android.RxFragmentObserver;
import rx.subscriptions.CompositeSubscription;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import javax.inject.Inject;
import java.util.Set;

@SuppressLint("ValidFragment")
public class SuggestedUsersCategoryFragment extends Fragment implements AdapterView.OnItemClickListener {

    private SuggestedUsersAdapter adapter;
    private Category category;
    private GridViewCompat adapterView;
    private final CompositeSubscription subscription = new CompositeSubscription();

    @Inject ImageOperations imageOperations;
    @Inject FollowingOperations followingOperations;

    public SuggestedUsersCategoryFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    SuggestedUsersCategoryFragment(FollowingOperations followingOperations) {
        this.followingOperations = followingOperations;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(Category.EXTRA)) {
            category = getArguments().getParcelable(Category.EXTRA);
        } else {
            category = Category.empty();
        }
        setAdapter(new SuggestedUsersAdapter(category.getUsers(), imageOperations));

    }

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

    public void setAdapter(SuggestedUsersAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_user_grid, container, false);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // for gridviewcompat setChoiceMode and setItemChecked
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapterView = (GridViewCompat) view.findViewById(R.id.suggested_users_grid);
        adapterView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        adapterView.setOnItemClickListener(this);
        adapterView.setAdapter(adapter);

        for (int i = 0; i < adapter.getCount(); i++) {
            adapterView.setItemChecked(i, followingOperations.getFollowedUserIds().contains(adapter.getItemId(i)));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        subscription.add(fromFragment(this, followingOperations.toggleFollowingBySuggestedUser(adapter.getItem(position)))
                .subscribe(new ToggleFollowingObserver(this)));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // for gridview setItemChecked
    public void toggleFollowings(final boolean shouldFollow) {
        for (int i = 0; i < adapter.getCount(); i++) {
            adapterView.setItemChecked(i, shouldFollow);
        }

        final Set<Long> followedUserIds = followingOperations.getFollowedUserIds();
        Observable<UserAssociation> toggleFollowings;
        if (shouldFollow) {
            toggleFollowings = followingOperations.addFollowingsBySuggestedUsers(category.getNotFollowedUsers(followedUserIds));
        } else {
            toggleFollowings = followingOperations.removeFollowingsBySuggestedUsers(category.getFollowedUsers(followedUserIds));
        }
        subscription.add(fromFragment(this, toggleFollowings).subscribe(new ToggleAllObserver(this)));
    }

    private static final class ToggleFollowingObserver extends RxFragmentObserver<SuggestedUsersCategoryFragment, UserAssociation> {
        public ToggleFollowingObserver(SuggestedUsersCategoryFragment fragment) {
            super(fragment);
        }

        @Override
        public void onCompleted(SuggestedUsersCategoryFragment fragment) {
            fragment.getActivity().supportInvalidateOptionsMenu();
        }

        @Override
        public void onError(SuggestedUsersCategoryFragment fragment, Throwable error) {
            error.printStackTrace();
            fragment.adapter.notifyDataSetChanged();
        }
    }

    private static final class ToggleAllObserver extends RxFragmentObserver<SuggestedUsersCategoryFragment, UserAssociation> {
        public ToggleAllObserver(SuggestedUsersCategoryFragment fragment) {
            super(fragment);
        }

        @Override
        public void onError(SuggestedUsersCategoryFragment fragment, Throwable error) {
            error.printStackTrace();
            fragment.adapter.notifyDataSetChanged();
        }
    }
}
