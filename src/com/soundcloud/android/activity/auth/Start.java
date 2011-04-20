package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.adapter.StartMenuAdapter;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.api.Token;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.RelativeLayout;

public class Start extends AccountAuthenticatorActivity {
    RelativeLayout animationHolder;
    RelativeLayout startMenu;
    Button facebookBtn;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_start);

        animationHolder = ((RelativeLayout) this.findViewById(R.id.animation_holder));
        startMenu = (RelativeLayout) animationHolder.findViewById(R.id.start_menu);
        facebookBtn = (Button) this.findViewById(R.id.facebook_btn);
        facebookBtn.setVisibility(View.INVISIBLE);
        facebookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Start.this, Facebook.class), 0);
            }
        });

        final GridView gv = (GridView) this.findViewById(R.id.grid_view);
        gv.setCacheColorHint(0);
        gv.setAdapter(new StartMenuAdapter(this));
        gv.setBackgroundColor(getResources().getColor(R.color.white));
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivityForResult(new Intent(Start.this, (position == 0 ? Login.class : SignUp.class)), 0);
            }
        });
        mHandler.postDelayed(mShowAuthControls, 600);
        animationHolder.removeView(startMenu);
    }

    private Runnable mShowAuthControls = new Runnable() {
        public void run() {
            AnimUtils.setLayoutAnim_slideupfrombottom(animationHolder, Start.this);
            animationHolder.addView(startMenu);
            mHandler.postDelayed(mShowFacebook, 500);
        }
    };

    private Runnable mShowFacebook = new Runnable() {
        public void run() {
            AnimUtils.runFadeInAnimationOn(Start.this, facebookBtn);
            facebookBtn.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data + ")");
        if (resultCode == RESULT_OK) {
            SoundCloudApplication app = (SoundCloudApplication) getApplication();

            User user = data.getParcelableExtra("user");
            Token token = (Token) data.getSerializableExtra("token");

            if (app.addUserAccount(user, token)) {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
                setAccountAuthenticatorResult(result);

                finish();
            } else {
                //showError
            }
        }
    }
}
