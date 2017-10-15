import android.app.AlertDialog
import android.app.Dialog
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import wseemann.media.romote.R
import wseemann.media.romote.utils.CommandHelper
import wseemann.media.romote.utils.DBUtils
import wseemann.media.romote.utils.PreferenceUtils

class SearchDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val inflater: LayoutInflater = activity.getLayoutInflater()

        var view: View = inflater.inflate(R.layout.dialog_fragment_manual_connection, null)
        var searchText: EditText = view.findViewById(R.id.ip_address_text) as EditText
        var searchButton: Button = view.findViewById(R.id.connect_button) as Button

        searchButton.setOnClickListener {
                var ipAddressText: EditText = searchText

                //sendCommand(CommandHelper.getDeviceInfoURL(ManualConnectionDialog.this.getContext(), mHost));
        }

        var builder: AlertDialog.Builder = AlertDialog.Builder(activity);
        builder.setView(view);
        builder.setTitle(getString(R.string.connect_manually));
        builder.setMessage(getString(R.string.connect_help));
        builder.setPositiveButton(R.string.install_channel_dialog_button, { dialog, id ->

        })

        return builder.create();
    }
}
