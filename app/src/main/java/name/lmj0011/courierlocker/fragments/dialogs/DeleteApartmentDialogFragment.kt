package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import name.lmj0011.courierlocker.database.Apartment

class DeleteApartmentDialogFragment(private val apt: Apartment, private val positiveClickListener: (apt: Apartment) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder
            .setTitle("Delete this Map?")
            .setMessage(apt.name)
            .setPositiveButton("Yes") { dialog, id ->
                positiveClickListener(apt)
            }
            .setNeutralButton("No") { dialog, id ->

            }

        return builder.create()
    }
}