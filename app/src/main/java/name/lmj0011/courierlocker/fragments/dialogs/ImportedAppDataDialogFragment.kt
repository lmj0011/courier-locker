package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import name.lmj0011.courierlocker.BuildConfig
import name.lmj0011.courierlocker.R

class ImportedAppDataDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context!!)
        val message = "database is being rebuilt.\n"+
                "Please wait for app to restart before using again.\n"+
                "Your app data should appear momentarily."

        builder.setMessage(message)

        builder.apply {
            setPositiveButton("OK") { _, _ ->
                // User clicked OK button
            }
        }

        return builder.create()
    }

    override fun onStart() {
        super.onStart()

        dialog?.findViewById<TextView>(android.R.id.message)?.gravity = Gravity.CENTER
    }
}