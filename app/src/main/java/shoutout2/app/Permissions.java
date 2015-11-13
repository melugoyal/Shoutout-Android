package shoutout2.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.HashMap;

public class Permissions {
    private static Activity activity;
    public static final HashMap<String, Integer> permissionInts = new HashMap<>();
    public Permissions(Activity activity) {
        this.activity = activity;
        permissionInts.put("location", 1);
    }

    public static boolean requestPermission(String permission) {
        if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) || permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                        permissionInts.get("location"));
                return false;
            }
            return true;
        }

        if (permissionInts.get(permission) == null) {
            permissionInts.put(permission, permissionInts.size() + 1);
        }

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity, permission)) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(mapActivity, permission)) {
//            }

            ActivityCompat.requestPermissions(activity,
                    new String[]{permission},
                    permissionInts.get(permission));
            return false;
        }
        return true;
    }
}
