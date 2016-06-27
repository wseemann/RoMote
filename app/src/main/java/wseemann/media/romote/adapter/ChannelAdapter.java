package wseemann.media.romote.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import wseemann.media.romote.R;
import wseemann.media.romote.model.Channel;
import wseemann.media.romote.util.ImageFetcher;
import wseemann.media.romote.utils.CommandHelper;

/**
 * The main adapter that backs the GridView. This is fairly standard except the number of
 * columns in the GridView is used to create a fake top row of empty views as we use a
 * transparent ActionBar and don't want the real top row of images to start off covered by it.
 */
public class ChannelAdapter extends ArrayAdapter<Channel> {

    private final Context mContext;
    private ImageFetcher mImageFetcher;
    private List<Channel> mChannels;
    private Handler mHandler;

    private int mItemHeight = 0;
    private int mNumColumns = 0;
    private FrameLayout.LayoutParams mImageViewLayoutParams;

    public ChannelAdapter(Context context, ImageFetcher imageFetcher, List<Channel> channels, Handler handler) {
        super(context, R.layout.empty_device_list, channels);
        mContext = context;
        mImageFetcher = imageFetcher;
        mChannels = channels;
        mImageViewLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mHandler = handler;
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
        TextView mText1;
        ImageView mImageButton;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        // Now handle the main ImageView thumbnails
        ViewHolder holder = null;
        LayoutInflater mInflater = (LayoutInflater)
                mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) { // if it's not recycled, instantiate and initialize
            convertView = mInflater.inflate(R.layout.list_item_grid, null);
            holder = new ViewHolder();
            holder.mImageView = (ImageView) convertView.findViewById(android.R.id.icon);
            holder.mText1 = (TextView) convertView.findViewById(android.R.id.text1);
            //imageView = new RecyclingImageView(mContext);
            holder.mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.mImageView.setLayoutParams(mImageViewLayoutParams);
            holder.mImageButton = (ImageView) convertView.findViewById(R.id.overflow_button);
            convertView.setTag(holder);
        } else { // Otherwise re-use the converted view
            holder = (ViewHolder) convertView.getTag();
        }

        // Check the height matches our calculated column width
        if (holder.mImageView.getLayoutParams().height != mItemHeight) {
            holder.mImageView.setLayoutParams(mImageViewLayoutParams);
        }

        final Channel channel = getItem(position);

        holder.mText1.setText(channel.getTitle());
        holder.mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = mHandler.obtainMessage();
                v.setTag(channel);
                message.obj = v;
                mHandler.sendMessage(message);
            }
        });

        // Finally load the image asynchronously into the ImageView, this also takes care of
        // setting a placeholder image while the background thread runs
        mImageFetcher.loadImage(CommandHelper.getIconURL(mContext, channel.getId()), holder.mImageView);
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
        mImageFetcher.setImageSize(height);
        notifyDataSetChanged();
    }

    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
    }

    public int getNumColumns() {
        return mNumColumns;
    }
}