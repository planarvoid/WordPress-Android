package com.soundcloud.android.stations;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class FetchAndStoreStationsCommand extends Command<List<Urn>, Boolean> {
    private static final String TAG = "FetchStations";

    private final StationsApi api;
    private final StationsStorage storage;

    @Inject
    public FetchAndStoreStationsCommand(StationsApi api, StationsStorage storage) {
        this.api = api;
        this.storage = storage;
    }

    @Override
    public Boolean call(List<Urn> input) {
        try {
            final List<ApiStationMetadata> apiStationMetadatas = api.fetchStations(input);
            storage.storeStationsMetadata(apiStationMetadatas);
            return true;
        } catch (IOException | ApiMapperException | ApiRequestException e) {
            Log.i(TAG, e.getMessage());
            return false;
        }
    }
}
