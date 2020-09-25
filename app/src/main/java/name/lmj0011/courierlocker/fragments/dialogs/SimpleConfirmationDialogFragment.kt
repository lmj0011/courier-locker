package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import name.lmj0011.courierlocker.database.Apartment

class SimpleConfirmationDialogFragment(private val title: String, private val message: String, private val positiveClickListener: () -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context!!)

        builder
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { dialog, id ->
                positiveClickListener()
            }
            .setNegativeButton("No") { dialog, id ->

            }

        return builder.create()
    }
}