package com.mapmyindia.sdk.demo.java.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapmyindia.sdk.demo.R;
import com.mapmyindia.sdk.demo.java.adapter.PoiAlongAdapter;
import com.mapmyindia.sdk.demo.java.plugin.DirectionPolylinePlugin;
import com.mapmyindia.sdk.demo.java.utils.CheckInternet;
import com.mapmyindia.sdk.demo.java.utils.InputFilterMinMax;
import com.mapmyindia.sdk.demo.java.utils.TransparentProgressDialog;
import com.mmi.services.api.alongroute.MapmyIndiaPOIAlongRoute;
import com.mmi.services.api.alongroute.models.POIAlongRouteResponse;
import com.mmi.services.api.alongroute.models.SuggestedPOI;
import com.mmi.services.api.directions.DirectionsCriteria;
import com.mmi.services.api.directions.MapmyIndiaDirections;
import com.mmi.services.api.directions.models.DirectionsResponse;
import com.mmi.services.api.directions.models.DirectionsRoute;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PoiAlongRouteActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapLongClickListener {

    private MapboxMap mapmyIndiaMap;
    private MapView mapView;
    private RecyclerView poiRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private TransparentProgressDialog transparentProgressDialog;
    private String profile = DirectionsCriteria.PROFILE_DRIVING;
    private DirectionPolylinePlugin directionPolylinePlugin;
    private RelativeLayout view;
    private EditText etStartLat, etStartLng, etDestLat, etDestLng, etKeyword;
    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_along_route);
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        initReferences();
        etStartLat = findViewById(R.id.et_start_lat);
        etStartLng = findViewById(R.id.et_start_lng);
        etDestLat = findViewById(R.id.et_dest_lat);
        etDestLng = findViewById(R.id.et_dest_lng);
        etKeyword = findViewById(R.id.et_keyword);
        etStartLng.setFilters(new InputFilter[]{new InputFilterMinMax(-180, 180)});
        etStartLat.setFilters(new InputFilter[]{new InputFilterMinMax(-90, 90)});
        etDestLng.setFilters(new InputFilter[]{new InputFilterMinMax(-180, 180)});
        etDestLat.setFilters(new InputFilter[]{new InputFilterMinMax(-90, 90)});

        view = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(view);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(250);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        Button button = findViewById(R.id.btn_search);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDirections();
            }
        });
        transparentProgressDialog = new TransparentProgressDialog(this, R.drawable.circle_loader, "");

    }


    private void initReferences() {
        mLayoutManager = new LinearLayoutManager(this);
        poiRecyclerView = findViewById(R.id.poiRecyclerview);
        poiRecyclerView.setLayoutManager(mLayoutManager);
//        poiRecyclerView.setVisibility(View.GONE);

    }

    @Override
    public void onMapReady(MapboxMap mapmyIndiaMap) {
        this.mapmyIndiaMap = mapmyIndiaMap;

        mapmyIndiaMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                28.594475, 77.202432), 10));
        findViewById(R.id.details_layout).setVisibility(View.VISIBLE);
        mapmyIndiaMap.addOnMapLongClickListener(this);
        if (CheckInternet.isNetworkAvailable(PoiAlongRouteActivity.this)) {
            Log.v("route", "calling");
            getDirections();
        } else {
            Toast.makeText(this, getString(R.string.pleaseCheckInternetConnection), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapError(int i, String s) {
    }


    private void getDirections() {

        double startLat = 0.0;
        double startLng = 0.0;
        double destLat = 0.0;
        double destLng = 0.0;
        String startLatText = etStartLat.getText().toString();
        String startLngText = etStartLng.getText().toString();
        String destLatText = etDestLat.getText().toString();
        String destLngText = etDestLng.getText().toString();
        String keyword = etKeyword.getText().toString();
        if (TextUtils.isEmpty(startLatText) || TextUtils.isEmpty(startLngText) || TextUtils.isEmpty(destLatText) || TextUtils.isEmpty(destLngText) || TextUtils.isEmpty(keyword)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startLat = Double.parseDouble(startLatText);
            startLng = Double.parseDouble(startLngText);
            destLng = Double.parseDouble(destLngText);
            destLat = Double.parseDouble(destLatText);
        } catch (Exception e) {
            //Igonore
        }
        if (startLat == 0 || startLng == 0 || destLat == 0 || destLng == 0) {
            Toast.makeText(this, "Invalid Coordinates", Toast.LENGTH_SHORT).show();
            return;
        }
        view.setVisibility(View.GONE);
        progressDialogShow();

        MapmyIndiaDirections.builder()
                .origin(Point.fromLngLat(startLng, startLat))
                .destination(Point.fromLngLat(destLng, destLat))
                .profile(profile)
                .resource(DirectionsCriteria.RESOURCE_ROUTE)
                .steps(false)
                .alternatives(false)
                .overview(DirectionsCriteria.OVERVIEW_FULL).build().enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
                if (response.code() == 200) {
                    if (response.body() != null) {
                        DirectionsResponse directionsResponse = response.body();
                        List<DirectionsRoute> results = directionsResponse.routes();

                        if (results.size() > 0) {
                            mapmyIndiaMap.clear();
                            DirectionsRoute directionsRoute = results.get(0);
                            if (directionsRoute != null && directionsRoute.geometry() != null) {

                                drawPath(PolylineUtils.decode(directionsRoute.geometry(), Constants.PRECISION_6));

                                callPOIAlongRoute(directionsRoute.geometry(), keyword);
                            }
                        }
                    }
                } else {
                    Toast.makeText(PoiAlongRouteActivity.this, response.message() + response.code(), Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable t) {
                progressDialogHide();
                t.printStackTrace();

            }
        });


    }


    private void callPOIAlongRoute(String path, String keyword) {
        if (keyword == null) {

            return;
        }
        MapmyIndiaPOIAlongRoute.builder()
                .category(keyword)
                .path(path)
                .buffer(300)
                .build().enqueueCall(new Callback<POIAlongRouteResponse>() {
            @Override
            public void onResponse(Call<POIAlongRouteResponse> call, Response<POIAlongRouteResponse> response) {
                //handle response
                progressDialogHide();
                if (response.code() == 200) {
                    if (response.body() != null) {
                        List<SuggestedPOI> pois = response.body().getSuggestedPOIs();
                        view.setVisibility(View.VISIBLE);
                        addMarker(pois);
                        /*for (SuggestedPOI poi:pois){

                        }*/

                    }
                } else {
                    Toast.makeText(PoiAlongRouteActivity.this, response.message() + response.code(), Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onFailure(Call<POIAlongRouteResponse> call, Throwable t) {
                t.printStackTrace();
                progressDialogHide();

            }
        });

    }

    private void drawPath(@NonNull List<Point> waypoints) {
        if (mapmyIndiaMap == null) {
            return;
        }
        ArrayList<LatLng> listOfLatLng = new ArrayList<>();
        for (Point point : waypoints) {
            listOfLatLng.add(new LatLng(point.latitude(), point.longitude()));
        }

        if (directionPolylinePlugin == null) {
            directionPolylinePlugin = new DirectionPolylinePlugin(mapmyIndiaMap, mapView, profile);
            directionPolylinePlugin.createPolyline(listOfLatLng);
        } else {
            directionPolylinePlugin.updatePolyline(profile, listOfLatLng);

        }
//        mapmyIndiaMap.addPolyline(new PolylineOptions().addAll(listOfLatLng).color(Color.parseColor("#3bb2d0")).width(4));
        LatLngBounds latLngBounds = new LatLngBounds.Builder().includes(listOfLatLng).build();
        mapmyIndiaMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 10, 250, 10, 100));
    }


    private void addMarker(List<SuggestedPOI> pois) {
        for (SuggestedPOI marker : pois) {
            mapmyIndiaMap.addMarker(new MarkerOptions().position(new LatLng(marker.getLatitude(), marker.getLongitude())).title(marker.getPoi()));
        }

        poiRecyclerView.setAdapter(new PoiAlongAdapter(pois));
    }

    /**
     * Show Progress Dialog
     */
    private void progressDialogShow() {
        transparentProgressDialog.show();
    }

    /**
     * Hide Progress dialog
     */
    private void progressDialogHide() {
        transparentProgressDialog.dismiss();
    }


    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Select Point as Source or Destination");

        alertDialog.setPositiveButton("Source", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                etStartLat.setText(String.format("%s", latLng.getLatitude()));
                etStartLng.setText(String.format("%s", latLng.getLongitude()));
                getDirections();
            }
        });
        alertDialog.setNegativeButton("Destination", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                etDestLat.setText(String.format("%s", latLng.getLatitude()));
                etDestLng.setText(String.format("%s", latLng.getLongitude()));
                getDirections();
            }
        });

        alertDialog.setCancelable(true);
        alertDialog.show();
    }
}
