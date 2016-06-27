package wseemann.media.romote.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import wseemann.media.romote.model.Entry;

/**
 * Created by wseemann on 6/19/16.
 */
public class DeviceInfoAdapter extends ArrayAdapter<Entry> {

    private Context mContext;

    public DeviceInfoAdapter(Context context, List<Entry> objects) {
        super(context, android.R.layout.simple_list_item_2, objects);
        mContext = context;
    }

    private class ViewHolder {
        TextView mText1;
        TextView mText2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        LayoutInflater mInflater = (LayoutInflater)
                mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(android.R.layout.simple_list_item_2, null);
            holder = new ViewHolder();
            holder.mText1 = (TextView) convertView.findViewById(android.R.id.text1);
            holder.mText2 = (TextView) convertView.findViewById(android.R.id.text2);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Entry entry = (Entry) getItem(position);

        holder.mText1.setText(entry.getKey() + ":");
        holder.mText2.setText(entry.getValue());

        return convertView;
    }
}
