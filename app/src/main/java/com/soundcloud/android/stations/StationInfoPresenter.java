package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationInfo.FROM_STATION_RECORD;
import static com.soundcloud.android.stations.StationInfoTracksBucket.FROM_TRACK_ITEM_LIST;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import rx.Observable;
import rx.functions.Func2;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class StationInfoPresenter extends RecyclerViewPresenter<List<StationInfoItem>, StationInfoItem> {

    private static Func2<StationInfoItem, StationInfoItem, List<StationInfoItem>> COMBINE_STATION_ITEMS =
            new Func2<StationInfoItem, StationInfoItem, List<StationInfoItem>>() {
                @Override
                public List<StationInfoItem> call(StationInfoItem header,
                                                  StationInfoItem trackList) {
                    return Arrays.asList(header, trackList);
                }
            };

    private final StationTrackOperations stationTrackOperations;
    private final StationsOperations stationOperations;
    private final StationInfoAdapter adapter;

    @Inject
    public StationInfoPresenter(StationTrackOperations stationsTrackOperations,
                                SwipeRefreshAttacher swipeRefreshAttacher,
                                StationInfoAdapter adapter, StationsOperations stationOperations) {
        super(swipeRefreshAttacher);
        this.stationTrackOperations = stationsTrackOperations;
        this.stationOperations = stationOperations;
        this.adapter = adapter;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);

        getBinding().connect();
    }

    @Override
    protected CollectionBinding<List<StationInfoItem>, StationInfoItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn stationUrn = fragmentArgs.getParcelable(StationInfoFragment.EXTRA_URN);

        return CollectionBinding.from(getStationInfo(stationUrn)
                                              .zipWith(getStationTracks(stationUrn), COMBINE_STATION_ITEMS))
                                .withAdapter(adapter).build();
    }

    @NonNull
    private Observable<StationInfoItem> getStationInfo(Urn stationUrn) {
        return stationOperations.station(stationUrn).map(FROM_STATION_RECORD);
    }

    private Observable<StationInfoItem> getStationTracks(Urn stationUrn) {
        return stationTrackOperations.initialStationTracks(stationUrn).map(FROM_TRACK_ITEM_LIST);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

}
