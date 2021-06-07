package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.GigLabel

class EditGigLabelNameDialogFragment(private val gigLabel: GigLabel, private val positiveClickListener: (newName: String) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        // ref: https://stackoverflow.com/a/17502042/2445763
        val textEnter = LayoutInflater.from(context).inflate(R.layout.dialog_gig_labels_name_input, null)
        val userInput = textEnter.findViewById<EditText>(R.id.gigLabelNameInputEditText)
        userInput.setText(gigLabel.name)

        builder
            .setView(textEnter)
            .setTitle(" ")
            .setPositiveButton("Save") { dialog, id ->
                val newName = userInput.text.toString()
                positiveClickListener(newName)
            }
            .setNeutralButton("Cancel") { dialog, id ->

            }

        return builder.create()
    }
}