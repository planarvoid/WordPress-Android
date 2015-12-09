package com.soundcloud.android.view.adapters;

import com.soundcloud.android.view.adapters.CardEngagementsPresenter.CardEngagementClickListener;

public interface CardViewHolder {

    void showLikeStats(String countString, boolean liked);

    void showRepostStats(String countString, boolean reposted);

    void setEngagementClickListener(CardEngagementClickListener cardEngagementClickListener);
}
