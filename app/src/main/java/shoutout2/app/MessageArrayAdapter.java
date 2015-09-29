package shoutout2.app;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
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
    private ImageButton messageButton;
    public MessageArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects, Map<String, Person> people, ImageButton messageButton) {
        super(context, resource, textViewResourceId, objects);
        for (T object : objects) {
            messageObjects.add((ParseObject)object);
        }
        this.people = people;
        this.messageButton = messageButton;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = super.getView(position, convertView, parent);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.icon);
        TextView text = (TextView) itemView.findViewById(R.id.label);
        if (position == 0) {
            imageView.setImageResource(R.drawable.message);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    messageButton.callOnClick();
                }
            });
            text.setText("Inbox");
            text.setTypeface(null, Typeface.BOLD);
        }
        else {
            ParseObject messageObject = messageObjects.get(position);
            text.setText(messageObject.getString("message"));
            if (!messageObject.getBoolean("read")) {
                text.setTypeface(null, Typeface.BOLD);
                messageObject.put("read", true);
                messageObject.saveInBackground();
            }
            else {
                text.setTypeface(null, Typeface.NORMAL);
            }
            imageView.setImageBitmap(people.get(messageObject.getParseUser("from").getObjectId()).icon);
        }
        return itemView;
    }
}
