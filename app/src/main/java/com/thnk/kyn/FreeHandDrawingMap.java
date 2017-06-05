package com.thnk.kyn;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.thnk.kyn.fragments.MapFragment;

import java.util.ArrayList;

import static com.thnk.kyn.R.id.btn_add_loc;
import static com.thnk.kyn.R.id.btn_cancel_draw;
import static com.thnk.kyn.R.id.btn_draw_State;

public class FreeHandDrawingMap extends AppCompatActivity implements OnMapReadyCallback
{
    int callCount = 0;
    Boolean mIsMapMovable = false; // to detect map is movable
    private GoogleMap mMap;
    private ArrayList<LatLng> mLatlngList;
    private View mDrawBtn;
    private View mCancelDrawBtn;
    private int mPrevEvent;
    private MapFragment mMapFragment;
    private View mAddLocBtn;
    private LatLng mSelectedLatLng;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_hand_drawing_map);
        mDrawBtn = findViewById(btn_draw_State);
        mCancelDrawBtn = findViewById(btn_cancel_draw);
        mAddLocBtn = findViewById(btn_add_loc);
        mMapFragment = new MapFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.map, mMapFragment).commit();
        mLatlngList = new ArrayList<>();
        loadDataToFireBase();
    }

    private void loadDataToFireBase()
    {
        /*DatabaseReference latitudeRef = FirebaseDatabase.getInstance().getReference().child("testNumSort");
        //DatabaseReference longitudeRef = FirebaseDatabase.getInstance().getReference().child("longitude");
        ArrayList<Integer> lat = new ArrayList<>();
        //ArrayList<Double> lng = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            lat.add((int) (Math.random()*10));
            //lng.add(aLatLngData[1]);
        }
        latitudeRef.setValue(lat);
        //longitudeRef.setValue(lng);*/
    }

    private void setDrawLayerTouchListener()
    {
        Log.d("MotionEv", "setDrawLayerTouchListener");
        findViewById(R.id.fram_map).setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                Log.d("MotionEv", "mIsMapMovable : " + mIsMapMovable);
                if (mIsMapMovable)
                {
                    handleFreeHandDrawing(event);
                }
                return mIsMapMovable;
            }
        });
    }

    private void handleFreeHandDrawing(MotionEvent event)
    {
        float x = event.getX();
        float y = event.getY();

        int x_co = Math.round(x);
        int y_co = Math.round(y);

        Point x_y_points = new Point(x_co, y_co);

        LatLng latLng = mMap.getProjection().fromScreenLocation(x_y_points);
        double latitude = latLng.latitude;

        double longitude = latLng.longitude;

        int eventAction = event.getAction();

        switch (eventAction)
        {
            case MotionEvent.ACTION_DOWN:
                // finger touches the screen
                mLatlngList.clear();
                mMap.clear();
                callCount = 0;
                mLatlngList.add(new LatLng(latitude, longitude));
                Log.d("MotionEv", "ACTION_DOWN ");

            case MotionEvent.ACTION_MOVE:
                mLatlngList.add(new LatLng(latitude, longitude));
                Log.d("MotionEv", "ACTION_MOVE ");
            case MotionEvent.ACTION_UP:
                callCount++;
                drawMap(eventAction);
                break;
        }
        mPrevEvent = eventAction;
    }

    private void setDrawButtonClickListener()
    {
        mDrawBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mIsMapMovable = true;
                mDrawBtn.setVisibility(View.GONE);
                mCancelDrawBtn.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setCancelDrawButtonClickListener()
    {
        mCancelDrawBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mMap.clear();
                mLatlngList.clear();
                mIsMapMovable = false;
                mDrawBtn.setVisibility(View.VISIBLE);
                mCancelDrawBtn.setVisibility(View.GONE);
            }
        });
    }

    public void drawMap(int event)
    {
        PolygonOptions rectOptions = new PolygonOptions();
        rectOptions.addAll(mLatlngList);
        rectOptions.strokeColor(Color.parseColor("#10d75a4a"));
        rectOptions.strokeWidth(5);
        rectOptions.fillColor(Color.TRANSPARENT);
        mMap.addPolygon(rectOptions);
        if(mPrevEvent == MotionEvent.ACTION_UP)
        {
            loadMarkers();
        }
    }

    public void loadMarkers()
    {
        Log.d("loadMarkers", "count" + mLatlngList.size());
        mHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {

                if (mLatlngList.size() > 0)
                {
                    mLatlngList.add(mLatlngList.get(0));
                }
                int count = 0;
                for (LatLng point : mMapFragment.mLatLngList)
                {
                    if (isPointInPolygon(point, mLatlngList))
                    {
                        addMarker(point);
                        count++;
                    }
                }
                Log.d("loadMarkers", "count : " + callCount + ", Size : " + mLatlngList.size());
                if (count <= 0)
                {
                    Toast.makeText(FreeHandDrawingMap.this, "No Match Found!!", Toast.LENGTH_SHORT).show();
                }
            }
        }, 1000);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        setDrawLayerTouchListener();
        setDrawButtonClickListener();
        setCancelDrawButtonClickListener();
        setAddLocationBtnClickListener();
        setOnLongPressListener();
        setOnMarkerDragListener();
    }

    private void setOnMarkerDragListener()
    {
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener()
        {
            @Override
            public void onMarkerDragStart(Marker marker)
            {

            }

            @Override
            public void onMarkerDrag(Marker marker)
            {

            }

            @Override
            public void onMarkerDragEnd(Marker marker)
            {
                mSelectedLatLng = marker.getPosition();
            }
        });
    }

    private void setOnLongPressListener()
    {
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener()
        {
            @Override
            public void onMapLongClick(LatLng latLng)
            {
                mAddLocBtn.setVisibility(View.VISIBLE);
                mCancelDrawBtn.setVisibility(View.GONE);
                mDrawBtn.setVisibility(View.GONE);
                mIsMapMovable = false;

                mSelectedLatLng = latLng;
                MarkerOptions options = new MarkerOptions().position(latLng).draggable(true);
                mMap.addMarker(options);
            }
        });
    }

    private void setAddLocationBtnClickListener()
    {
        mAddLocBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mAddLocBtn.setVisibility(View.GONE);
                mMapFragment.addLocation(mSelectedLatLng);
                mCancelDrawBtn.setVisibility(View.GONE);
                mDrawBtn.setVisibility(View.VISIBLE);
                Toast.makeText(FreeHandDrawingMap.this, "Saved!", Toast.LENGTH_SHORT).show();
                mMap.clear();
                mMapFragment.refreshTheMarkers();
            }
        });
    }

    private boolean isPointInPolygon(LatLng tap, ArrayList<LatLng> vertices)
    {
        int intersectCount = 0;
        for (int j = 0; j < vertices.size() - 1; j++)
        {
            if (rayCastIntersect(tap, vertices.get(j), vertices.get(j + 1)))
            {
                intersectCount++;
            }
        }

        return ((intersectCount % 2) == 1); // odd = inside, even = outside;
    }

    private boolean rayCastIntersect(LatLng tap, LatLng vertA, LatLng vertB)
    {
        double aY = vertA.latitude;
        double bY = vertB.latitude;
        double aX = vertA.longitude;
        double bX = vertB.longitude;
        double pY = tap.latitude;
        double pX = tap.longitude;

        if ((aY > pY && bY > pY) || (aY < pY && bY < pY)
                || (aX < pX && bX < pX))
        {
            return false; // a and b can't both be above or below pt.y, and a or
            // b must be east of pt.x
        }

        double m = (aY - bY) / (aX - bX); // Rise over run
        double bee = (-aX) * m + aY; // y = mx + b
        double x = (pY - bee) / m; // algebra is neat!

        return x > pX;
    }

    public void addMarker(LatLng latLng)
    {
        MarkerOptions options = new MarkerOptions().position(latLng);
        mMap.addMarker(options);
    }


}
