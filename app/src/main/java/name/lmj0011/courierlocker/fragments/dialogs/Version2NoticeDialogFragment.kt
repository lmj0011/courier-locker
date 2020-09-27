package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import name.lmj0011.courierlocker.R


class Version2NoticeDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let{
            val builder = AlertDialog.Builder(it)

            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_v2_update_notice, null)

            view.findViewById<Button>(R.id.v2_update_notice_btn).setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://github.com/lmj0011/courier-locker/wiki/Version-2-Update-Guide")
                startActivity(intent)
            }

            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        PreferenceManager.getDefaultSharedPreferences(activity).edit().apply {
            putBoolean(resources.getString(R.string.sp_key_v2_update_notice), false)
            apply()
        }

    }
}