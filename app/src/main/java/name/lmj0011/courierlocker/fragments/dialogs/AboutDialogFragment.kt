package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import name.lmj0011.courierlocker.BuildConfig
import name.lmj0011.courierlocker.R

class AboutDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder
            .setMessage("Courier Locker \n\n version: ${BuildConfig.VERSION_NAME} \n\n build: ${resources.getString(R.string.app_build)}")


        return builder.create()
    }

    override fun onStart() {
        super.onStart()

        dialog?.findViewById<TextView>(android.R.id.message)?.gravity = Gravity.CENTER
    }
}