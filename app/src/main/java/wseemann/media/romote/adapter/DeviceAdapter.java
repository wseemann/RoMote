package wseemann.media.romote.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import wseemann.media.romote.R;
import wseemann.media.romote.model.Device;

/**
 * Created by wseemann on 6/19/16.
 */
public class DeviceAdapter extends ArrayAdapter<Device> {

    private Context mContext;
    private Handler mHandler;

    public DeviceAdapter(Context context, List<Device> objects, Handler handler) {
        super(context, R.layout.device, objects);
        mContext = context;
        mHandler = handler;
    }

    private class ViewHolder {
        TextView mText1;
        TextView mText2;
        ImageView mImageButton;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        LayoutInflater mInflater = (LayoutInflater)
                mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.device, null);
            holder = new ViewHolder();
            holder.mText1 = (TextView) convertView.findViewById(android.R.id.text1);
            holder.mText2 = (TextView) convertView.findViewById(android.R.id.text2);
            holder.mImageButton = (ImageView) convertView.findViewById(R.id.overflow_button);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Device device = (Device) getItem(position);

        holder.mText1.setText(device.getModelName()); //device.getUserDeviceName());
        holder.mText2.setText("SN: " + device.getSerialNumber());
        holder.mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = mHandler.obtainMessage();
                v.setTag(device);
                message.obj = v;
                mHandler.sendMessage(message);
            }
        });

        return convertView;
    }
}
