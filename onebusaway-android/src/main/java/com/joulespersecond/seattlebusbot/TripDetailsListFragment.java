package com.joulespersecond.seattlebusbot;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.joulespersecond.oba.ObaApi;
import com.joulespersecond.oba.elements.ObaReferences;
import com.joulespersecond.oba.elements.ObaStop;
import com.joulespersecond.oba.elements.ObaTripSchedule;
import com.joulespersecond.oba.elements.ObaTripStatus;
import com.joulespersecond.oba.provider.ObaContract;
import com.joulespersecond.oba.request.ObaTripDetailsRequest;
import com.joulespersecond.oba.request.ObaTripDetailsResponse;
import com.joulespersecond.seattlebusbot.util.UIHelp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ben on 8/15/2015.
 */
public class TripDetailsListFragment extends ListFragment {

    private static final String TAG = "TripDetailsListFragment";

    private static final int TRIP_DETAILS_LOADER = 0;

    private String mTripId;

    private String mStopId;

    private ObaTripDetailsResponse mTripInfo;

    private ListAdapter mAdapater;

    private final TripDetailsLoaderCallback mTripDetailsCallback = new TripDetailsLoaderCallback();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Start out with a progress indicator.
        setListShown(false);

        // Get the tripID from the "uri" segment
        Uri uri = (Uri) getArguments().getParcelable(FragmentUtils.URI);
        if (uri == null) {
            Log.e(TAG, "No URI in arguments");
            return;
        }

        extractTripIdAndStopId(uri);

        getLoaderManager().initLoader(TRIP_DETAILS_LOADER, null, mTripDetailsCallback);
    }

    private void extractTripIdAndStopId(Uri uri) {
        // get the segments after 'trips'
        List<String> segments = uri.getPathSegments();
        int offsetFromTripsSegment = -1;
        for (String segment : segments) {
            if (offsetFromTripsSegment == -1) {
                if (segment.equals(ObaContract.Trips.PATH)) {
                    offsetFromTripsSegment = 0;
                }
            } else {
                offsetFromTripsSegment++;
                if (offsetFromTripsSegment == 1) {
                    mTripId = segment;
                } else if (offsetFromTripsSegment == 2) {
                    mStopId = segment;
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup root, Bundle savedInstanceState) {
        if (root == null) {
            // Currently in a layout without a container, so no
            // reason to create our view.
            return null;
        }
        return inflater.inflate(R.layout.trip_details, null);
    }

    private void setTripDetails(ObaTripDetailsResponse data) {
        mTripInfo = data;

        final int code = mTripInfo.getCode();
        if (code == ObaApi.OBA_OK) {
            setEmptyText("");
        } else {
            setEmptyText(getString(UIHelp.getRouteErrorString(getActivity(), code)));
        }

        mAdapater = new SimpleAdapter(getActivity(),
                convertTripDetailsToListOfMaps(mTripInfo),
                R.layout.trip_details_listitem,
                new String[]{"name", "direction", "time"},
                new int[]{R.id.stop_id, R.id.direction, R.id.time});

        setListAdapater(mAdapater);
    }

    private List<Map<String, String>> convertTripDetailsToListOfMaps(ObaTripDetailsResponse details) {
        ObaTripSchedule schedule = details.getSchedule();
        ObaReferences references = details.getRefs();
        ObaTripStatus status = details.getStatus();
        ObaTripSchedule.StopTime[] stopTimes = schedule.getStopTimes();

        List<Map<String, String>> listContents = new ArrayList<>(stopTimes.length);

        for (ObaTripSchedule.StopTime stopTime : stopTimes) {
            ObaStop stop = references.getStop(stopTime.getStopId());
            Map<String, String> stopInfo = new HashMap<>(3);

            stopInfo.put("name", stop.getName());
            stopInfo.put("direction", stop.getName());  // getActivity().getString(UIHelp.getStopDirectionText(stop.getDirection())));
            stopInfo.put("time", DateUtils.formatDateTime(getActivity(),
                    status.getServiceDate() + stopTime.getArrivalTime() * 1000,
                    DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_NO_NOON |
                            DateUtils.FORMAT_NO_MIDNIGHT
            ));

            listContents.add(stopInfo);
        }

        return listContents;
    }

    public void setListAdapater(ListAdapter adapater) {
        ListView list = getListView();

        if (list != null) {
            list.setAdapter(adapater);
            setListShown(true);
        }
    }

    private final class TripDetailsLoaderCallback
            implements LoaderManager.LoaderCallbacks<ObaTripDetailsResponse> {

        @Override
        public Loader<ObaTripDetailsResponse> onCreateLoader(int id, Bundle args) {
            return new TripDetailsLoader(getActivity(), mTripId);
        }

        @Override
        public void onLoadFinished(Loader<ObaTripDetailsResponse> loader,
                                   ObaTripDetailsResponse data) {
            setTripDetails(data);
        }

        @Override
        public void onLoaderReset(Loader<ObaTripDetailsResponse> loader) {
            // Nothing to do right here...
        }
    }

    private final static class TripDetailsLoader extends AsyncTaskLoader<ObaTripDetailsResponse> {

        private final String mTripId;

        TripDetailsLoader(Context context, String tripId) {
            super(context);
            mTripId = tripId;
        }

        @Override
        public void onStartLoading() {
            forceLoad();
        }

        @Override
        public ObaTripDetailsResponse loadInBackground() {
            return ObaTripDetailsRequest.newRequest(getContext(), mTripId).call();
        }
    }
}
