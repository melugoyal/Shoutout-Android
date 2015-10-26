package shoutout2.app.Utils;

import android.location.Location;
import android.os.Handler;
import android.util.Log;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.parse.ParseUser;

import java.util.Date;

import shoutout2.app.MapView.MapActivity;
import shoutout2.app.Person;

public class FirebaseUtils {
    private static final Firebase refStatus = new Firebase("https://shoutout.firebaseIO.com/status");
    private static final Firebase refLoc = new Firebase("https://shoutout.firebaseIO.com/loc");
    private static final Firebase refPrivacy = new Firebase("https://shoutout.firebaseIO.com/privacy");
    private static final Firebase refOnline = new Firebase("https://shoutout.firebaseIO.com/online");
    private static MapActivity mapActivity;

    public FirebaseUtils(final MapActivity mapActivity) {
        FirebaseUtils.mapActivity = mapActivity;
        initStatusRef();
        initLocRef();
        initPrivacyRef();
        initOnlineRef();
    }

    private static void initStatusRef() {
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
                                Person person = mapActivity.people.get(userId);
                                person.setFields(status, new Date(System.currentTimeMillis()));
                                handler.post(new Runnable() {
                                    public void run() {
                                        mapActivity.makeMarkerActive(userId);
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
    }

    private static void initLocRef() {
        refLoc.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                final Marker marker = mapActivity.markers.get(snapshot.getName());
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
    }

    private static void initPrivacyRef() {
        refPrivacy.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String s) {
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String s) {
                String userId = snapshot.getName();
                if (snapshot.child("privacy").getValue().toString().equals("NO")) {
                    mapActivity.hideUserMarkers(userId);
                } else if (snapshot.child("privacy").getValue().toString().equals("YES")) {
                    mapActivity.makeMarkerActive(userId);
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
    }

    private static void initOnlineRef() {
        refOnline.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String userId = dataSnapshot.getName();
                if (dataSnapshot.getValue().toString().equals("YES")) {
                    mapActivity.changeMarkerOnlineStatus(userId, true);
                } else {
                    mapActivity.changeMarkerOnlineStatus(userId, false);
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

    public static void updateStatus(String status) {
        refStatus.child(ParseUser.getCurrentUser().getObjectId()).child("status").setValue(status);
    }

    public static void updateLocation(Location location) {
        refLoc.child(ParseUser.getCurrentUser().getObjectId()).child("lat").setValue(location.getLatitude());
        refLoc.child(ParseUser.getCurrentUser().getObjectId()).child("long").setValue(location.getLongitude());
    }

    public static void setOnline(boolean online) {
        refOnline.child(ParseUser.getCurrentUser().getObjectId()).setValue(online ? "YES" : "NO");
    }

    public static void setPrivacy(boolean privacy) {
        refPrivacy.child(ParseUser.getCurrentUser().getObjectId()).child("privacy").setValue(privacy ? "YES" : "NO");
    }
}
