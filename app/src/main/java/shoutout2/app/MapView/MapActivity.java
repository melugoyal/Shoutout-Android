package shoutout2.app.MapView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import shoutout2.app.Utils.FirebaseUtils;
import shoutout2.app.MessageNumCircle;
import shoutout2.app.Permissions;
import shoutout2.app.Person;
import shoutout2.app.R;
import shoutout2.app.Utils.Utils;


public class MapActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    public Map<String, Marker> markers;
    public Map<String, Person> people;
    protected GoogleApiClient mLocationClient;
    protected MessageNumCircle messageNumCircle;
    private MapFragment mapFragment;
    private static final int UPDATE_INTERVAL = 5000; // milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Permissions(this);
        setContentView(R.layout.map_activity);

        createMapFragment();

        markers = new HashMap<>();
        people = new HashMap<>();
        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        new FirebaseUtils(this);
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

    public void startMessageTo(String userId) {
        mapFragment.updateStatus("@" + people.get(userId).username + " ");
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
                    marker.setMarker(new BitmapDrawable(getResources(), Utils.viewToBitmap(markerView)));
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
        marker.closeToolTip();
        mapFragment.map.removeMarker(marker);
        mapFragment.map.invalidate();
    }

    @Override
    public void onLocationChanged(Location mCurrentLocation) {
        if (ParseUser.getCurrentUser() != null) {
            ParseUser.getCurrentUser().put("geo", new ParseGeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
            ParseUser.getCurrentUser().saveInBackground();
            FirebaseUtils.updateLocation(mCurrentLocation);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
        FirebaseUtils.setOnline(true);
        ParseUser.getCurrentUser().put("online", true);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        if (ParseUser.getCurrentUser() != null) {
            FirebaseUtils.setOnline(false);
            ParseUser.getCurrentUser().put("online", false);
        }
        super.onStop();
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
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
        if (Permissions.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            doLocationStuff();
        }
    }

    protected void doLocationStuff() {
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(UPDATE_INTERVAL);
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);

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
                                                ParseUser user = objectList.get(i);
                                                people.put(user.getObjectId(), new Person(user));
                                                mapFragment.getIcons(geoloc, user);
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
    }

    private void createMapFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        mapFragment = (MapFragment) fragmentManager.findFragmentByTag(MapFragment.TAG);

        if (mapFragment == null) {
            mapFragment = new MapFragment();
        }

        Utils.addFragment(fragmentManager, R.id.map_activity_container, MapFragment.TAG, mapFragment);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("google maps", "GoogleApiClient connection has been suspend");
    }

    /*
    * Called by Location Services if the attempt to
    * Location Services fails.
    */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        1);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            AlertDialog alertDialog = new AlertDialog.Builder(MapActivity.this).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage(Integer.toString(connectionResult.getErrorCode()));
            alertDialog.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == Permissions.permissionInts.get("location")) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                doLocationStuff();
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
