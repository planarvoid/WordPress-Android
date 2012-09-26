package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.view.ScListView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;


@Tracking(page = Page.Entry_signup__find_friends)
public class SuggestedUsers extends ScListActivity {
    private ScListView mListView;
    private Button facebookBtn;
    private boolean resumedBefore;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.suggested_users);
        /*
        ffAdp = new SectionedAdapter(this);
        ffAdpWrap = new SectionedEndlessAdapter(this, ffAdp, true);
        ffAdpWrap.addListener(this);

        mListView = new SectionedListView(this);
        configureList(mListView);

        LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.suggested_users_header, null, false);
        mListView.getRefreshableView().addHeaderView(header, null, false);

        ((ViewGroup) findViewById(R.id.listHolder)).addView(mListView);

        ffAdpWrap.configureViews(mListView);
        ffAdpWrap.setEmptyViewText(R.string.empty_list);
        mListView.setAdapter(ffAdpWrap,true);

        facebookBtn = (Button) header.findViewById(R.id.facebook_btn);
        facebookBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                configureFacebook();
            }
        });

        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });

        if (getIntent() != null && getIntent().getBooleanExtra(Start.FB_CONNECTED_EXTRA, false)) {
            facebookBtn.setVisibility(View.GONE);
            addFriendsSection();
        }

        addSuggestedUsersSection();

        mPreviousState = (Object[]) getLastCustomNonConfigurationInstance();
        if (mPreviousState != null) {
            mListView.getWrapper().restoreState(mPreviousState);
        }
*/
        // result ok no matter what
        setResult(RESULT_OK);

        // start tour on top so we can preload  users
        startActivity(new Intent(this, Tour.class));
    }

}
