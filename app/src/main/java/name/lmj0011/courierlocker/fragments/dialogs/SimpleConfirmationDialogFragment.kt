package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SimpleConfirmationDialogFragment(private val title: String, private val message: String, private val positiveClickListener: () -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { dialog, id ->
                positiveClickListener()
            }
            .setNeutralButton("No") { dialog, id ->

            }

        return builder.create()
    }
}