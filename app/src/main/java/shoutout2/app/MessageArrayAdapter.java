package shoutout2.app;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import shoutout2.app.MyMapActivity.Person;

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
        ParseObject messageObject = messageObjects.get(position);
        View itemView = super.getView(position, convertView, parent);
        TextView text = (TextView) itemView.findViewById(R.id.label);
        text.setText(messageObject.getString("message"));
        ImageView imageView = (ImageView) itemView.findViewById(R.id.icon);
        Log.d("PEOPLE", people.size() + "");
        imageView.setImageBitmap(people.get(messageObject.getParseUser("from").getObjectId()).icon);
        return itemView;
    }
}
