package com.soundcloud.android.activity.tour;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

public class TourActivity extends Activity {
    final static Class<?>[] ORDER = { Record.class, Share.class, Follow.class, Comment.class, You.class };

    protected SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getApp().trackPage("/tour/" + getClass().getSimpleName().toLowerCase());
    }

    protected void init(String title, final int tourIndex) {
        ((TextView) findViewById(R.id.txt_title)).setText(title);
        ((RadioButton) ((RadioGroup) findViewById(R.id.rdo_tour_step)).getChildAt(tourIndex)).setChecked(true);
        findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tourIndex < ORDER.length - 1) {
                    startActivity(new Intent(TourActivity.this, ORDER[tourIndex + 1]));
                } else {
                    startActivity(new Intent(TourActivity.this, Finish.class));
                }
                finish();
            }
        });

        ((RadioGroup) findViewById(R.id.rdo_tour_step)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rdo_tour_rec:
                        startActivity(new Intent(TourActivity.this, Record.class));
                        finish();
                        break;
                    case R.id.rdo_tour_share:
                        startActivity(new Intent(TourActivity.this, Share.class));
                        finish();
                        break;
                    case R.id.rdo_tour_follow:
                        startActivity(new Intent(TourActivity.this, Follow.class));
                        finish();
                        break;
                    case R.id.rdo_tour_comment:
                        startActivity(new Intent(TourActivity.this, Comment.class));
                        finish();
                        break;
                    case R.id.rdo_tour_you:
                        startActivity(new Intent(TourActivity.this, You.class));
                        finish();
                        break;
                }
            }
        });
    }
}
