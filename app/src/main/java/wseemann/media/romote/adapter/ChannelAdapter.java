package wseemann.media.romote.adapter;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.List;

import wseemann.media.romote.R;

import com.bumptech.glide.RequestManager;
import com.jaku.model.Channel;
import wseemann.media.romote.utils.CommandHelper;

/**
 * The main adapter that backs the GridView. This is fairly standard except the number of
 * columns in the GridView is used to create a fake top row of empty views as we use a
 * transparent ActionBar and don't want the real top row of images to start off covered by it.
 */
public class ChannelAdapter extends ArrayAdapter<Channel> {

    private Context context;
    private RequestManager requestManager;
    private List<Channel> mChannels;
    private Handler mHandler;
    private CommandHelper commandHelper;

    private int mItemHeight = 0;
    private int mNumColumns = 0;
    private FrameLayout.LayoutParams mImageViewLayoutParams;

    public ChannelAdapter(Context context, RequestManager requestManager, List<Channel> channels, Handler handler, CommandHelper commandHelper) {
        super(context, R.layout.empty_device_list, channels);
        this.context = context;
        this.requestManager = requestManager;
        mChannels = channels;
        mImageViewLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mHandler = handler;
        this.commandHelper = commandHelper;
    }

    @Override
    public int getCount() {
        // If columns have yet to be determined, return no items
        if (getNumColumns() == 0) {
            return 0;
        }

        // Size + number of columns for top empty row
        return mChannels.size();
    }

    private class ViewHolder {
        ImageView mImageView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        // Now handle the main ImageView thumbnails
        ViewHolder holder = null;
        LayoutInflater mInflater = (LayoutInflater)
                context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) { // if it's not recycled, instantiate and initialize
            convertView = mInflater.inflate(R.layout.list_item_grid, null);
            holder = new ViewHolder();
            holder.mImageView = (ImageView) convertView.findViewById(android.R.id.icon);
            holder.mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.mImageView.setLayoutParams(mImageViewLayoutParams);
            convertView.setTag(holder);
        } else { // Otherwise re-use the converted view
            holder = (ViewHolder) convertView.getTag();
        }

        // Check the height matches our calculated column width
        if (holder.mImageView.getLayoutParams().height != mItemHeight) {
            holder.mImageView.setLayoutParams(mImageViewLayoutParams);
        }

        final Channel channel = getItem(position);

        // Finally load the image asynchronously into the ImageView, this also takes care of
        // setting a placeholder image while the background thread runs
        requestManager.load(Uri.parse(commandHelper.getIconURL(channel.getId())))
                .into(holder.mImageView);

        return convertView;
    }

    /**
     * Sets the item height. Useful for when we know the column width so the height can be set
     * to match.
     *
     * @param height
     */
    public void setItemHeight(int height) {
        if (height == mItemHeight) {
            return;
        }
        mItemHeight = height;
        mImageViewLayoutParams =
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mItemHeight);
        notifyDataSetChanged();
    }

    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    public int getChannelCount() {
        return mChannels.size();
    }
}