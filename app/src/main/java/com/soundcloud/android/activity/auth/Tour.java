package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.view.tour.Comment;
import com.soundcloud.android.view.tour.Finish;
import com.soundcloud.android.view.tour.Follow;
import com.soundcloud.android.view.tour.Record;
import com.soundcloud.android.view.tour.Share;
import com.soundcloud.android.view.tour.Start;
import com.soundcloud.android.view.tour.TourLayout;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;


public class Tour extends ScActivity {
    private ViewPager mViewPager;
    private View[] mViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tour);

        mViewPager = (ViewPager) findViewById(R.id.tour_view);
        mViews = new View[]{new Start(this), new Record(this), new Share(this), new Follow(this), new Comment(this), new Finish(this)};
        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return mViews.length;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View v = mViews[position];
                v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.FILL_PARENT));
                container.addView(v);
                return v;
            }

            @Override
            public void destroyItem(View collection, int position, Object view) {
                ((ViewPager) collection).removeView((View) view);
            }


            @Override
            public boolean isViewFromObject(View view, Object object) {
                return object == view;
            }
        });


        mViewPager.setCurrentItem(0);

        final Button btnDone = (Button) findViewById(R.id.btn_done);
        final Button btnSkip = (Button) findViewById(R.id.btn_done_dark);

        final View.OnClickListener done = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ObjectEquality
                ((SoundCloudApplication) getApplication()).track(
                        view == btnDone ? Click.Tour_Tour_done : Click.Tour_Tour_skip);
                finish();
            }
        };

        btnDone.setOnClickListener(done);
        btnSkip.setOnClickListener(done);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                ((RadioButton) ((RadioGroup) findViewById(R.id.rdo_tour_step)).getChildAt(i)).setChecked(true);
                if (i < mViews.length - 1) {
                    btnDone.setVisibility(View.GONE);
                    btnSkip.setVisibility(View.VISIBLE);
                } else {
                    btnDone.setVisibility(View.VISIBLE);
                    btnSkip.setVisibility(View.GONE);
                }
                ((SoundCloudApplication) getApplication()).track(mViews[mViewPager.getCurrentItem()].getClass());
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }

    /* package */ String getMessage() {
        return getActiveTour().getMessage().toString();
    }

    private TourLayout getActiveTour() {
        return (TourLayout) mViewPager.getChildAt(mViewPager.getCurrentItem());
    }

}
