package shoutout2.app;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.androidmapsextensions.ClusteringSettings;
import com.androidmapsextensions.GoogleMap;
import com.androidmapsextensions.Marker;
import com.androidmapsextensions.MarkerOptions;
import com.androidmapsextensions.SupportMapFragment;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyMapActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.InfoWindowAdapter {

    private GoogleMap map;
    private Map<String, Marker> markers;
    private Map<String, Person> people;
    private GoogleApiClient mLocationClient;
    private LocationRequest mLocationRequest;
    private static final int UPDATE_INTERVAL = 5000; // milliseconds
    private static final int MIN_ZOOM = 3;
    private final Firebase ref = new Firebase("https://shoutout.firebaseIO.com/");
    private final Firebase refStatus = new Firebase("https://shoutout.firebaseIO.com/status");
    private final Firebase refLoc = new Firebase("https://shoutout.firebaseIO.com/loc");
    private final Firebase refPrivacy = new Firebase("https://shoutout.firebaseIO.com/privacy");
    private final Firebase refOnline = new Firebase("https://shoutout.firebaseIO.com/online");
    private static boolean slideUpVisible = false;
    private static boolean firstTime = false;
    private static float zoomlevel;
    private static double maplat;
    private static double maplong;
    private static boolean firstConnect = true;
    private int SHOUTOUT_SLIDE_OFFSET;
    private static ToggleButton mSwitch;
    private static Button updateStatus;
    private static EditText mEdit;
    private static ImageButton updateShoutButton;
    private static ImageButton mButton;
    private static ImageButton cancelShoutButton;
    private static ImageView changeStatusPin;
    private ListView messagesListView;
    private ImageButton messageButton;
    private LinearLayout settingsView;
    private MessageNumCircle messageNumCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_map);

        new Permissions(this);

        slideUpVisible = false;
        firstTime = false;
        markers = new HashMap<>();
        people = new HashMap<>();
        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getExtendedMap();
        map.setOnMarkerClickListener(this);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMyLocationEnabled(false);
        map.setInfoWindowAdapter(this);
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                makeMarkerActive(""); // make all markers inactive
            }
        });

        ImageButton myLocationButton = (ImageButton) findViewById(R.id.my_location_button);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseGeoPoint currLoc = ParseUser.getCurrentUser().getParseGeoPoint("geo");
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currLoc.getLatitude(), currLoc.getLongitude()), zoomlevel));
                makeMarkerActive(ParseUser.getCurrentUser().getObjectId());
            }
        });
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                zoomlevel = cameraPosition.zoom;
                if (zoomlevel < MIN_ZOOM) {
                    map.animateCamera(CameraUpdateFactory.zoomTo(MIN_ZOOM));
                    zoomlevel = MIN_ZOOM;
                }
                maplat = cameraPosition.target.latitude;
                maplong = cameraPosition.target.longitude;

                String closestMarkerUserId = "";
                try {
                    closestMarkerUserId = findClosestMarker(maplat, maplong);
                } catch (Exception e) {
                    Log.e("err finding closest", "error " + e.getLocalizedMessage());
                }
                makeMarkerActive(closestMarkerUserId);
            }
        });
        map.setClustering(new ClusteringSettings().clusterOptionsProvider(new MapClusteringOptions(getResources())).clusterSize(96.));

        final ParseGeoPoint currloc = ParseUser.getCurrentUser().getParseGeoPoint("geo");
        if (currloc != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currloc.getLatitude(), currloc.getLongitude()), 4));
        }

        messagesListView = (ListView)findViewById(R.id.messages_list);
        initMessageButton();

        settingsView = (LinearLayout) findViewById(R.id.settings_list);

        // wait for map to show everyone's markers
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                public void run() {
                    try {
                        Log.d("PARSEOBJECTID", ParseUser.getCurrentUser().getObjectId());
                        while (people.size() > 0 && !markers.containsKey(ParseUser.getCurrentUser().getObjectId()));
                        handler.post(new Runnable() {
                            public void run() {
                                if (ParseUser.getCurrentUser().getBoolean("visible") && currloc != null) {
                                    makeMarkerActive(ParseUser.getCurrentUser().getObjectId());
                                }
                                findViewById(R.id.loading).setVisibility(View.INVISIBLE);
                                findViewById(R.id.gradient).setVisibility(View.VISIBLE);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        handler.post(new Runnable() {

                            public void run() {
                                Log.e("error", "error");
                            }
                        });
                    }
                }
            });
            th.start();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        mButton = (ImageButton) findViewById(R.id.imagebutton);
        updateShoutButton = (ImageButton) findViewById(R.id.update_shout_button);
        mEdit = (EditText) findViewById(R.id.changeStatus);
        updateStatus = (Button) findViewById(R.id.updateStatusButton);
        mSwitch = (ToggleButton) findViewById(R.id.switch1);
        cancelShoutButton = (ImageButton) findViewById(R.id.cancel_shout);
        changeStatusPin = (ImageView) findViewById(R.id.changeStatusPin);

        cancelShoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slideShoutoutSlideUp();
            }
        });
        updateShoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateStatus();
            }
        });
        updateStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatus();
            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!slideUpVisible) {
                    updateStatus();
                } else {
                    slideShoutoutSlideUp();
                }
            }
        });

        initFirebaseRefs();
    }

    private void initFirebaseRefs() {
        // live-update data location and status information from firebase

        refStatus.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onChildChanged(final DataSnapshot snapshot, String previousChildName) {
                final String status = snapshot.child("status").getValue().toString();
                final String userId = snapshot.getName();
                Log.d("FirebaseChildChanged", "found marker");
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("objectId", userId);
                query.getFirstInBackground(new GetCallback<ParseUser>() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        people.get(userId).activeIcon = null;
                        getActiveMarker(parseUser, status);
                        // wait for the active marker to be updated
                        try {
                            final Handler handler = new Handler();
                            Thread th = new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        while (people.get(userId).activeIcon == null) ;
                                        handler.post(new Runnable() {
                                            public void run() {
                                                markers.get(userId).setIcon(BitmapDescriptorFactory.fromBitmap(people.get(userId).activeIcon));
                                                makeMarkerActive(userId);
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        handler.post(new Runnable() {

                                            public void run() {
                                                Log.e("error", "error");
                                            }
                                        });
                                    }
                                }
                            });
                            th.start();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onCancelled(FirebaseError err) {

            }
        });


        refLoc.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                final Marker marker = markers.get(snapshot.getName());
                final double newlat = Double.parseDouble(snapshot.child("lat").getValue().toString());
                final double newlong = Double.parseDouble(snapshot.child("long").getValue().toString());
                if (marker != null) {
                    marker.animatePosition(new LatLng(newlat, newlong));
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onCancelled(FirebaseError err) {

            }
        });

        refPrivacy.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String s) {
                String userId = snapshot.getName();
                if (snapshot.child("privacy").getValue().toString().equals("NO")) {
                    hideUserMarkers(userId);
                } else if (snapshot.child("privacy").getValue().toString().equals("YES")) {
                    makeMarkerActive(userId);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        refOnline.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String userId = dataSnapshot.getName();
                if (dataSnapshot.getValue().toString().equals("YES")) {
                    changeMarkerOnlineStatus(userId, true);
                } else {
                    changeMarkerOnlineStatus(userId, false);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public void initSettingsView(View view) {
        findViewById(R.id.settings_list).setVisibility(View.VISIBLE);
        mSwitch.setChecked(ParseUser.getCurrentUser().getBoolean("visible"));
        mSwitch.setVisibility(View.VISIBLE);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean privacy) {
                if (privacy) {
                    ref.child("privacy").child(ParseUser.getCurrentUser().getObjectId()).child("privacy").setValue("YES");
                    ParseUser.getCurrentUser().put("visible", true);
                } else {
                    ref.child("privacy").child(ParseUser.getCurrentUser().getObjectId()).child("privacy").setValue("NO");
                    ParseUser.getCurrentUser().put("visible", false);
                }
            }
        });
    }

    protected void updateStatus(String... statusParam) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        SHOUTOUT_SLIDE_OFFSET = (int) getResources().getDimension(R.dimen.shoutout_slide_offset);
        if (!slideUpVisible) {
            mButton.setBackgroundColor(0xaa0088ff);
            if (!firstTime) {
                mButton.setPadding(0, SHOUTOUT_SLIDE_OFFSET, 0, 0);
                firstTime = true;
            } else {
                mButton.setY(mButton.getY() + SHOUTOUT_SLIDE_OFFSET);
            }
            mEdit.setText("");
            mEdit.append(statusParam.length > 0 ? statusParam[0] : ParseUser.getCurrentUser().getString("status"));
            mEdit.setVisibility(View.VISIBLE);
            mEdit.requestFocus();
            cancelShoutButton.setVisibility(View.VISIBLE);
            mgr.showSoftInput(mEdit, InputMethodManager.SHOW_IMPLICIT);
            updateStatus.setVisibility(View.VISIBLE);
            changeStatusPin.setVisibility(View.VISIBLE);
            slideUpVisible = true;
        } else {
            String status = mEdit.getText().toString();
            ParseUser.getCurrentUser().put("status", status);
            ref.child("status").child(ParseUser.getCurrentUser().getObjectId()).child("status").setValue(status);
            ParseUser.getCurrentUser().put("views", 0);
            ParseUser.getCurrentUser().saveInBackground();
            checkStatusForMessage(status);
            slideShoutoutSlideUp();
        }
    }

    private void slideShoutoutSlideUp() {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mButton.setBackgroundColor(0x55006666);
        mButton.setY(mButton.getY() - SHOUTOUT_SLIDE_OFFSET);
        mEdit.setVisibility(View.INVISIBLE);
        updateStatus.setVisibility(View.INVISIBLE);
        changeStatusPin.setVisibility(View.INVISIBLE);
        cancelShoutButton.setVisibility(View.INVISIBLE);
        mgr.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
        slideUpVisible = false;
    }

    private void initMessageButton() {
        messageButton = (ImageButton) findViewById(R.id.message_button);
        messageNumCircle = new MessageNumCircle(getResources(), (ImageView) findViewById(R.id.red_circle));
        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (messagesListView.getVisibility() == View.VISIBLE) {
                    hideMessageListView();
                } else {
                    initMessagesView();
                }
            }
        });
        updateMessageButton();
    }

    private void updateMessageButton() {
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

    private void initMessagesView() {
        ParseQuery<ParseObject> messageQuery = new ParseQuery<ParseObject>("Messages");
        messageQuery.whereEqualTo("to", ParseUser.getCurrentUser());
        messageQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> messageList, ParseException e) {
                if (e == null && messageList != null) {
                    messageList.add(0, new ParseObject("Messages")); // dummy parse object for header
                    ArrayAdapter<ParseObject> adapter = new MessageArrayAdapter<>(MyMapActivity.this, R.layout.messages_view, R.id.label, messageList, people, messageButton, getResources());
                    messagesListView.setAdapter(adapter);

                    messagesListView.setVisibility(View.VISIBLE);
                    messagesListView.animate().translationY(0).alpha(1.0f);
                }
            }
        });
        messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != 0) {
                    ParseObject item = (ParseObject) messagesListView.getAdapter().getItem(i);
                    try {
                        updateStatus("@" + item.getParseUser("from").fetchIfNeeded().getUsername() + " ");
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                hideMessageListView();
            }
        });
    }

    private void hideMessageListView() {
        messagesListView.animate().translationY(messagesListView.getHeight()).alpha(0.0f);
        messagesListView.setVisibility(View.INVISIBLE);
        updateMessageButton();
    }

    private static void checkStatusForMessage(final String status) {
        for (String word : status.split("[^a-zA-Z\\d@]")) { // split on all characters except letters, numbers and @
            if (word.startsWith("@")) {
                String username = word.substring(1);
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("username", username);
                query.getFirstInBackground(new GetCallback<ParseUser>() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        ParseObject message = new ParseObject("Messages");
                        message.put("from", ParseUser.getCurrentUser());
                        message.put("to", parseUser);
                        message.put("read", false);
                        message.put("message", status);
                        message.saveInBackground();
                    }
                });
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker != null) {
            makeMarkerActive(marker.getTitle());
        }
        return true;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        Person person = people.get(marker.getTitle());
        ((TextView) findViewById(R.id.status)).setText(person.status);
        ((TextView) findViewById(R.id.username)).setText(person.username);
        ((TextView) findViewById(R.id.date)).setText(person.updatedAt);
        return MyMapActivity.this.getLayoutInflater().inflate(R.layout.info_window, null);
//        return MyMapActivity.this.getLayoutInflater().inflate(R.layout.no_info_window, null);
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    private void makeMarkerActive(final String userId) {
//        Person person = people.get(userId);
//        if (person != null && !userId.equals(ParseUser.getCurrentUser().getObjectId())) {
//            final ParseUser parseUser = person.getParseUserObject();
//            try {
//                final int views = parseUser.fetchIfNeeded().getInt("views");
//                parseUser.put("views", views + 1);
//                parseUser.saveInBackground(new SaveCallback() {
//                    @Override
//                    public void done(ParseException e) {
//                        if (e != null) {
//                            Log.e("INCREMENT ERROR", e.getLocalizedMessage());
//                        }
//                        //setMarkerViewCount(userId, views+1);
//                    }
//                });
//            } catch (Exception e) {}
//        }
        for (Marker marker : markers.values()) {
            if (marker == null) {
                continue;
            }
            if (marker.getTitle().equals(userId)) {
                try {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(people.get(userId).activeIcon));
                    marker.showInfoWindow();
                } catch (Exception e) {
                    Log.e("SETTING MARKER ACTIVE", "error " + e.getLocalizedMessage() + "\n" + e.getStackTrace().toString());
                }
            }
            else {
                try {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(people.get(marker.getTitle()).icon));
                } catch (Exception e) {}
            }
        }
    }

    private void hideUserMarkers(String userId) {
        markers.get(userId).setVisible(false);
    }

    @Override
    public void onLocationChanged(Location mCurrentLocation) {
        ParseUser.getCurrentUser().put("geo", new ParseGeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
        ParseUser.getCurrentUser().saveInBackground();
        refLoc.child(ParseUser.getCurrentUser().getObjectId()).child("lat").setValue(mCurrentLocation.getLatitude());
        refLoc.child(ParseUser.getCurrentUser().getObjectId()).child("long").setValue(mCurrentLocation.getLongitude());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
        ref.child("online").child(ParseUser.getCurrentUser().getObjectId()).setValue("YES");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (messagesListView.getVisibility() == View.VISIBLE) {
                hideMessageListView();
            } else if (settingsView.getVisibility() == View.VISIBLE) {
                settingsView.setVisibility(View.INVISIBLE);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        ref.child("online").child(ParseUser.getCurrentUser().getObjectId()).setValue("NO");
        super.onStop();
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        for (Marker marker : markers.values()) {
            try {
                marker.remove();
            } catch (Exception e) {
                Log.e("ERROR REMOVING MARKER", marker.getTitle() + " " + e.getLocalizedMessage());
            }
        }

        if (Permissions.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            doLocationStuff();
        }
    }

    private void doLocationStuff() {
        mLocationRequest = LocationRequest.create();
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
                                if (firstConnect) {
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currloc.getLatitude(), currloc.getLongitude()), 15));
                                    firstConnect = false;
                                } else {
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(maplat, maplong), zoomlevel));
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
                                                getActiveMarker(user);
                                                getActiveMarker(user, ""); // get the empty status marker
                                                getInactiveMarker(geoloc, user);
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
            AlertDialog alertDialog = new AlertDialog.Builder(MyMapActivity.this).create();
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
                Toast.makeText(MyMapActivity.this, "Please allow Shoutout to access your location.", Toast.LENGTH_LONG).show();
            }
            return;
        }
    }

    private String findClosestMarker(double latitude, double longitude) { // returns the userId for the closest marker
        float minDistance = Float.MAX_VALUE;
        String key = "";
        for (Marker marker : markers.values()) {
            double markerLat = marker.getPosition().latitude;
            double markerLong = marker.getPosition().longitude;
            float[] results = new float[3];
            Location.distanceBetween(latitude, longitude, markerLat, markerLong, results);
            if (results[0] < minDistance) {
                minDistance = results[0];
                key = marker.getTitle();
            }
        }
        return key;
    }

    private Bitmap getCroppedBitmap(Bitmap sbmp) {
        Bitmap output = Bitmap.createBitmap(sbmp.getWidth(),
                sbmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, sbmp.getWidth(), sbmp.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#BAB399"));
        canvas.drawCircle(sbmp.getWidth() / 2f, sbmp.getHeight() / 2f,
                sbmp.getWidth() / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(sbmp, rect, rect, paint);
        return output;
    }

    private void changeMarkerOnlineStatus(String userId, boolean online) {
        Paint paint = new Paint();
        if (online) {
            paint.setColor(Color.GREEN);
        } else {
            paint.setColor(Color.parseColor(getString(R.string.shoutout_blue)));
        }
        Canvas canvas;
        Person person = people.get(userId);
        if (person == null || person.activeIcon == null || person.icon == null) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                canvas = new Canvas(person.activeIcon);
            } else {
                canvas = new Canvas(person.icon);
            }
            canvas.drawCircle(getResources().getDimension(R.dimen.online_icon_x_center), getResources().getDimension(R.dimen.online_icon_y_center), (int) getResources().getDimension(R.dimen.online_icon_size), paint);
        }
        if (online) {
            makeMarkerActive(userId);
        }
    }

    private void setMarkerViewCount(String userId, int viewCount) {
        Person person = people.get(userId);
        if (person == null || person.activeIcon == null) {
            return;
        }
        Canvas canvas = new Canvas(person.activeIcon);
        Log.d("VIEW COUNT", viewCount + " " + person.userId);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(getResources().getDimension(R.dimen.bubble_info_text_size));
        float y = getResources().getDimension(R.dimen.views_text_top_padding);
        float x = getResources().getDimension(R.dimen.views_text_left_padding);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRect(x,y,getResources().getDimension(R.dimen.views_text_right_endpoint),getResources().getDimension(R.dimen.views_text_bottom_endpoint), clearPaint);
        canvas.drawText("Views: " + viewCount, x, y, paint);
    }

    private Bitmap getUserIcon(final ParseUser user) {
        final String urlString = user.getString("picURL");
        if (urlString != null) {
            try {
                URL url = new URL(urlString);
                Bitmap icon = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                if (user.getObjectId().equals(ParseUser.getCurrentUser().getObjectId()) && user.get("profileImage") == null) { // upload the user's image to Parse
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    ParseFile imageFile = new ParseFile(user.getObjectId() + "_pic.png", stream.toByteArray());
                    final ParseObject imageObj = new ParseObject("Images");
                    imageObj.put("image", imageFile);
                    imageObj.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            user.put("profileImage", imageObj);
                            user.remove("picURL");
                            user.saveInBackground();
                        }
                    });
                }
                return icon;
            } catch (Exception e) {
            }
        } else {
            try {
                ParseObject imageObject = user.fetchIfNeeded().getParseObject("profileImage");
                byte[] fileData = imageObject.fetchIfNeeded().getParseFile("image").getData();
                return BitmapFactory.decodeByteArray(fileData, 0, fileData.length);
            } catch (Exception e) {
                Log.e("ERROR GETTING ICON", user.getObjectId() + "\n" + e.getLocalizedMessage());
            }
        }
        return null;
    }

    private void getActiveMarker(final ParseUser user, final String... statusParam) {
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap background = BitmapFactory.decodeResource(getResources(), R.drawable.shout_bubble_active);
                        final String status = statusParam.length > 0 ? statusParam[0] : user.getString("status");
                        String display = user.getString("displayName");
                        final String displayName = display == null ? user.getUsername() : display;
                        final Person person = people.get(user.getObjectId());
                        Bitmap mIcon = getUserIcon(user);
                        if (mIcon == null) {
                            return;
                        }
                        final Bitmap mIcon1 = mIcon.copy(Bitmap.Config.ARGB_8888, true);
                        mIcon.recycle();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                int width = (int) getResources().getDimension(R.dimen.active_bubble_width);
                                int height = (int) getResources().getDimension(R.dimen.active_bubble_height);
                                Bitmap markerIcon = Bitmap.createScaledBitmap(background, width, height, false);
                                background.recycle();
                                placePicInBitmap(mIcon1, markerIcon);
                                if (status.equals("")) {
                                    person.emptyStatusIcon = markerIcon;
                                    if (user.getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
                                        changeStatusPin.setImageBitmap(markerIcon);
                                    }
                                    person.activeIcon = null;
                                } else {
                                    writeTextInBubble(markerIcon, status, displayName);
                                    person.activeIcon = markerIcon;
//                                    setMarkerViewCount(user.getObjectId(), user.getInt("views"));
                                }
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
            getActiveMarker(user);
        }
    }

    private String insertStatusNewlines(String status) {
        final int status_line_length = 26;
        int len = status.length();
        for (int i = status_line_length; i < len; i += status_line_length) {
            int j;
            for (j = i; status.charAt(j) != ' '; j--) { // iterate backwards to find the beginning of the word, change space to newline
                if (j==0) {
                    return status;
                }
            }
            char[] newStatus = status.toCharArray();
            newStatus[j] = '\n';
            status = String.valueOf(newStatus);
        }
        return status;
    }

    private void writeTextInBubble(Bitmap activeIconBackground, String status, String displayName) {
        Canvas canvas = new Canvas(activeIconBackground);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(getResources().getDimension(R.dimen.status_text_size));
        float y = getResources().getDimension(R.dimen.status_text_top_padding);
        float x = getResources().getDimension(R.dimen.status_text_left_padding);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        int lineNum = 1;
        for (String line : insertStatusNewlines(status).split("\n")) {
            if (lineNum++ > 3) { // only print the first two lines of the status
                break;
            }
            canvas.drawText(line, x, y, paint);
            y += paint.descent() - paint.ascent();
        }

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(getResources().getDimension(R.dimen.bubble_info_text_size));
        canvas.drawText(displayName, x, y, paint);
    }

    private void getInactiveMarker(final ParseGeoPoint geoloc, final ParseUser user) {
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                public void run() {
                    try {
                        Bitmap mIcon = getUserIcon(user);
                        if (mIcon == null) {
                            Log.d("NULL ICON RETURNED", user.getObjectId());
                            return;
                        }
                        final Bitmap mIcon1 = mIcon.copy(Bitmap.Config.ARGB_8888, true);
                        mIcon.recycle();
                        final Bitmap background = BitmapFactory.decodeResource(getResources(), R.drawable.shout_bubble_inactive);
                        handler.post(new Runnable() {
                            public void run() {
                                int width = (int) getResources().getDimension(R.dimen.inactive_bubble_width);
                                int height = (int) getResources().getDimension(R.dimen.inactive_bubble_height);
                                final Bitmap markerIcon = Bitmap.createScaledBitmap(background, width, height, false);
                                background.recycle();
                                placePicInBitmap(mIcon1, markerIcon);
                                Marker newmark = map.addMarker(new MarkerOptions().position(new LatLng(geoloc.getLatitude(), geoloc.getLongitude()))
                                        .anchor(0.0f, 1.0f)
                                        .title(user.getObjectId())
                                        .icon(BitmapDescriptorFactory.fromBitmap(markerIcon)));
                                people.get(user.getObjectId()).icon = markerIcon;
                                if (people.get(user.getObjectId()).activeIcon == null) {
                                    people.get(user.getObjectId()).activeIcon = markerIcon;
                                }

                                if (!user.getBoolean("visible")) {
                                    newmark.setVisible(false);
                                }
                                markers.put(user.getObjectId(), newmark);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
//                        getInactiveMarker(geoloc, user);
                    }
                }
            });
            th.start();
        } catch (Exception e1) {
            e1.printStackTrace();
            getInactiveMarker(geoloc, user);
        }
    }

    private void placePicInBitmap(Bitmap mIcon1, Bitmap iconBackground) {
        int picSize = (int) getResources().getDimension(R.dimen.icon_size);
        int picPadding = (int) getResources().getDimension(R.dimen.icon_padding);
        Bitmap icon = Bitmap.createScaledBitmap(mIcon1, picSize, picSize, false);
        icon = getCroppedBitmap(icon);
        for (int i = 0; i < icon.getWidth(); i++) {
            for (int j = 0; j < icon.getHeight(); j++) {
                if (Math.pow(i - icon.getWidth() / 2, 2) + Math.pow(j - icon.getHeight() / 2, 2) < Math.pow(icon.getWidth() / 2, 2)) {
                    iconBackground.setPixel(Math.min(i + picPadding, iconBackground.getWidth() - 1), Math.min(j + picPadding, iconBackground.getHeight() - 1), icon.getPixel(i, j));
                }
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
