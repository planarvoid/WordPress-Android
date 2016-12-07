package com.soundcloud.android.view.adapters;

import com.soundcloud.android.view.adapters.CardEngagementsPresenter.CardEngagementClickListener;

public interface CardViewHolder {

    void showLikeStats(String countString, boolean liked);

    void showRepostStats(String countString, boolean reposted);

    void showDuration(String duration);

    void showGenre(String genre);

    void setEngagementClickListener(CardEngagementClickListener cardEngagementClickListener);

    void hideRepostStats();
}
