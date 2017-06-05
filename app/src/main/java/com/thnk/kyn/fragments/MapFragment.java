package com.thnk.kyn.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapFragment extends SupportMapFragment implements OnMapReadyCallback
{
    private GoogleMap mMap;
    private float mZoom;
    Map<String, Double> mLatitudeMap = new HashMap<>();
    Map<String, Double> mLongitudeMap = new HashMap<>();
    public ArrayList<LatLng> mLatLngList = new ArrayList<>();
    DatabaseReference mLongitudeRef;
    DatabaseReference mLatitudeRef;

    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        getMapAsync(this);
        mZoom = 15;
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        mLongitudeRef = rootRef.child("longitude");
        mLatitudeRef = rootRef.child("latitude");
    }

    public void addLocation(LatLng latLng)
    {
        String key = mLatitudeRef.push().getKey();
        mLatitudeRef.child(key).setValue(latLng.latitude);
        mLongitudeRef.child(key).setValue(latLng.longitude);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        mMap.setMyLocationEnabled(true);
        goToLatLng(12.98053, 77.6485067);

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener()
        {
            @Override
            public void onCameraIdle()
            {
                refreshTheMarkers();
            }
        });

        ((OnMapReadyCallback) getActivity()).onMapReady(googleMap);
    }

    public void setZoom(float zoom)
    {
        mZoom = zoom;
        if (mMap != null)
        {
            goToLatLng(mMap.getCameraPosition().target);
        }
    }

    public void goToLatLng(double lat, double lng)
    {
        LatLng latLng = new LatLng(lat, lng);
        goToLatLng(latLng);
    }

    public void goToLatLng(LatLng latLng)
    {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mZoom));
    }


    public void refreshTheMarkers()
    {
        final LatLngBounds curScreen = mMap.getProjection()
                .getVisibleRegion().latLngBounds;

        final Query latQuery = mLatitudeRef.orderByValue().startAt(curScreen.southwest.latitude)
                .endAt(curScreen.northeast.latitude).limitToFirst(100);

        latQuery.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                mLatitudeMap.clear();
                for (DataSnapshot child : dataSnapshot.getChildren())
                {
                    mLatitudeMap.put(child.getKey(), (Double) child.getValue());
                }
                latQuery.removeEventListener(this);
                updateLongitudeMap();
            }

            private void updateLongitudeMap()
            {
                final Query lngQuery = mLongitudeRef.orderByValue().startAt(curScreen.southwest.longitude)
                        .endAt(curScreen.northeast.longitude);
                lngQuery.addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        mLongitudeMap.clear();
                        for (DataSnapshot child : dataSnapshot.getChildren())
                        {
                            Log.d("ParsingTest", "Lng Child key : " + child.getKey() + ", Value : " + child.getValue());
                            mLongitudeMap.put(child.getKey(), (Double) child.getValue());
                        }

                        mLatLngList.clear();
                        for (Map.Entry<String, Double> entry : mLongitudeMap.entrySet())
                        {
                            if (mLatitudeMap.containsKey(entry.getKey()))
                            {
                                LatLng latLng = new LatLng(mLatitudeMap.get(entry.getKey()), entry.getValue());
                                mLatLngList.add(latLng);
                            }
                        }
                        lngQuery.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {
                        lngQuery.removeEventListener(this);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                latQuery.removeEventListener(this);
            }
        });
    }
}
