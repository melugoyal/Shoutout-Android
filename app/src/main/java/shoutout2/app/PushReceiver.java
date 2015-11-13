package shoutout2.app;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONObject;

import shoutout2.app.Utils.Utils;

public class PushReceiver extends ParsePushBroadcastReceiver{

    @Override
    protected Bitmap getLargeIcon(Context context, Intent intent) {
        try {
            String alert = (new JSONObject(intent.getExtras().getString(ParsePushBroadcastReceiver.KEY_PUSH_DATA))).getString("alert");
            String username = alert.substring(0, alert.indexOf(':'));
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("username", username);
            return Utils.getCroppedBitmap(Utils.getUserIcon(query.getFirst()));
        } catch (Exception e) {
            Log.e("error", e.getLocalizedMessage());
            return null;
        }
    }
}
