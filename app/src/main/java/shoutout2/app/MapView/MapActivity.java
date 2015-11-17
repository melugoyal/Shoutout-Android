package shoutout2.app.MapView;

import android.Manifest;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import shoutout2.app.BackgroundLocationService;
import shoutout2.app.MessageNumCircle;
import shoutout2.app.Permissions;
import shoutout2.app.Person;
import shoutout2.app.R;
import shoutout2.app.Utils.FirebaseUtils;
import shoutout2.app.Utils.Utils;

public class MapActivity extends FragmentActivity {

    public Map<String, Marker> markers;
    public Map<String, Person> people;
    protected MessageNumCircle messageNumCircle;
    protected MapFragment mapFragment;
    private static final int UPDATE_INTERVAL = 5000; // milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Permissions(this);
        setContentView(R.layout.map_activity);

        createMapFragment();

        markers = new HashMap<>();
        people = new HashMap<>();
        new FirebaseUtils(this);

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", ParseUser.getCurrentUser());
        installation.saveInBackground();
        ParsePush.subscribeInBackground("global");

        if (Permissions.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            startService(new Intent(this, BackgroundLocationService.class));
        }
    }

    protected void updateMessageButton() {
        final ParseQuery<ParseObject> messageQuery = new ParseQuery<ParseObject>("Messages");
        messageQuery.whereEqualTo("to", ParseUser.getCurrentUser());
        messageQuery.whereEqualTo("read", false);
        messageQuery.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                if (e == null) {
                    messageNumCircle.setCircle(count);
                }
            }
        });
    }

    public void startMessageTo(String username) {
        mapFragment.updateStatus("@" + username + " ");
    }

    public void makeMarkerActive(final String userId) {
        for (Marker marker : markers.values()) {
            if (marker == null) {
                continue;
            }
            if (marker.getTitle().equals(userId) && !marker.inCluster) {
                try {
                    View markerView = people.get(userId).markerView;
                    mapFragment.initInfoWindow(markerView, userId);
                    float ratio =  mapFragment.scale * 1.2f;
                    Bitmap activeMarker = Utils.viewToBitmap(markerView);
                    Bitmap newMarker = Bitmap.createScaledBitmap(activeMarker, (int) (ratio * activeMarker.getWidth()), (int) (ratio * activeMarker.getHeight()), true);
                    marker.setMarker(new BitmapDrawable(getResources(), newMarker));
                    mapFragment.map.selectMarker(marker);
                } catch (Exception e) {
                    Log.e("SETTING MARKER ACTIVE", "error " + e.getLocalizedMessage());
                }
            } else {
                try {
                    marker.setMarker(new BitmapDrawable(getResources(), people.get(marker.getTitle()).scaledInactiveMarker));
                    marker.closeToolTip();
                } catch (Exception e) {
                    Log.e("ERROR CLOSING TOOLTIP", "error " + e.getLocalizedMessage());
                }
            }
        }
    }

    public void changeMarkerOnlineStatus(String userId, boolean online) {
        Person person = people.get(userId);
        if (person == null) {
            return;
        }
        View markerView = person.markerView;
        markerView.findViewById(R.id.onlineIcon).setVisibility(online ? View.VISIBLE : View.GONE);
        markerView.findViewById(R.id.infoWindow).setVisibility(View.GONE);
        person.inactiveMarker = Utils.viewToBitmap(markerView);
    }

    public void hideUserMarkers(String userId) {
        Marker marker = markers.get(userId);
        if (marker != null) {
            marker.closeToolTip();
            mapFragment.map.removeMarker(marker);
            mapFragment.map.invalidate();
        }
    }

    public void showUserMarkers(String userId) {
        Marker marker = markers.get(userId);
        if (marker != null) {
            mapFragment.map.addMarker(marker);
            mapFragment.map.invalidate();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUtils.setOnline(true);
        ParseUser.getCurrentUser().put("online", true);
        ParseUser.getCurrentUser().saveInBackground();
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ParseGeoPoint temploc = null;
                        while (temploc == null) {
                            temploc = ParseUser.getCurrentUser().getParseGeoPoint("geo");
                        }
                        final ParseGeoPoint currloc = temploc;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mapFragment.maplat == 0 && mapFragment.maplong == 0) {
                                    ParseGeoPoint currloc = ParseUser.getCurrentUser().getParseGeoPoint("geo");
                                    mapFragment.map.setCenter(new LatLng(currloc.getLatitude(), currloc.getLongitude()));
                                } else {
                                    mapFragment.map.setCenter(new LatLng(mapFragment.maplat, mapFragment.maplong));
                                }

                                ParseQuery<ParseUser> query = ParseUser.getQuery();
                                query.whereWithinMiles("geo", currloc, 50);
                                query.findInBackground(new FindCallback<ParseUser>() {
                                    public void done(List<ParseUser> objectList, ParseException e) {
                                        if (e == null) {
                                            for (int i = objectList.size() - 1; i >= 0; i--) {
                                                ParseGeoPoint geoloc = objectList.get(i).getParseGeoPoint("geo");
                                                if (geoloc != null) {
                                                    ParseUser user = objectList.get(i);
                                                    people.put(user.getObjectId(), new Person(user));
                                                    mapFragment.getIcons(geoloc, user);
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            th.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (BackgroundLocationService.mLocationRequest != null) {
            BackgroundLocationService.mLocationRequest.setInterval(UPDATE_INTERVAL);
            BackgroundLocationService.mLocationRequest.setFastestInterval(UPDATE_INTERVAL);
        }
    }

    @Override
    public void onBackPressed() {
        updateMessageButton();
        FragmentManager manager = getFragmentManager();
        int count = manager.getBackStackEntryCount();
        if (count == 1 && mapFragment.listViewFragment != null && mapFragment.listViewFragment.isVisible()) {
            findViewById(R.id.list_view_triangle).setVisibility(View.INVISIBLE);
        }
        if (count == 0) {
            finish();
        } else {
            manager.popBackStack();
            if (count == 1 && !ParseUser.getCurrentUser().getBoolean("visible")) { // if we came back to map fragment
                Log.d("init disabled", "init disabled");
                mapFragment.initDisabledView();
            }
        }
    }

    @Override
    protected void onStop() {
        if (ParseUser.getCurrentUser() != null) {
            FirebaseUtils.setOnline(false);
            ParseUser.getCurrentUser().put("online", false);
            ParseUser.getCurrentUser().saveInBackground();
        }
        if (mapFragment != null) {
            mapFragment.map.clear();
        }
        for (Person person : people.values()) {
            try {
                person.inactiveMarker.recycle();
                person.emptyStatusIcon.recycle();
                person.scaledInactiveMarker.recycle();
            } catch (Exception e) {
                Log.e("error clearing person", person.username + " " + e.getLocalizedMessage());
            }
        }
        people.clear();
        BackgroundLocationService.mLocationRequest.setInterval(BackgroundLocationService.UPDATE_INTERVAL);
        BackgroundLocationService.mLocationRequest.setFastestInterval(BackgroundLocationService.UPDATE_INTERVAL);
        super.onStop();
    }

    private void createMapFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        mapFragment = (MapFragment) fragmentManager.findFragmentByTag(MapFragment.TAG);

        if (mapFragment == null) {
            mapFragment = new MapFragment();
        }
        try {
            Utils.addFragment(fragmentManager, R.id.map_activity_container, MapFragment.TAG, mapFragment, false);
        } catch (Exception e) {
            Log.e("add map fragment error", " " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == Permissions.permissionInts.get("location")) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startService(new Intent(this, BackgroundLocationService.class));
            } else {
                Toast.makeText(MapActivity.this, "Please allow Shoutout to access your location.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }
}
