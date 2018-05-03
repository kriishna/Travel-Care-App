package com.project.minor.travelcare;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.HashMap;

public class GetDirectionsData extends AsyncTask<Object, String, String> {

    private String duration, distance;
    private GoogleMap mMap;
    private String googleDirectionsData;

    @Override
    protected String doInBackground(Object... objects) {
        mMap = (GoogleMap) objects[0];
        String url = (String) objects[1];
        LatLng latLng = (LatLng) objects[2];

        DownloadUrl downloadUrl = new DownloadUrl();
        try {
            googleDirectionsData = downloadUrl.readUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return googleDirectionsData;
    }

    @Override
    protected void onPostExecute(String s) {
        DataParser parser = new DataParser();
        String[] directionsList = parser.parseDirections(s);
        HashMap<String, String> distanceList = parser.parseDistance(s);
        duration = distanceList.get("duration");
        distance = distanceList.get("distance");

        Log.v("Distance: ", distance);
        Log.v("Duration: ", duration);
        setDistance(distance);
        setDuration(duration);
        displayDirection(directionsList);
    }


    public String getDistance() {
        return distance;
    }

    private void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDuration() {
        return duration;
    }

    private void setDuration(String duration) {
        this.duration = duration;
    }


    // In Google Maps, the latitude and longitude coordinates that define
    // a polyline or polygon are stored as an encoded string
    private void displayDirection(String[] directionsList) {
        for (String aDirectionsList : directionsList) {
            PolylineOptions options = new PolylineOptions();
            options.color(Color.GRAY);
            options.width(10);
            // The PolyUtil is useful for converting encoded polylines
            // and polygons to latitude/longitude coordinates, and vice versa.
            options.addAll(PolyUtil.decode(aDirectionsList));
            mMap.addPolyline(options);
        }
    }


}
