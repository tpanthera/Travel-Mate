package io.github.project_travel_mate.travel;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.project_travel_mate.R;
import objects.MapItem;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import utils.GPSTracker;

import static utils.Constants.HERE_API_APP_CODE;
import static utils.Constants.HERE_API_APP_ID;
import static utils.Constants.HERE_API_LINK;
import static utils.Constants.HERE_API_MODES;

/**
 * Show markers on map around user's current location
 */
public class MapRealTimeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final List<MapItem> mMapItems = new ArrayList<>();
    @BindView(R.id.data)
    ScrollView scrollView;
    private int mIndex = 0;
    private Handler mHandler;
    private String mCurlat;
    private String mCurlon;
    private GoogleMap mGoogleMap;
    private static final int REQUEST_LOCATION = 199;
    GPSTracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_realtime);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mHandler = new Handler(Looper.getMainLooper());

        scrollView.setVisibility(View.GONE);

        setTitle("Places");

        // Get user's current location
        tracker = new GPSTracker(this);
        if (!tracker.canGetLocation()) {
            tracker.displayLocationRequest(this);
        } else {
            mCurlat = Double.toString(tracker.getLatitude());
            mCurlon = Double.toString(tracker.getLongitude());
            getMarkers("eat-drink", R.drawable.ic_local_pizza_black_24dp);
        }

        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Calls API to get nearby places
     *
     * @param mode mode; type of places;
     * @param icon marker icon
     */
    private void getMarkers(String mode, final int icon) {

        String uri = HERE_API_LINK + "?at=" + mCurlat + "," + mCurlon + "&cat=" + mode
                + "&app_id=" + HERE_API_APP_ID + "&app_code=" + HERE_API_APP_CODE;

        Log.v("EXECUTING", uri);

        //Set up client
        OkHttpClient client = new OkHttpClient();
        //Execute request
        Request request = new Request.Builder()
                .url(uri)
                .build();
        //Setup callback
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Request Failed", "Message : " + e.getMessage());
            }

            @Override
            public void onResponse(final Call call, final Response response) throws IOException {

                final String res = Objects.requireNonNull(response.body()).string();
                mHandler.post(() -> {
                    try {
                        JSONObject json = new JSONObject(res);
                        json = json.getJSONObject("results");
                        JSONArray routeArray = json.getJSONArray("items");

                        for (int i = 0; i < routeArray.length(); i++) {
                            String name = routeArray.getJSONObject(i).getString("title");
                            String web = routeArray.getJSONObject(i).getString("href");
                            String nums = routeArray.getJSONObject(i).getString("distance");
                            String addr = routeArray.getJSONObject(i).getString("vicinity");

                            Double latitude = Double.parseDouble(
                                    routeArray.getJSONObject(i).getJSONArray("position").get(0).toString());
                            Double longitude = Double.parseDouble(
                                    routeArray.getJSONObject(i).getJSONArray("position").get(1).toString());

                            showMarker(latitude, longitude, name, icon);
                            mMapItems.add(new MapItem(name, nums, web, addr));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("ERROR : ", e.getMessage() + " ");
                    }
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        if (item.getItemId() == R.id.action_sort) {

            new MaterialDialog.Builder(this)
                    .title(R.string.title)
                    .items(R.array.items)
                    .itemsCallbackMultiChoice(null, (dialog, which, text) -> {

                        mGoogleMap.clear();
                        mMapItems.clear();

                        for (int i = 0; i < which.length; i++) {
                            Log.v("selected", which[i] + " " + text[i]);
                            Integer icon;
                            switch (which[0]) {
                                case 0:
                                    icon = R.drawable.ic_local_pizza_black_24dp;
                                    break;
                                case 1:
                                    icon = R.drawable.ic_local_bar_black_24dp;
                                    break;
                                case 2:
                                    icon = R.drawable.ic_camera_alt_black_24dp;
                                    break;
                                case 3:
                                    icon = R.drawable.ic_directions_bus_black_24dp;
                                    break;
                                case 4:
                                    icon = R.drawable.ic_local_mall_black_24dp;
                                    break;
                                case 5:
                                    icon = R.drawable.ic_local_gas_station_black_24dp;
                                    break;
                                case 6:
                                    icon = R.drawable.ic_local_atm_black_24dp;
                                    break;
                                case 7:
                                    icon = R.drawable.ic_local_hospital_black_24dp;
                                    break;
                                default:
                                    icon = R.drawable.ic_attach_money_black_24dp;
                                    break;
                            }
                            MapRealTimeActivity.this.getMarkers(HERE_API_MODES.get(which[0]), icon);

                        }
                        return true;
                    })
                    .positiveText(R.string.choose)
                    .show();

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets marker at given location on map
     *
     * @param locationLat  latitude
     * @param locationLong longitude
     * @param locationName name of location
     * @param locationIcon icon
     */
    private void showMarker(Double locationLat, Double locationLong, String locationName, Integer locationIcon) {
        LatLng coord = new LatLng(locationLat, locationLong);

        if (ContextCompat.checkSelfPermission(MapRealTimeActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mGoogleMap != null) {
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 10));

                MarkerOptions abc = new MarkerOptions();
                MarkerOptions x = abc
                        .title(locationName)
                        .position(coord)
                        .icon(BitmapDescriptorFactory.fromResource(locationIcon));
                mGoogleMap.addMarker(x);

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap map) {

        mGoogleMap = map;

        if (mCurlat == null || mCurlon == null)
            return;

        // Zoom to current location
        LatLng coordinate = new LatLng(Double.parseDouble(mCurlat), Double.parseDouble(mCurlon));
        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 10);
        map.animateCamera(yourLocation);

        map.setOnMarkerClickListener(marker -> {
            scrollView.setVisibility(View.VISIBLE);
            for (int i = 0; i < mMapItems.size(); i++) {
                if (mMapItems.get(i).getName().equals(marker.getTitle())) {
                    mIndex = i;
                    break;
                }
            }

            TextView title = MapRealTimeActivity.this.findViewById(R.id.item_title);
            TextView description = MapRealTimeActivity.this.findViewById(R.id.item_description);
            final Button calls, book;
            calls = MapRealTimeActivity.this.findViewById(R.id.call);
            book = MapRealTimeActivity.this.findViewById(R.id.book);

            title.setText(mMapItems.get(mIndex).getName());
            description.setText(mMapItems.get(mIndex).getAddress());
            calls.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + mMapItems.get(mIndex).getNumber()));
                MapRealTimeActivity.this.startActivity(intent);

            });
            book.setOnClickListener(view -> {
                Intent browserIntent;
                try {
                    browserIntent = new Intent(
                            Intent.ACTION_VIEW, Uri.parse(mMapItems.get(mIndex).getAddress()));
                    MapRealTimeActivity.this.startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(MapRealTimeActivity.this,
                            R.string.no_activity_for_browser, Toast.LENGTH_LONG).show();
                }
            });
            return false;
        });
    }

    public static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, MapRealTimeActivity.class);
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        //User agreed to make required location settings changes
                        //startLocationUpdates();
                        Toast.makeText(getApplicationContext(),
                                R.string.location_enabled, Toast.LENGTH_LONG).show();
                        mCurlat = Double.toString(tracker.getLatitude());
                        mCurlon = Double.toString(tracker.getLongitude());
                        getMarkers("eat-drink", R.drawable.ic_local_pizza_black_24dp);

                        break;
                    case Activity.RESULT_CANCELED:
                        //User chose not to make required location settings changes
                        Toast.makeText(getApplicationContext(),
                                R.string.location_not_enabled, Toast.LENGTH_LONG).show();
                        break;
                }
                break;
        }
    }
}
