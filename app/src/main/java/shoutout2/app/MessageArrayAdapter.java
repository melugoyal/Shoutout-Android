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
    private ImageButton messageButton;
    private Resources res;
    public MessageArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects, Map<String, Person> people, ImageButton messageButton, Resources res) {
        super(context, resource, textViewResourceId, objects);
        for (T object : objects) {
            messageObjects.add((ParseObject)object);
        }
        this.people = people;
        this.messageButton = messageButton;
        this.res = res;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = super.getView(position, convertView, parent);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.icon);
        TextView text = (TextView) itemView.findViewById(R.id.label);
        if (position == 0) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
            params.width = messageButton.getWidth();
            params.height = messageButton.getHeight();
            imageView.setLayoutParams(params);

            imageView.setImageResource(R.drawable.message);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    messageButton.callOnClick();
                }
            });
            params = (RelativeLayout.LayoutParams) text.getLayoutParams();
            params.setMarginStart((int) res.getDimension(R.dimen.inbox_text_padding));
            text.setLayoutParams(params);
            text.setText("Inbox");
            text.setTypeface(null, Typeface.BOLD);
        }
        else {
            ParseObject messageObject = messageObjects.get(position);
            text.setText("");
            String htmlString = "";
            String message = "";
            String username = "";
            try {
                message = messageObject.fetchIfNeeded().getString("message");
                username = messageObject.fetchIfNeeded().getParseUser("from").fetchIfNeeded().getUsername();
                text.setText(Html.fromHtml(messageObject.fetchIfNeeded().getString("message") + "<br><br><b><small>-" + messageObject.fetchIfNeeded().getParseUser("from").fetchIfNeeded().getUsername() + "</small></b>"));
            } catch (Exception e) {
                Log.e("ERROR SHOWING MESSAGE", messageObject.getObjectId() + " " + e.getLocalizedMessage());
            }
            if (!messageObject.getBoolean("read")) {
                htmlString += "<b>" + message + "</b>";
                messageObject.put("read", true);
                messageObject.saveInBackground();
            } else {
                htmlString += message;
            }
            htmlString += "<br><br><b><small>-" + username + "</small></b>";
            text.setText(Html.fromHtml(htmlString));
            try {
                imageView.setImageBitmap(people.get(messageObject.fetchIfNeeded().getParseUser("from").fetchIfNeeded().getObjectId()).emptyStatusIcon);
            } catch (Exception e) {
                Log.e("IMAGE FOR MESSAGE VIEW", messageObject.getObjectId() + " " + people.get(messageObject.getParseUser("from").getObjectId()));
            }
        }
        return itemView;
    }
}
