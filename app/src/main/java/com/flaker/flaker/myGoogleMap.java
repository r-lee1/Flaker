package com.flaker.flaker;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.SupportActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bruce on 2/21/18.
 *
 * The myGoogleMap class has the following API
 *
 * moveMapToLatLng(LatLng latLng)
 *
 * Will move the map to the latlng given
 *
 * drawRoute(LatLng start, LatLng end)
 *
 * Will draw a route from the start to the end
 *
 * createSingleMarker(LatLng latLng)
 *
 * Will create a single marker that will be replaced if called again
 *
 *
 */

public class myGoogleMap extends MainActivity {

    // API Clients
    private static GeoDataClient mGeoDataClient;
    private static PlaceDetectionClient mPlaceDetectionClient;
    private static FusedLocationProviderClient mFusedLocationProviderClient;

    // Main Google Map Object
    private static GoogleMap mGoogleMap;

    // Default Map Values
    private static final Integer DEFAULT_ZOOM = 15;
    private static LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085); // Australia

    // Permissions
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static boolean mLocationPermissionGranted = false;

    private static Marker destinationMarker;
    private static LatLng placeLatLng;
    private static List<Polyline> polylines = new ArrayList<>();
    private static LocationRequest mLocationRequest;
    private static LocationCallback mLocationCallback;
    private static Location mLastKnownLocation;
    private static LatLng mLastKnownLatLng;
    private static Boolean currentlyRouting = false;

    public static void init(AppCompatActivity context) {

        setupMapAPIClients(context);
        createLocationCallback();
        setupGoogleMapCallback(context);

    }

    public static void moveMapToLatLngWithBounds(LatLng latLng) {
        Log.d("BRUCE", "MOVING THE MAP");
//        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                new LatLng(mLastKnownLocation.getLatitude(),
//                        mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(mLastKnownLatLng);
        builder.include(latLng);
        LatLngBounds bounds = builder.build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 400);

        mGoogleMap.animateCamera(cameraUpdate);
        Log.d("BRUCE", "MOVING DONE");
    }

    public static void moveMapToLatLng(LatLng latLng) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM);
        mGoogleMap.animateCamera(cameraUpdate);
    }

    public static void drawRoute(LatLng start, LatLng end) {
        Log.d("BRUCE", "YAY");
        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.WALKING)
                .withListener(new RoutingListener() {
                    @Override
                    public void onRoutingFailure(RouteException e) {

                    }

                    @Override
                    public void onRoutingStart() {

                    }

                    @Override
                    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
                        currentlyRouting = true;
                        if(polylines.size() > 0) {
                            for (Polyline poly : polylines) {
                                poly.remove();
                            }
                        }

                        polylines = new ArrayList<>();
                        //add route(s) to the map.
                        for (int i = 0; i < route.size(); i++) {

                            //In case of more than 5 alternative routes


                            PolylineOptions polyOptions = new PolylineOptions();

                            polyOptions.width(10 + i * 3);
                            polyOptions.addAll(route.get(i).getPoints());
                            Polyline polyline = mGoogleMap.addPolyline(polyOptions);
                            polylines.add(polyline);

//                            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onRoutingCancelled() {

                    }
                })
                .waypoints(start, end)
                .build();
        routing.execute();
        Log.d("BRUCE", "FINISHED");
    }



    private static void setupMapAPIClients(AppCompatActivity context) {
        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(context, null);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(context, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    private static void createLocationCallback() {
        // Construct a location callback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    mLastKnownLocation = location;
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//                    moveMapToLatLng(currentLatLng);
                }
            };
        };
    }

    private static void setupGoogleMapCallback(final AppCompatActivity context) {
        FragmentManager mFragmentManager = context.getSupportFragmentManager();
        SupportMapFragment mapFragment =
                (SupportMapFragment) mFragmentManager.findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mGoogleMap = googleMap;
                getLocationPermission(context);
                getDeviceLocation(context);
                updateLocationUI(context);
                setupAutoCompleteWidget(context);
            }
        });
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    public static void getLocationPermission(AppCompatActivity context) {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(context.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(context,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    public static void getDeviceLocation(AppCompatActivity context) {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(context, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            Log.d("BRUCE","SUCCESSFUL THING HAPPENED");
                            mLastKnownLocation = task.getResult();
                            mLastKnownLatLng = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
//                            Log.d("Bruce", mLastKnownLatLng.toString());
                            if(mLastKnownLocation != null) {
                                moveMapToLatLng(mLastKnownLatLng);
                            }
                        } else {
                            Log.d("BRUCE", "ATTEMPTING DEFAULT");
                            LatLng defaultLatLng = new LatLng(mDefaultLocation.latitude, mDefaultLocation.longitude);
                            moveMapToLatLng(defaultLatLng);
                            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    public static void updateLocationUI(AppCompatActivity context) {
        if (mGoogleMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission(context);
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private static void setupAutoCompleteWidget(AppCompatActivity context) {
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                context.getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                placeLatLng = place.getLatLng();
                myGoogleMap.moveMapToLatLngWithBounds(placeLatLng);
                myGoogleMap.drawRoute(mLastKnownLatLng, placeLatLng);
                myGoogleMap.createSingleMarker(placeLatLng);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error for autocomplete
                Log.d("Bruce", "An error occurred: " + status);
            }
        });
    }

    private static void createSingleMarker(LatLng latLng) {

        // If a marker exists already, remove the marker
        if (destinationMarker != null) {
            destinationMarker.remove();
        }

        destinationMarker = mGoogleMap.addMarker(new MarkerOptions().position(latLng));


    }
}