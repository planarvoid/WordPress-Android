package com.soundcloud.android.fragment;

import static android.text.TextUtils.isEmpty;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyListView;

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

    private long mUserId;
    private EmptyListView mEmptyInfoView;
    private boolean mDisplayedInfo, mInfoError, mAllowEmpty;

    public static UserDetailsFragment newInstance(long userId) {
        UserDetailsFragment fragment = new UserDetailsFragment();
        Bundle args = new Bundle();
        args.putLong("userId", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mUserId = getArguments().getLong("userId");
        ViewGroup fragmentLayout = (ViewGroup) inflater.inflate(R.layout.user_browser_details_view, null);
        User user = SoundCloudApplication.MODEL_MANAGER.getUser(mUserId);
        if (user != null){
            updateViews(fragmentLayout, user);
        }
        configureEmptyView(fragmentLayout);
        return fragmentLayout;
    }

    public void onSuccess(User user) {
        mAllowEmpty = true;
        if (getActivity() != null){
            mInfoError = false;
            ViewGroup fragmentLayout = (ViewGroup) getView();
            if (fragmentLayout != null) {
                updateViews(fragmentLayout, user);
                configureEmptyView(fragmentLayout);
            }
        }
    }

    public void onError() {
        mAllowEmpty = true;
        if (getActivity() != null) {
            mInfoError = true;
            if (!mDisplayedInfo) {
                ViewGroup fragmentLayout = (ViewGroup) getView();
                if (fragmentLayout != null) configureEmptyView(fragmentLayout);
            }
        }
    }

    private void updateViews(ViewGroup fragmentLayout, User user) {
        mDisplayedInfo = setupWebsite(fragmentLayout, user)
                | setupDiscogs(fragmentLayout, user)
                | setupMyspace(fragmentLayout, user)
                | setupLocation(fragmentLayout, user)
                | setupDescription(fragmentLayout, user);
    }

    private void configureEmptyView(ViewGroup fragmentLayout) {
        if (mDisplayedInfo && mEmptyInfoView != null && mEmptyInfoView.getParent() == fragmentLayout) {
            fragmentLayout.removeView(mEmptyInfoView);
        } else if (!mDisplayedInfo) {
            if (mEmptyInfoView == null) {
                mEmptyInfoView = new EmptyListView(getActivity());
            }
            if (!mAllowEmpty) {
                mEmptyInfoView.setStatus(EmptyListView.Status.WAITING);
            } else {
                if (mInfoError) {
                    mEmptyInfoView.setStatus(EmptyListView.Status.ERROR);

                } else {
                    mEmptyInfoView.setStatus(EmptyListView.Status.OK);
                    if (mUserId == SoundCloudApplication.getUserId()) {
                        if (getActivity() != null) {
                            mEmptyInfoView.setMessageText(R.string.info_empty_you_message)
                                    .setActionText(getString(R.string.info_empty_you_action))
                                    .setButtonActions(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/settings")), null);
                        }
                    } else {
                        mEmptyInfoView.setMessageText(R.string.info_empty_other_message)
                                .setActionText(null);
                    }
                    if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                        // won't fit in most landscape views
                        mEmptyInfoView.setImage(-1);
                        mEmptyInfoView.setActionText(null);
                    }
                }


            }

            if (mEmptyInfoView.getParent() != fragmentLayout) {
                if (mEmptyInfoView.getParent() instanceof ViewGroup){
                    ((ViewGroup) mEmptyInfoView.getParent()).removeView(mEmptyInfoView);
                }
                fragmentLayout.addView(mEmptyInfoView,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
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
