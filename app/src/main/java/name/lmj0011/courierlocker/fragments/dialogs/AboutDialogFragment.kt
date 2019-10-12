package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import name.lmj0011.courierlocker.BuildConfig
import name.lmj0011.courierlocker.R

class AboutDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context!!)

        builder
            .setMessage("Courier Locker \n\n version: ${BuildConfig.VERSION_NAME} \n\n build: ${resources.getString(R.string.app_build)}")


        return builder.create()
    }

    override fun onStart() {
        super.onStart()

        dialog?.findViewById<TextView>(android.R.id.message)?.gravity = Gravity.CENTER
    }
}