package shoutout2.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.events.MapListener;
import com.mapbox.mapboxsdk.events.RotateEvent;
import com.mapbox.mapboxsdk.events.ScrollEvent;
import com.mapbox.mapboxsdk.events.ZoomEvent;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyMapActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        MapViewListener,
        MapListener {

    private MapView map;
    private Map<String, Marker> markers;
    public Map<String, Person> people;
    private GoogleApiClient mLocationClient;
    private static final int UPDATE_INTERVAL = 5000; // milliseconds
    private static final int MIN_ZOOM = 3;
    private final Firebase ref = new Firebase("https://shoutout.firebaseIO.com/");
    private final Firebase refStatus = new Firebase("https://shoutout.firebaseIO.com/status");
    private final Firebase refLoc = new Firebase("https://shoutout.firebaseIO.com/loc");
    private final Firebase refPrivacy = new Firebase("https://shoutout.firebaseIO.com/privacy");
    private final Firebase refOnline = new Firebase("https://shoutout.firebaseIO.com/online");
    private static double maplat;
    private static double maplong;
    private static boolean firstConnect = true;
    private RelativeLayout messagesView;
    private ImageButton messageButton;
    private MessageNumCircle messageNumCircle;
    private int screen;
    private final int MAP_SCREEN = 1;
    private final int SETTINGS = 2;
    private final int UPDATE_SHOUT = 3;

    private final int CLUSTER_ZOOM_LEVEL = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_map);
        screen = MAP_SCREEN;

        new Permissions(this);

        markers = new HashMap<>();
        people = new HashMap<>();
        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        map = (MapView) findViewById(R.id.map);
        map.setClusteringEnabled(true, null, CLUSTER_ZOOM_LEVEL);
        map.setMapViewListener(this);
        map.addListener(this);

        final ImageButton myLocationButton = (ImageButton) findViewById(R.id.my_location_button);
        myLocationButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        myLocationButton.setBackgroundResource(R.drawable.my_location_pressed);
                        final ParseGeoPoint currLoc = ParseUser.getCurrentUser().getParseGeoPoint("geo");
                        map.getController().animateTo(new LatLng(currLoc.getLatitude(), currLoc.getLongitude()));
                        makeMarkerActive(ParseUser.getCurrentUser().getObjectId());
                        return true;
                    case MotionEvent.ACTION_UP:
                        myLocationButton.setBackgroundResource(R.drawable.my_location);
                        return true;
                }
                return false;
            }
        });
        map.setMinZoomLevel(MIN_ZOOM);

        messagesView = (RelativeLayout) findViewById(R.id.message_view);
        initMessageButton();

        initSettingsButton();

        final ImageButton updateShoutButton = (ImageButton) findViewById(R.id.update_shout_button);
        updateShoutButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        updateShoutButton.setBackgroundResource(R.drawable.update_shout_pressed);
                        updateStatus();
                        return true;
                    case MotionEvent.ACTION_UP:
                        updateShoutButton.setBackgroundResource(R.drawable.update_shout);
                        return true;
                }
                return false;
            }
        });

        final ImageButton listViewButton = (ImageButton) findViewById(R.id.list_view_button);
        listViewButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        listViewButton.setBackgroundResource(R.drawable.list_view_pressed);
                        Toast.makeText(MyMapActivity.this, "List view coming soon.", Toast.LENGTH_LONG).show();
                        return true;
                    case MotionEvent.ACTION_UP:
                        listViewButton.setBackgroundResource(R.drawable.list_view);
                        return true;
                }
                return false;
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
                try {
                    final Handler handler = new Handler();
                    Thread th = new Thread(new Runnable() {
                        public void run() {
                            try {
                                final String status = snapshot.child("status").getValue().toString();
                                final String userId = snapshot.getName();
                                Person person = people.get(userId);
                                person.setFields(status, new Date(System.currentTimeMillis()));
                                handler.post(new Runnable() {
                                    public void run() {
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
                    marker.setPoint(new LatLng(newlat, newlong));
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

    @Override
    public void onScroll(ScrollEvent event) {
        LatLng pos = map.getCenter();
        maplat = pos.getLatitude();
        maplong = pos.getLongitude();
        String closestMarkerUserId = "";
        try {
            closestMarkerUserId = findClosestMarker(maplat, maplong);
        } catch (Exception e) {
            Log.e("err finding closest", "error " + e.getLocalizedMessage());
        }
        makeMarkerActive(closestMarkerUserId);
    }

    @Override
    public void onZoom(ZoomEvent event) {

    }

    @Override
    public void onRotate(RotateEvent event) {

    }

    @Override
    public void onShowMarker(final MapView pMapView, final Marker pMarker) {
        pMarker.getParentHolder().setFocus(pMarker);
    }

    @Override
    public void onHideMarker(final MapView pMapView, final Marker pMarker){
    }

    @Override
    public void onTapMarker(final MapView pMapView, final Marker marker){
        if (marker.bubbleShowing) {
            startMessageTo(marker.getTitle());
        } else {
            makeMarkerActive(marker.getTitle());
        }
    }

    @Override
    public void onLongPressMarker(final MapView pMapView, final Marker pMarker) {

    }

    @Override
    public void onTapMap(final MapView pMapView, final ILatLng pPosition) {
        makeMarkerActive("");
    }

    @Override
    public void onLongPressMap(final MapView pMapView, final ILatLng pPosition) {

    }

    protected void updateStatus(String... statusParam) {
        makeMarkerActive("");
        LayoutInflater inflater = getLayoutInflater();
        final RelativeLayout rootView = (RelativeLayout) findViewById(R.id.main_map_view);
        final View view = inflater.inflate(R.layout.update_shout, rootView);
        view.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        screen = UPDATE_SHOUT;

        final EditText mEdit = (EditText) view.findViewById(R.id.newStatus);
        final Button updateStatus = (Button) view.findViewById(R.id.updateStatusButton);
        final ImageButton cancelShoutButton = (ImageButton) view.findViewById(R.id.cancel_shout);
        final ImageView changeStatusPin = (ImageView) view.findViewById(R.id.changeStatusPin);
        final InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mEdit.setText("");
        mEdit.append(statusParam.length > 0 ? statusParam[0] : ParseUser.getCurrentUser().getString("status"));
        mEdit.requestFocus();
        mgr.showSoftInput(mEdit, InputMethodManager.SHOW_IMPLICIT);

        changeStatusPin.setImageBitmap(people.get(ParseUser.getCurrentUser().getObjectId()).emptyStatusIcon);

        cancelShoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mgr.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
                rootView.removeView(rootView.findViewById(R.id.update_shout_view));
                screen = MAP_SCREEN;
            }
        });

        updateStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status = mEdit.getText().toString();
                ParseUser.getCurrentUser().put("status", status);
                ref.child("status").child(ParseUser.getCurrentUser().getObjectId()).child("status").setValue(status);
                ParseUser.getCurrentUser().saveInBackground();
                checkStatusForMessage(status);
                cancelShoutButton.callOnClick();
            }
        });
    }

    private void initSettingsButton() {
        final ImageButton settingsButton = (ImageButton) findViewById(R.id.settings_button);
        settingsButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        settingsButton.setBackgroundResource(R.drawable.settings_pressed);
                        initSettingsView();
                        return true;
                    case MotionEvent.ACTION_UP:
                        settingsButton.setBackgroundResource(R.drawable.settings);
                        return true;
                }
                return false;
            }
        });
    }

    public void initSettingsView() {
        LayoutInflater inflater = getLayoutInflater();
        final RelativeLayout rootView = (RelativeLayout) findViewById(R.id.main_map_view);
        final View view = inflater.inflate(R.layout.settings, rootView);
        view.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        screen = SETTINGS;

        ToggleButton mSwitch = (ToggleButton) view.findViewById(R.id.switch1);
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

        ImageButton changeIconButton = (ImageButton) view.findViewById(R.id.settings_change_icon);
        changeIconButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        ImageButton closeButton = (ImageButton) view.findViewById(R.id.settings_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rootView.removeView(rootView.findViewById(R.id.settings_view));
                screen = MAP_SCREEN;
            }
        });

        final EditText usernameField = (EditText) view.findViewById(R.id.change_username_field);
        usernameField.setText(ParseUser.getCurrentUser().getUsername());
        Button changeUsernameButton = (Button) view.findViewById(R.id.change_username_button);
        changeUsernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                if (Utils.usernameTaken(username)) {
                    Toast.makeText(MyMapActivity.this, "Username taken. Please enter a different username.",Toast.LENGTH_LONG).show();
                    return;
                }
                if (username.contains(" ")) {
                    Toast.makeText(MyMapActivity.this, "Username must be one word.",Toast.LENGTH_LONG).show();
                    return;
                }
                ParseUser.getCurrentUser().setUsername(username);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                people.get(ParseUser.getCurrentUser().getObjectId()).username = username;
            }
        });

        Button feedbackButton = (Button) view.findViewById(R.id.feedback_button);
        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(MyMapActivity.this);

                alert.setTitle("Feedback");

                final EditText input = new EditText(MyMapActivity.this);
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ParseObject feedback = new ParseObject("Feedback");
                        feedback.put("message", input.getText().toString());
                        feedback.put("author", ParseUser.getCurrentUser());
                        feedback.saveInBackground();
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                alert.show();
            }
        });

        Button logoutButton = (Button) view.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyMapActivity.this.finish();
                ParseUser.logOut();
//                startActivity(new Intent(MyMapActivity.this, LoginActivity.class));
            }
        });
    }

    private void initMessageButton() {
        messageButton = (ImageButton) findViewById(R.id.message_button);
        messageNumCircle = new MessageNumCircle(getResources(), (ImageView) findViewById(R.id.red_circle));
        messageButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        messageButton.setBackgroundResource(R.drawable.message_pressed);
                        if (messagesView.getVisibility() == View.VISIBLE) {
                            hideMessageView();
                        } else {
                            initMessagesView();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        messageButton.setBackgroundResource(R.drawable.message);
                        return true;
                }
                return false;
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
        final ListView messagesListView = (ListView) findViewById(R.id.messages_list);
        final ParseQuery<ParseObject> messageQuery = new ParseQuery<ParseObject>("Messages");
        messageQuery.whereEqualTo("to", ParseUser.getCurrentUser());
        messageQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> messageList, ParseException e) {
                if (e == null && messageList != null) {
                    ArrayAdapter<ParseObject> adapter = new MessageArrayAdapter<>(MyMapActivity.this, R.layout.messages_view, R.id.label, messageList, people);
                    messagesListView.setAdapter(adapter);

                    messagesView.setVisibility(View.VISIBLE);
                    messagesView.animate().translationY(0).alpha(1.0f);
                }
            }
        });
        messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ParseObject item = (ParseObject) messagesListView.getAdapter().getItem(i);
                try {
                    startMessageTo(item.fetchIfNeeded().getParseUser("from").fetchIfNeeded().getObjectId());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                hideMessageView();
            }
        });
    }

    public void startMessageTo(String userId) {
        Log.d("SENDING MESSAGE TO", userId);
        updateStatus("@" + people.get(userId).username + " ");
    }

    private void hideMessageView() {
        messagesView.animate().translationY(messagesView.getHeight()).alpha(0.0f);
        messagesView.setVisibility(View.INVISIBLE);
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

    private void makeMarkerActive(final String userId) {
        for (Marker marker : markers.values()) {
            if (marker == null) {
                continue;
            }
            if (marker.getTitle().equals(userId) && !marker.inCluster) {
                try {
                    LayerDrawable layerDrawable = (LayerDrawable) marker.getDrawable();
                    layerDrawable.getDrawable(2).setBounds((int) getResources().getDimension(R.dimen.info_window_padding_left), 0, (int) getResources().getDimension(R.dimen.info_window_width) + (int) getResources().getDimension(R.dimen.info_window_padding_left), (int) getResources().getDimension(R.dimen.info_window_height));
                    layerDrawable.setDrawableByLayerId(R.id.infoWindow, new BitmapDrawable(getResources(), infoWindowToBitmap(userId)));
                    map.selectMarker(marker);
                } catch (Exception e) {
                    Log.e("SETTING MARKER ACTIVE", "error " + e.getLocalizedMessage());
                }
            } else {
                try {
                    LayerDrawable layerDrawable = (LayerDrawable) marker.getDrawable();
                    layerDrawable.setDrawableByLayerId(R.id.infoWindow, getBlankImage());
                    marker.closeToolTip();
                } catch (Exception e) {
                    Log.e("ERROR CLOSING TOOLTIP", "error " + e.getLocalizedMessage());
                }
            }
        }
    }

    private BitmapDrawable getBlankImage() {
        return new BitmapDrawable(getResources(),BitmapFactory.decodeResource(getResources(), android.R.color.transparent));
    }

    private Bitmap infoWindowToBitmap(String userId) {
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.info_window, null);
        Person person = people.get(userId);
        ((TextView) v.findViewById(R.id.status)).setText(person.status);
        ((TextView) v.findViewById(R.id.username)).setText(person.username);
        ((TextView) v.findViewById(R.id.date)).setText(person.updatedAt);
        v.setDrawingCacheEnabled(true);
        v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());

        v.buildDrawingCache(true);
        Bitmap b = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false); // clear drawing cache
        return b;
    }

    private void hideUserMarkers(String userId) {
        Marker marker = markers.get(userId);
        marker.closeToolTip();
        map.removeMarker(marker);
        map.invalidate();
    }

    @Override
    public void onLocationChanged(Location mCurrentLocation) {
        if (ParseUser.getCurrentUser() != null) {
            ParseUser.getCurrentUser().put("geo", new ParseGeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
            ParseUser.getCurrentUser().saveInBackground();
            refLoc.child(ParseUser.getCurrentUser().getObjectId()).child("lat").setValue(mCurrentLocation.getLatitude());
            refLoc.child(ParseUser.getCurrentUser().getObjectId()).child("long").setValue(mCurrentLocation.getLongitude());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
        ref.child("online").child(ParseUser.getCurrentUser().getObjectId()).setValue("YES");
        ParseUser.getCurrentUser().put("online", true);
    }

    @Override
    public void onBackPressed() {
        if (screen == MAP_SCREEN) {
            if (messagesView.getVisibility() == View.VISIBLE) {
                hideMessageView();
                return;
            }
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        if (ParseUser.getCurrentUser() != null) {
            ref.child("online").child(ParseUser.getCurrentUser().getObjectId()).setValue("NO");
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
        map.clear();

        if (Permissions.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            doLocationStuff();
        }
    }

    private void doLocationStuff() {
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
                                if (firstConnect) {
                                    map.setCenter(new LatLng(currloc.getLatitude(), currloc.getLongitude()));
                                    firstConnect = false;
                                } else {
                                    map.setCenter(new LatLng(maplat, maplong));
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
                                                getEmptyStatusIcon(user);
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
            if (marker.inCluster) {
                continue;
            }
            double markerLat = marker.getPosition().getLatitude();
            double markerLong = marker.getPosition().getLongitude();
            float[] results = new float[3];
            Location.distanceBetween(latitude, longitude, markerLat, markerLong, results);
            if (results[0] < minDistance) {
                minDistance = results[0];
                key = marker.getTitle();
            }
        }
        return key;
    }

    private void changeMarkerOnlineStatus(String userId, boolean online) {
        Marker marker = markers.get(userId);
        if (marker == null) {
            return;
        }
        LayerDrawable layerDrawable = (LayerDrawable) marker.getDrawable();
        layerDrawable.getDrawable(3).setAlpha(online ? 1 : 0);
        if (online) {
            makeMarkerActive(userId);
        }
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

    private void getEmptyStatusIcon(final ParseUser user) {
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap background = BitmapFactory.decodeResource(getResources(), R.drawable.shout_bubble_active);
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
                                person.emptyStatusIcon = markerIcon;
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
            getEmptyStatusIcon(user);
        }
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
                                Marker marker = new Marker(user.getObjectId(), null, new LatLng(geoloc.getLatitude(), geoloc.getLongitude()), (int) (getResources().getDimension(R.dimen.inactive_bubble_width) + getResources().getDimension(R.dimen.info_window_padding_left)), (int) -getResources().getDimension(R.dimen.info_window_padding_bottom));
                                marker.setHotspot(Marker.HotspotPlace.LOWER_LEFT_CORNER);

                                int picSize = (int) getResources().getDimension(R.dimen.icon_size);
                                Bitmap icon = Bitmap.createScaledBitmap(mIcon1, picSize, picSize, false);
                                BitmapDrawable[] layers = new BitmapDrawable[4];
                                layers[0] = new BitmapDrawable(getResources(), markerIcon);
                                layers[1] = new BitmapDrawable(getResources(), Utils.getCroppedBitmap(icon));
                                layers[1].setGravity(Gravity.LEFT | Gravity.TOP);
                                layers[2] = getBlankImage();
                                Bitmap onlineIcon = BitmapFactory.decodeResource(getResources(), R.drawable.online_icon);
                                int iconSize = (int) getResources().getDimension(R.dimen.online_icon_size);
                                layers[3] = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(onlineIcon, iconSize, iconSize, false));
                                layers[3].setGravity(Gravity.LEFT | Gravity.BOTTOM);
                                LayerDrawable layerDrawable = new LayerDrawable(layers);
                                layerDrawable.setId(0, R.id.bubble);
                                layerDrawable.setId(1, R.id.userPic);
                                layerDrawable.setId(2, R.id.infoWindow);
                                layerDrawable.setId(3, R.id.onlineIcon);
                                int picPadding = (int) getResources().getDimension(R.dimen.icon_padding);
                                int iconPadding[] = {(int) getResources().getDimension(R.dimen.online_icon_left_padding), (int) getResources().getDimension(R.dimen.online_icon_bottom_padding)};
                                layerDrawable.setLayerInset(1, picPadding, picPadding, 0, 0);
                                layerDrawable.setLayerInset(3, iconPadding[0], 0, 0, iconPadding[1]);
                                marker.setMarker(layerDrawable);
                                marker.setToolTip(new MyInfoWindow(map));

                                if (user.getBoolean("visible")) {
                                    map.addMarker(marker);
                                    markers.put(user.getObjectId(), marker);
                                    changeMarkerOnlineStatus(user.getObjectId(), user.getBoolean("online"));
                                }
                                if (ParseUser.getCurrentUser().getObjectId().equals(user.getObjectId())) {
                                    map.getController().setZoom(18.1f);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
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
        icon = Utils.getCroppedBitmap(icon);
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
