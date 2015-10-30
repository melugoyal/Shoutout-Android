package shoutout2.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import shoutout2.app.MapView.MapActivity;
import shoutout2.app.Utils.Utils;

public class MessageArrayAdapter<T> extends ArrayAdapter<T> {
    private List<ParseObject> messageObjects = new ArrayList<>();
    private Map<String, Person> people;
    public MessageArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects, Map<String, Person> people) {
        super(context, resource, textViewResourceId, objects);
        for (T object : objects) {
            messageObjects.add((ParseObject)object);
        }
        this.people = people;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = super.getView(position, convertView, parent);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.icon);
        TextView statusText = (TextView) itemView.findViewById(R.id.label);
        TextView usernameText = (TextView) itemView.findViewById(R.id.sender_username);
        ParseObject messageObject = messageObjects.get(position);
        ParseUser user;
        try {
            user = messageObject.fetchIfNeeded().getParseUser("from");
        } catch (Exception e) {
            user = null;
        }
        statusText.setText("");
        String message = "";
        String username = "";
        try {
            message = messageObject.fetchIfNeeded().getString("message");
            username = user.fetchIfNeeded().getUsername();
        } catch (Exception e) {
            Log.e("ERROR SHOWING MESSAGE", messageObject.getObjectId() + " " + e.getLocalizedMessage());
        }
        if (!messageObject.getBoolean("read")) {
            messageObject.put("read", true);
            messageObject.saveInBackground();
        }
        statusText.setText(message);
        usernameText.setText(username);
        try {
            Person person = people.get(user.fetchIfNeeded().getObjectId());
            Bitmap icon;
            if (person == null || person.icon == null) {
                icon = Utils.getCroppedBitmap(Utils.getUserIcon(user));
            } else {
                icon = person.icon;
            }
            imageView.setImageDrawable(new BitmapDrawable(Resources.getSystem(), icon));
        } catch (Exception e) {
            Log.e("IMAGE FOR MESSAGE VIEW", messageObject.getObjectId() + " " + username + " " + e.getLocalizedMessage());
        }
        return itemView;
    }
}
