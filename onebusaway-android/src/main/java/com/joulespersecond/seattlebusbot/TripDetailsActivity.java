package com.joulespersecond.seattlebusbot;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.joulespersecond.oba.ObaAnalytics;
import com.joulespersecond.oba.provider.ObaContract;
import com.joulespersecond.seattlebusbot.util.UIHelp;

/**
 * Created by Ben on 8/14/2015.
 */
public class TripDetailsActivity extends SherlockFragmentActivity {

    public static void start(Context context, String tripId, String stopId) {
        context.startActivity(makeIntent(context, tripId, stopId));
    }

    public static Intent makeIntent(Context context, String tripId, String stopId) {
        Intent myIntent = new Intent(context, TripDetailsActivity.class);
        myIntent.setData(ObaContract.Trips.buildUri(tripId, stopId));
        return myIntent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIHelp.setupActionBar(this);

        FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentById(android.R.id.content) == null) {
            TripDetailsListFragment list = new TripDetailsListFragment();
            list.setArguments(FragmentUtils.getIntentArgs(getIntent()));

            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ObaAnalytics.reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
