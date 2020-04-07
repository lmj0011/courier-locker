package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import name.lmj0011.courierlocker.database.Building

class NavigateToAptBuildingDialogFragment(private val building: Building, private val aptName: String) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context!!)

        builder
            .setTitle("$aptName")
            .setMessage("Navigate to building ${building.number}?")
            .setPositiveButton("Yes") { dialog, id ->
                val gmmIntentUri: Uri = Uri.parse("google.navigation:q=${building.latitude},${building.longitude}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }
            .setNegativeButton("No") { dialog, id ->

            }

        return builder.create()
    }
}