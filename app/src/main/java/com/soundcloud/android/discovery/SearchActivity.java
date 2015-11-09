package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.utils.TransitionUtils;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SearchActivity extends ScActivity {

    @Inject BaseLayoutHelper layoutHelper;

    @Inject @LightCycle SearchPresenter presenter;
    @Inject @LightCycle PlayerController playerController;

    @Override
    protected void setActivityContentView() {
        layoutHelper.createActionBarLayout(this, R.layout.search);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setBgClickHandler();
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            dismiss();
            return true;
        }
        return false;
    }

    private void setBgClickHandler() {
        findViewById(R.id.search_screen_bg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void dismiss() {
        if (TransitionUtils.transitionsSupported()){
            ((ViewGroup) findViewById(R.id.toolbar_id)).getChildAt(1)
                    .animate()
                    .alpha(0)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            supportFinishAfterTransition();
                        }
                    }).start();
        } else {
            supportFinishAfterTransition();
        }
    }
}
