package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import name.lmj0011.courierlocker.database.Apartment

class DeleteApartmentDialogFragment(private val apt: Apartment, private val positiveClickListener: (apt: Apartment) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())

        builder
            .setTitle("Delete this Map?")
            .setMessage("${apt.name}")
            .setPositiveButton("Yes") { dialog, id ->
                positiveClickListener(apt)
            }
            .setNegativeButton("No") { dialog, id ->

            }

        return builder.create()
    }
}