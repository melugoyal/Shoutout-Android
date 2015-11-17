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

import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import shoutout2.app.Utils.Utils;

public class ListViewArrayAdapter<T> extends ArrayAdapter<T> {
    private List<ParseUser> userObjects = new ArrayList<>();
    private Map<String, Person> people;
    public ListViewArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects, Map<String, Person> people) {
        super(context, resource, textViewResourceId, objects);
        for (T object : objects) {
            userObjects.add((ParseUser)object);
        }
        this.people = people;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = super.getView(position, convertView, parent);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.icon);
        TextView statusText = (TextView) itemView.findViewById(R.id.status);
        TextView usernameText = (TextView) itemView.findViewById(R.id.username);
        ParseUser user = userObjects.get(position);
        statusText.setText("");
        String status = "";
        String username = "";
        try {
            status = user.fetchIfNeeded().getString("status");
            username = user.fetchIfNeeded().getUsername();
        } catch (Exception e) {
            Log.e("ERROR", user.getObjectId() + " " + e.getLocalizedMessage());
        }
        statusText.setText(status);
        usernameText.setText(username);
        try {
            Person person = people.get(user.fetchIfNeeded().getObjectId());
            Bitmap icon;
            if (person == null || person.icon == null) {
                icon = Utils.getCroppedBitmap(Utils.getUserIcon(user));
            } else {
                icon = person.icon;
            }
            if (icon != null) {
                imageView.setImageDrawable(new BitmapDrawable(Resources.getSystem(), icon));
            }
        } catch (Exception e) {
            Log.e("IMAGE FOR MESSAGE VIEW", user.getObjectId() + " " + username + " " + e.getLocalizedMessage());
        }
        return itemView;
    }
}
