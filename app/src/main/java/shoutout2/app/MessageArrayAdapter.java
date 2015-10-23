package shoutout2.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.parse.ParseObject;

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
        statusText.setText("");
        String message = "";
        String username = "";
        try {
            message = messageObject.fetchIfNeeded().getString("message");
            username = messageObject.fetchIfNeeded().getParseUser("from").fetchIfNeeded().getUsername();
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
            imageView.setImageBitmap(people.get(messageObject.fetchIfNeeded().getParseUser("from").fetchIfNeeded().getObjectId()).emptyStatusIcon);
        } catch (Exception e) {
            Log.e("IMAGE FOR MESSAGE VIEW", messageObject.getObjectId() + " " + people.get(messageObject.getParseUser("from").getObjectId()));
        }
        return itemView;
    }
}
