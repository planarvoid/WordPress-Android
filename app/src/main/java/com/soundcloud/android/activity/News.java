package com.soundcloud.android.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.ScListView;
import com.viewpagerindicator.TabPageIndicator;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class News extends ScListActivity {

    private MainFragmentAdapter mAdapter;
    private ViewPager mPager;
    private TabPageIndicator mIndicator;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.simple_tabs);

        mAdapter = new MainFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setBackgroundColor(Color.WHITE);

        mIndicator = (TabPageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
    }

    class MainFragmentAdapter extends FragmentPagerAdapter {
            protected final Content[] contents = new Content[]{Content.ME_SOUND_STREAM, Content.ME_FAVORITES, Content.ME_ACTIVITIES};
            protected final int[] titleIds = new int[]{R.string.tab_title_my_sound_stream,R.string.tab_title_my_likes, R.string.tab_activity};

            public MainFragmentAdapter(FragmentManager fm) {
                super(fm);
            }

            @Override
            public ScListFragment getItem(int position) {
                return ScListFragment.newInstance(contents[position]);
            }

            @Override
            public int getCount() {
                return contents.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return getResources().getString(titleIds[position]);
            }
        }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        menu.add(menu.size(), Consts.OptionsMenu.FILTER, 0, R.string.menu_stream_setting).setIcon(
                R.drawable.ic_menu_incoming);
        return true;
    }

    @Override
        public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, Dashboard.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.menu_record:
                // app icon in action bar clicked; go home
                intent = new Intent(this, ScCreate.class);
                startActivity(intent);
                return true;
            case R.id.menu_search:
                // app icon in action bar clicked; go home
                intent = new Intent(this, ScSearch.class);
                startActivity(intent);
                return true;
            case R.id.menu_you:
                // app icon in action bar clicked; go home
                intent = new Intent(this, UserBrowser.class);
                startActivity(intent);
                return true;
            case Consts.OptionsMenu.FILTER:
                track(Page.Stream_stream_setting, getApp().getLoggedInUser());
                track(Click.Stream_main_stream_setting);

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.dashboard_filter_title))
                        .setNegativeButton(R.string.dashboard_filter_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                track(Click.Stream_box_stream_cancel);
                            }
                        })
                        .setItems(new String[]{
                                getString(R.string.dashboard_filter_all),
                                getString(R.string.dashboard_filter_exclusive)
                        },
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final boolean exclusive = which == 1;
                                        /*
                                        SharedPreferencesUtils.apply(PreferenceManager
                                                .getDefaultSharedPreferences(Dashboard.this)
                                                .edit()
                                                .putBoolean(Consts.PrefKeys.EXCLUSIVE_ONLY_KEY, exclusive));

                                        ((EventsAdapterWrapper) mListView.getWrapper()).setContent(exclusive ?
                                                Content.ME_EXCLUSIVE_STREAM : Content.ME_SOUND_STREAM);

                                        mListView.getWrapper().reset();
                                        mListView.getRefreshableView().invalidateViews();
                                        mListView.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mListView.getWrapper().onRefresh();
                                            }
                                        });

                                        track(exclusive ? Click.Stream_box_stream_only_Exclusive
                                                : Click.Stream_box_stream_all_tracks);
                                    }
                                })
                        .create()
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
*/
}
