package com.flaker.flaker;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.directions.route.AbstractRouting;
import com.directions.route.Routing;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;


public class MainActivity extends MapsActivity {

    // Google Map
    private LatLng placeLatLng;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    // Activity View State
    /*

    */
    private String viewState;

    private final String TAG = this.toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupOnMapReadyCallback();
        includeDrawer();
    }


    private void setupAutoCompleteWidget() {
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                this.getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                placeLatLng = place.getLatLng();
                moveMapToLatLngWithBounds(placeLatLng, true);
                drawRoute(mLastKnownLatLng, placeLatLng, Routing.TravelMode.WALKING);
                createSingleMarker(placeLatLng);
                viewState = "confirmDestination";
                updateUI(viewState);
                requestLocationUpdates();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error for autocomplete
                Log.d(TAG, "An error occurred: " + status);
            }
        });
    }

    private void setupOnMapReadyCallback() {
        final Context context = this;
        FragmentManager mFragmentManager = this.getSupportFragmentManager();
        SupportMapFragment mapFragment =
                (SupportMapFragment) mFragmentManager.findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
//                try {
//                    // Customise the styling of the base map using a JSON object defined
//                    // in a raw resource file.
//                    boolean success = googleMap.setMapStyle(
//                            MapStyleOptions.loadRawResourceStyle(
//                                    context, R.raw.map_json));
//                    if (!success) {
//                        Log.e(TAG, "Style parsing failed.");
//                    }
//                } catch (Resources.NotFoundException e) {
//                    Log.e(TAG, "Can't find style. Error: ", e);
//                }
                mGoogleMap = googleMap;
                getLocationPermission();
                getDeviceLocation();
                updateLocationUI();
                setupAutoCompleteWidget();
            }
        });
    }

    private void updateUI(String viewState) {
        switch (viewState) {
            case "searchDestination":
                break;
            case "confirmDestination":
                ValueAnimator animation = ValueAnimator.ofFloat(1.0f, 0.7f);

                final Guideline guideLine = (Guideline) this.findViewById(R.id.guideline);
                final ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();

                animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                        float animatedValue = (float)updatedAnimation.getAnimatedValue();
                        params.guidePercent = animatedValue; // 45% // range: 0 <-> 1
                        guideLine.setLayoutParams(params);
                    }
                });
                animation.setDuration(1300);
                animation.start();

                ConstraintLayout autoCompleteLayout = this.findViewById(R.id.place_autocomplete_layout);
                autoCompleteLayout.setVisibility(ConstraintLayout.GONE);

                // Display the ETA on the confirm box

                Log.d("ETA", "TRYING TO CHANGE");



                // Change icon to Arrow back
//                this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_black);//your icon here
                break;
            default:
                break;
        }
    }

    @SuppressLint("MissingPermission")

    private void requestLocationUpdates() {
        mLocationRequest = new LocationRequest();

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);

        // Construct a location callback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Log.d("BRUCE", "GOT A NEW LOCATION");
                    mLastKnownLatLng = new LatLng(location.getLatitude(),location.getLongitude());
                    drawRoute(mLastKnownLatLng, placeLatLng, Routing.TravelMode.WALKING);

                }
            };
        };

        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }


    public void changeTravelMode(View view) {

        Integer viewId = view.getId();

        switch (viewId) {
            case R.id.requesterWalkButton:
                Log.d(TAG, "*** CHOSE TO WALK ***");
                travelMode = Routing.TravelMode.WALKING;

                break;
            case R.id.requesterBikeButton:
                Log.d(TAG, "*** CHOSE TO BIKE ***");
                travelMode = Routing.TravelMode.BIKING;
                break;
            case R.id.requesterCarButton:
                Log.d(TAG, "*** CHOSE TO DRIVE ***");
                travelMode = Routing.TravelMode.DRIVING;
                break;
            default:
                break;
        }
        Log.d(TAG, viewId.toString());
    }
}


