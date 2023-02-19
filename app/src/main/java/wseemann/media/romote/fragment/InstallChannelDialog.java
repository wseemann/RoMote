package wseemann.media.romote.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.utils.CommandHelper;

/**
 * Created by wseemann on 6/20/16.
 */
@AndroidEntryPoint
public class InstallChannelDialog extends DialogFragment {

    @Inject
    protected CommandHelper commandHelper;

    private String mChannelCode;

    private static InstallChannelListener mListener;

    public static InstallChannelDialog getInstance(Activity activity, String channelCode) {
        try {
            // Instantiate the InstallChannelListener so we can send events with it
            mListener = (InstallChannelListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement InstallChannelListener");
        }

        InstallChannelDialog fragment = new InstallChannelDialog();

        Bundle args = new Bundle();
        args.putString("channel_code", channelCode);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(false);

        mChannelCode = getArguments().getString("channel_code");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_install_channel, null);
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);

        Glide.with(this)
                .load(Uri.parse(commandHelper.getIconURL(mChannelCode)))
                .into(imageView);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setTitle(getString(R.string.install_channel_dialog_title));
        builder.setMessage(getString(R.string.install_channel_dialog_message) + mChannelCode + "?");
        builder.setPositiveButton(R.string.install_channel_dialog_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (mListener != null) {
                    mListener.onDialogCancelled(InstallChannelDialog.this);
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (mListener != null) {
                    mListener.onDialogCancelled(InstallChannelDialog.this);
                }
            }
        });

        return builder.create();
    }

    public interface InstallChannelListener {
        public void onDialogCancelled(DialogFragment dialog);
        public void onInstallSelected(DialogFragment dialog);
    }
}
