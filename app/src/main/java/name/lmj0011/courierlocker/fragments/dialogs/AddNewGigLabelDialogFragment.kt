package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import name.lmj0011.courierlocker.R

class AddNewGigLabelDialogFragment(private val positiveClickListener: (newName: String) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        // ref: https://stackoverflow.com/a/17502042/2445763
        val textEnter = LayoutInflater.from(context).inflate(R.layout.dialog_gig_labels_name_input, null)
        val userInput = textEnter.findViewById<EditText>(R.id.gigLabelNameInputEditText)

        builder
            .setView(textEnter)
            .setTitle("Create New Gig Label")
            .setPositiveButton("Create") { dialog, id ->
                val newName = userInput.text.toString()
                positiveClickListener(newName)
            }
            .setNeutralButton("Cancel") { dialog, id ->

            }

        return builder.create()
    }
}