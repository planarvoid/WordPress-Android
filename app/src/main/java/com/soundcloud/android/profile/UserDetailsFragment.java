package com.soundcloud.android.profile;

import static android.text.TextUtils.isEmpty;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.EmptyViewBuilder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class UserDetailsFragment extends Fragment {

    private long userId;
    private EmptyViewBuilder emptyViewFactory;
    private int emptyViewStatus = EmptyListView.Status.WAITING;
    private boolean displayedInfo;

    public static UserDetailsFragment newInstance(long userId) {
        UserDetailsFragment fragment = new UserDetailsFragment();
        Bundle args = new Bundle();
        args.putLong("userId", userId);
        fragment.setArguments(args);
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        emptyViewFactory = new EmptyViewBuilder();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        userId = getArguments().getLong("userId");
        ViewGroup fragmentLayout = (ViewGroup) inflater.inflate(R.layout.user_browser_details_view, null);
        User user = SoundCloudApplication.sModelManager.getUser(userId);
        if (user != null) {
            updateViews(fragmentLayout, user);
        }
        return fragmentLayout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configureEmptyView((ViewGroup) view);
    }

    public void onSuccess(User user) {
        emptyViewStatus = EmptyListView.Status.OK;
        ViewGroup fragmentLayout = (ViewGroup) getView();
        if (fragmentLayout != null) {
            updateViews(fragmentLayout, user);
            configureEmptyView(fragmentLayout);
        }
    }

    public void onError() {
        emptyViewStatus = EmptyListView.Status.ERROR;
        ViewGroup fragmentLayout = (ViewGroup) getView();
        if (fragmentLayout != null) {
            configureEmptyView(fragmentLayout);
        }
    }

    private void updateViews(ViewGroup fragmentLayout, User user) {
        displayedInfo = setupWebsite(fragmentLayout, user)
                | setupDiscogs(fragmentLayout, user)
                | setupMyspace(fragmentLayout, user)
                | setupLocation(fragmentLayout, user)
                | setupDescription(fragmentLayout, user);
    }

    private void configureEmptyView(ViewGroup fragmentLayout) {
        EmptyListView emptyView = (EmptyListView) fragmentLayout.findViewById(android.R.id.empty);
        if (emptyView != null) fragmentLayout.removeView(emptyView);

        if (!displayedInfo) {
            if (emptyViewStatus == EmptyListView.Status.OK) {
                if (userId == SoundCloudApplication.fromContext(getActivity()).getAccountOperations().getLoggedInUserId()) {
                    emptyViewFactory.withMessageText(getString(R.string.info_empty_you_message))
                                     .withSecondaryText(getString(R.string.info_empty_you_secondary))
                                     .withImage(R.drawable.empty_profile);
                } else {
                    emptyViewFactory.withMessageText(getString(R.string.info_empty_other_message))
                                     .withImage(R.drawable.empty_info);
                }
            }
            emptyView = emptyViewFactory.build(getActivity());
            emptyView.setId(android.R.id.empty);
            emptyView.setStatus(emptyViewStatus);

            fragmentLayout.addView(emptyView,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private boolean setupDiscogs(ViewGroup fragmentLayout, final User user) {
        TextView discogsName = (TextView) fragmentLayout.findViewById(R.id.discogs_name);
        if (!isEmpty(user.discogs_name)) {

            discogsName.setMovementMethod(LinkMovementMethod.getInstance());
            discogsName.setVisibility(View.VISIBLE);
            discogsName.setFocusable(true);
            discogsName.setClickable(true);
            discogsName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://www.discogs.com/artist/" + user.discogs_name));
                    startActivity(viewIntent);
                }
            });
            return true;
        } else {
            discogsName.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupDescription(ViewGroup fragmentLayout, User user) {
        TextView description = (TextView) fragmentLayout.findViewById(R.id.description);
        if (!isEmpty(user.description)) {
            description.setVisibility(View.VISIBLE);
            description.setText(ScTextUtils.fromHtml(user.description));
            description.setMovementMethod(LinkMovementMethod.getInstance());
            return true;
        } else {
            description.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupLocation(ViewGroup fragmentLayout, User user) {
        final String location = user.getLocation();
        TextView locationView = (TextView) fragmentLayout.findViewById(R.id.location);
        if (!isEmpty(location)) {
            locationView.setText(getString(R.string.from) + " " + location);
            locationView.setVisibility(View.VISIBLE);
            return true;
        } else {
            locationView.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupWebsite(ViewGroup fragmentLayout, final User user) {
        TextView website = (TextView) fragmentLayout.findViewById(R.id.website);
        if (!isEmpty(user.website)) {
            website.setText(user.getWebSiteTitle());
            website.setVisibility(View.VISIBLE);
            website.setFocusable(true);
            website.setClickable(true);
            website.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.website));
                    startActivity(viewIntent);
                }
            });
            return true;
        } else {
            website.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupMyspace(ViewGroup fragmentLayout, final User user) {
        TextView myspaceName = (TextView) fragmentLayout.findViewById(R.id.myspace_name);
        if (!isEmpty(user.myspace_name)) {
            myspaceName.setMovementMethod(LinkMovementMethod.getInstance());
            myspaceName.setVisibility(View.VISIBLE);
            myspaceName.setFocusable(true);
            myspaceName.setClickable(true);
            myspaceName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent =
                            new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("http://www.myspace.com/" + user.myspace_name));
                    startActivity(viewIntent);
                }
            });
            return true;
        } else {
            myspaceName.setVisibility(View.GONE);
            return false;
        }
    }
}
