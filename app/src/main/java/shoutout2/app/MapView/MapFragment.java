package shoutout2.app.MapView;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.events.MapListener;
import com.mapbox.mapboxsdk.events.RotateEvent;
import com.mapbox.mapboxsdk.events.ScrollEvent;
import com.mapbox.mapboxsdk.events.ZoomEvent;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.ClusterMarker;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

import shoutout2.app.MessageNumCircle;
import shoutout2.app.MyInfoWindow;
import shoutout2.app.Person;
import shoutout2.app.R;
import shoutout2.app.Utils.Utils;

public class MapFragment extends Fragment implements MapViewListener, MapListener {
    public static final String TAG = "MAP_FRAGMENT";
    private MapActivity mapActivity;
    protected MapView map;
    private static final int CLUSTER_ZOOM_LEVEL = 16;
    private static final int MIN_ZOOM = 3;
    protected float scale = 1f;
    private double latitudeOffset = 0; // will be non-zero when list view is showing
    protected double maplat;
    protected double maplong;
    private Resources res;
    protected View mapView;
    protected ListViewFragment listViewFragment;
    private View crosshairs;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mapActivity = (MapActivity) getActivity();
        res = mapActivity.getResources();
        final View view = inflater.inflate(R.layout.map_fragment, container, false);
        mapView = view;
        map = (MapView) view.findViewById(R.id.map);
        map.setClusteringEnabled(true, new DrawClusterMarker(getResources()), CLUSTER_ZOOM_LEVEL);
        map.setMapViewListener(this);
        map.addListener(this);
        map.setMinZoomLevel(MIN_ZOOM);
        crosshairs = mapView.findViewById(R.id.crosshairs);

        initMyLocationButton();
        initListViewButton();
        initUpdateShoutButton();
        initMessageButton();
        initSettingsButton();

        if (!ParseUser.getCurrentUser().getBoolean("visible")) {
            initDisabledView();
        }

        return view;
    }

    private void initMyLocationButton() {
        final ImageButton myLocationButton = (ImageButton) mapView.findViewById(R.id.my_location_button);
        myLocationButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        myLocationButton.setBackgroundResource(R.drawable.my_location_pressed);
                        try {
                            final ParseGeoPoint currLoc = ParseUser.getCurrentUser().fetchIfNeeded().getParseGeoPoint("geo");
                            map.getController().animateTo(new LatLng(currLoc.getLatitude() - latitudeOffset, currLoc.getLongitude()));
                            mapActivity.makeMarkerActive(ParseUser.getCurrentUser().getObjectId());
                        } catch (Exception e) {
                            Log.e("location button error", e.getLocalizedMessage());
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        myLocationButton.setBackgroundResource(R.drawable.my_location);
                        return true;
                }
                return false;
            }
        });
    }

    private void initListViewButton() {
        final ImageButton listViewButton = (ImageButton) mapView.findViewById(R.id.list_view_button);
        listViewButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        listViewButton.setBackgroundResource(R.drawable.list_view_pressed);
                        initListView();
                        return true;
                    case MotionEvent.ACTION_UP:
                        listViewButton.setBackgroundResource(R.drawable.list_view);
                        return true;
                }
                return false;
            }
        });
    }

    private void initUpdateShoutButton() {
        final ImageButton updateShoutButton = (ImageButton) mapView.findViewById(R.id.update_shout_button);
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

    }

    private void initMessageButton() {
        final ImageButton messageButton = (ImageButton) mapView.findViewById(R.id.message_button);
        mapActivity.messageNumCircle = new MessageNumCircle(res, (ImageView) mapView.findViewById(R.id.red_circle));
        messageButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        messageButton.setBackgroundResource(R.drawable.message_pressed);
                        initMessagesView();
                        return true;
                    case MotionEvent.ACTION_UP:
                        messageButton.setBackgroundResource(R.drawable.message);
                        return true;
                }
                return false;
            }
        });
        mapActivity.updateMessageButton();
    }

     protected void initDisabledView() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(DisabledAppFragment.TAG);

        if (fragment == null) {
            fragment = new DisabledAppFragment();
        }
        Utils.addFragment(fragmentManager, R.id.map_activity_container, DisabledAppFragment.TAG, fragment, true);
    }

    private void initListView() {
        FragmentManager fragmentManager = getFragmentManager();
        listViewFragment = (ListViewFragment) fragmentManager.findFragmentByTag(ListViewFragment.TAG);
        if (listViewFragment == null) {
            listViewFragment = new ListViewFragment();
        }

        Point size = new Point();
        mapActivity.getWindowManager().getDefaultDisplay().getSize(size);
        float paddingTop = res.getDimension(R.dimen.list_view_padding_top);
        if (listViewFragment.isVisible()) {
            ILatLng newCenter = map.getProjection().fromPixels(size.x/2.0f, paddingTop/2.0f);
            map.getController().animateTo(newCenter);
            crosshairs.setY(size.y / 2.0f);
            latitudeOffset = 0;
            mapActivity.onBackPressed();
        } else {
            Utils.addFragment(fragmentManager, R.id.map_activity_container, ListViewFragment.TAG, listViewFragment, true);
            ILatLng newCenter = map.getProjection().fromPixels(size.x/2.0f, size.y - paddingTop/2.0f);
            latitudeOffset = map.getCenter().getLatitude() - newCenter.getLatitude();
            map.getController().animateTo(newCenter);
            crosshairs.setY(res.getDimension(R.dimen.list_view_padding_top) / 2.0f);
            mapView.findViewById(R.id.list_view_triangle).setVisibility(View.VISIBLE);
        }
    }

    private void initMessagesView() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(MessagesFragment.TAG);

        if (fragment == null) {
            fragment = new MessagesFragment();
        }
        Utils.addFragment(fragmentManager, R.id.map_activity_container, MessagesFragment.TAG, fragment, true);
    }

    private void initSettingsButton() {
        final ImageButton settingsButton = (ImageButton) mapView.findViewById(R.id.settings_button);
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
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(SettingsFragment.TAG);

        if (fragment == null) {
            fragment = new SettingsFragment();
        }
        Utils.addFragment(fragmentManager, R.id.map_activity_container, SettingsFragment.TAG, fragment, true);
    }

    protected void initInfoWindow(View markerView, String userId) {
        View v = markerView.findViewById(R.id.infoWindow);
        Person person = mapActivity.people.get(userId);
        v.setVisibility(View.VISIBLE);
        v.findViewById(R.id.status).setVisibility(View.VISIBLE);
        v.findViewById(R.id.username).setVisibility(View.VISIBLE);
        v.findViewById(R.id.date).setVisibility(View.VISIBLE);
        ((TextView) v.findViewById(R.id.status)).setText(person.status);
        ((TextView) v.findViewById(R.id.username)).setText(person.username);
        ((TextView) v.findViewById(R.id.date)).setText(person.updatedAt);
    }

    @Override
    public void onScroll(ScrollEvent event) {
        ILatLng center = map.getProjection().fromPixels(crosshairs.getX(), crosshairs.getY());
        maplat = center.getLatitude();
        maplong = center.getLongitude();
        String closestMarkerUserId = "";
        try {
            closestMarkerUserId = findClosestMarker(maplat, maplong);
        } catch (Exception e) {
            Log.e("err finding closest", "error " + e.getLocalizedMessage());
        }
        mapActivity.makeMarkerActive(closestMarkerUserId);
        if (listViewFragment != null && listViewFragment.isVisible()) {
            listViewFragment.getPins();
        }
    }

    @Override
    public void onZoom(ZoomEvent event) {
        int minZoomForScaling = CLUSTER_ZOOM_LEVEL - 2;
        scale = Math.max((event.getZoomLevel() - minZoomForScaling) / (map.getMaxZoomLevel() - minZoomForScaling), 0.20f);
        int width = (int) (scale * getResources().getDimension(R.dimen.inactive_bubble_width));
        int height = (int) (scale * getResources().getDimension(R.dimen.inactive_bubble_height));
        for (Marker marker : mapActivity.markers.values()) {
            Person person = mapActivity.people.get(marker.getTitle());
            if (person.inactiveMarker != null) {
                Bitmap newPic = Bitmap.createScaledBitmap(person.inactiveMarker, width, height, false);
                person.scaledInactiveMarker = newPic;
                if (!marker.bubbleShowing) {
                    marker.setMarker(new BitmapDrawable(getResources(), newPic));
                }
            }
        }
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
            mapActivity.startMessageTo(mapActivity.people.get(marker.getTitle()).username);
        } else {
            mapActivity.makeMarkerActive(marker.getTitle());
        }
    }

    @Override
    public void onLongPressMarker(final MapView pMapView, final Marker pMarker) {

    }

    @Override
    public void onTapMap(final MapView pMapView, final ILatLng pPosition) {
        mapActivity.makeMarkerActive("");
    }

    @Override
    public void onLongPressMap(final MapView pMapView, final ILatLng pPosition) {

    }

    private String findClosestMarker(double latitude, double longitude) { // returns the userId for the closest marker
        float minDistance = Float.MAX_VALUE;
        String key = "";
        for (Marker marker : mapActivity.markers.values()) {
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

    protected void getIcons(final ParseGeoPoint geoloc, final ParseUser user) {
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap background = BitmapFactory.decodeResource(res, R.drawable.shout_bubble_inactive);
                        final Person person = mapActivity.people.get(user.getObjectId());
                        Bitmap mIcon = Utils.getUserIcon(user);
                        if (mIcon == null) {
                            return;
                        }
                        final Bitmap mIcon1 = mIcon.copy(Bitmap.Config.ARGB_8888, true);
                        mIcon.recycle();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                    int width = (int) res.getDimension(R.dimen.inactive_bubble_width);
                                    int height = (int) res.getDimension(R.dimen.inactive_bubble_height);
                                    final Bitmap markerIcon = Bitmap.createScaledBitmap(background, width, height, false);
                                    background.recycle();

                                    LayoutInflater inflater = mapActivity.getLayoutInflater();
                                    View view = inflater.inflate(R.layout.marker, null);
                                    ((ImageView) view.findViewById(R.id.marker_bubble)).setImageBitmap(markerIcon);
                                    int picSize = (int) res.getDimension(R.dimen.icon_size);
                                    Bitmap icon = Bitmap.createScaledBitmap(mIcon1, picSize, picSize, false);
                                    icon = Utils.getCroppedBitmap(icon);
                                    person.icon = icon;
                                    ((ImageView) view.findViewById(R.id.userPic)).setImageBitmap(icon);

                                if (user.getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) { // only need empty status icon for the currernt user
                                    View infoWindow = view.findViewById(R.id.infoWindow);
                                    infoWindow.setVisibility(View.VISIBLE);
                                    infoWindow.findViewById(R.id.status).setVisibility(View.GONE);
                                    infoWindow.findViewById(R.id.username).setVisibility(View.GONE);
                                    infoWindow.findViewById(R.id.date).setVisibility(View.GONE);
                                    person.emptyStatusIcon = Utils.viewToBitmap(view);
                                }
                                person.markerView = view;
                                getInactiveMarker(geoloc, user);
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
            getIcons(geoloc, user);
        }
    }

    protected void getInactiveMarker(final ParseGeoPoint geoloc, final ParseUser user) {
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                public void run() {
                    try {
                        final Bitmap onlineIcon = BitmapFactory.decodeResource(res, R.drawable.online_icon);
                        final int iconSize = (int) res.getDimension(R.dimen.online_icon_size);
                        final String userId = user.getObjectId();
                        final Person person = mapActivity.people.get(userId);
                        handler.post(new Runnable() {
                            public void run() {
                                Marker marker = new Marker(user.getObjectId(), null, new LatLng(geoloc.getLatitude(), geoloc.getLongitude()));
                                marker.setHotspot(Marker.HotspotPlace.LOWER_LEFT_CORNER);

                                View view = person.markerView;
                                view.findViewById(R.id.infoWindow).setVisibility(View.GONE);
                                ((ImageView)view.findViewById(R.id.onlineIcon)).setImageBitmap(Bitmap.createScaledBitmap(onlineIcon, iconSize, iconSize, false));
                                mapActivity.changeMarkerOnlineStatus(user.getObjectId(), user.getBoolean("online"));
                                Bitmap markerIcon = Utils.viewToBitmap(view);
                                person.inactiveMarker = markerIcon;
                                person.scaledInactiveMarker = markerIcon;
                                marker.setMarker(new BitmapDrawable(res, markerIcon));
                                marker.setToolTip(new MyInfoWindow(map));
                                mapActivity.markers.put(userId, marker);
                                if (user.getBoolean("visible")) {
                                    map.addMarker(marker);
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

    protected void updateStatus(String... statusParam) {
        mapActivity.makeMarkerActive("");

        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(UpdateShoutFragment.TAG);

        if (fragment == null) {
            fragment = new UpdateShoutFragment();
            Bundle args = new Bundle();
            args.putStringArray("statusParam", statusParam);
            fragment.setArguments(args);
        }
        Utils.addFragment(fragmentManager, R.id.map_activity_container, UpdateShoutFragment.TAG, fragment, true);
    }
}
