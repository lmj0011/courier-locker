package name.lmj0011.courierlocker.fragments.dialogs


import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ClearAllCustomersDialogFragment : DialogFragment() {
    // Use this instance of the interface to deliver action events
    private lateinit var listener: NoticeDialogListener

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface NoticeDialogListener {
        fun onClearAllCustomersDialogPositiveClick(dialog: DialogFragment)
        fun onClearAllCustomersDialogNegativeClick(dialog: DialogFragment)
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = parentFragment as NoticeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(("$parentFragment must implement NoticeDialogListener"))
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return listener?.let {
            // Build the dialog and set up the button click handlers
            val builder = MaterialAlertDialogBuilder(requireContext())

            builder
                .setTitle("Clear All Customers?")
                .setMessage("This action cannot be undone!")
                .setPositiveButton("Yes") { dialog, id ->
                    // Send the positive button event back to the host activity
                    listener.onClearAllCustomersDialogPositiveClick(this)
                }
                .setNeutralButton("No") { dialog, id ->
                    // Send the negative button event back to the host activity
                    listener.onClearAllCustomersDialogNegativeClick(this)
                }

            builder.create()
        }
    }
}