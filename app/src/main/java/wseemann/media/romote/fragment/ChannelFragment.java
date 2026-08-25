package wseemann.media.romote.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;
import wseemann.media.romote.BuildConfig;
import wseemann.media.romote.R;
import wseemann.media.romote.adapter.ChannelAdapter;
import wseemann.media.romote.event.ChannelScreenUiEvent;
import wseemann.media.romote.util.Utils;
import wseemann.media.romote.utils.BroadcastUtils;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.viewmodels.ChannelScreenViewModel;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.model.Channel;
import com.wseemann.ecp.request.LaunchAppRequest;

import javax.inject.Inject;

/**
 * The main fragment that powers the ImageGridActivity screen. Fairly straight forward GridView
 * implementation with the key addition being the ImageWorker class w/ImageCache to load children
 * asynchronously, keeping the UI nice and smooth and caching thumbnails for quick retrieval. The
 * cache is retained over configuration changes like orientation change so the images are populated
 * quickly if, for example, the user rotates the device.
 */
@AndroidEntryPoint
public class ChannelFragment extends Fragment {

    private static final String TAG = "ImageGridFragment";

    @Inject
    protected CommandHelper commandHelper;

    private int mImageThumbSize;
    private int mImageThumbSpacing;
    private ChannelAdapter mAdapter;

    private SwipeRefreshLayout mSwiperefresh;

    private ChannelScreenViewModel channelScreenViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        channelScreenViewModel = new ViewModelProvider(this).get(ChannelScreenViewModel.class);

        setHasOptionsMenu(true);

        RequestManager requestManager = Glide.with(this);

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mAdapter = new ChannelAdapter(getActivity(), requestManager, new ArrayList<>(), commandHelper);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.UPDATE_DEVICE_BROADCAST);
        requireActivity().registerReceiver(mUpdateReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_channels, container, false);
        final GridView mGridView = v.findViewById(android.R.id.list);

        mSwiperefresh = v.findViewById(R.id.swiperefresh);
        // This method performs the actual data-refresh operation.
        // The method calls setRefreshing(false) when it's finished.
        mSwiperefresh.setOnRefreshListener(
                () -> {
                    // This method performs the actual data-refresh operation.
                    // The method calls setRefreshing(false) when it's finished.
                    channelScreenViewModel.onHandleEvent(ChannelScreenUiEvent.LoadChannelsEvent.INSTANCE);
                }
        );

        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener((parent, view, position, id) -> {
            Channel channel = (Channel) parent.getItemAtPosition(position);

            performLaunch(channel.getId());
            BroadcastUtils.Companion.sendUpdateDeviceBroadcast(requireContext());
        });

        // This listener is used to get the final width of the GridView and then calculate the
        // number of columns and the width of each column. The width of each column is variable
        // as the GridView has stretchMode=columnWidth. The column width is used to set the height
        // of each view so we get nice square thumbnails.
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(
                                    mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            if (numColumns > 0) {
                                final int columnWidth =
                                        (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
                                mAdapter.setNumColumns(numColumns);
                                mAdapter.setItemHeight(columnWidth);
                                if (BuildConfig.DEBUG) {
                                    Timber.tag(TAG).d("onCreateView - numColumns set to %s", numColumns);
                                }
                                if (Utils.hasJellyBean()) {
                                    mGridView.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                } else {
                                    mGridView.getViewTreeObserver()
                                            .removeGlobalOnLayoutListener(this);
                                }
                            }
                        }
                    }
                });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        channelScreenViewModel.getUiStateLiveData().observe(getViewLifecycleOwner(), state -> {
            mSwiperefresh.setRefreshing(state.isLoading());
            onLoadFinished(state.getChannels());
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        requireActivity().unregisterReceiver(mUpdateReceiver);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            channelScreenViewModel.onHandleEvent(ChannelScreenUiEvent.LoadChannelsEvent.INSTANCE);
            return true;
        }

        return false;
    }

    private void onLoadFinished(List<Channel> channels) {
        if (channels.isEmpty()) {
            //setListShown(true);
            return;
        }

        mAdapter.clear();
        mAdapter.notifyDataSetChanged();

        // Set the new devices in the adapter.
        for (int i = 0; i < channels.size(); i++) {
            mAdapter.add(channels.get(i));
        }

        mAdapter.notifyDataSetChanged();
    }

    private void showMenu(final View v) {
        PopupMenu popup = new PopupMenu(requireActivity(), v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                Channel channel = (Channel) v.getTag();

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, "Install this Roku channel (" +
                        channel.getTitle() + "):\n\n" +
                        "http://romote/" + channel.getId() + "\n\n" + "Sent using RoMote.");
                intent.setType("text/plain");
                startActivity(intent);
                return true;
            } else {
                return false;
            }
        });
        popup.inflate(R.menu.channel_menu);
        popup.show();
    }

    private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            channelScreenViewModel.onHandleEvent(ChannelScreenUiEvent.LoadChannelsEvent.INSTANCE);
        }
    };

    private void performLaunch(String appId) {
        String url = commandHelper.getDeviceURL();

        LaunchAppRequest launchAppIdRequest = new LaunchAppRequest(url, appId);
        launchAppIdRequest.sendAsync(new ResponseCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void unused) {

            }

            @Override
            public void onError(@NonNull Exception e) {

            }
        });
    }

    public void refresh() {
        if (mAdapter.getChannelCount() == 0) {
            channelScreenViewModel.onHandleEvent(ChannelScreenUiEvent.LoadChannelsEvent.INSTANCE);
        }
    }
}
