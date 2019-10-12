package name.lmj0011.courierlocker.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.fragments.dialogs.AboutDialogFragment


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("about")?.setOnPreferenceClickListener {
            val dialog = AboutDialogFragment()
            dialog.show(childFragmentManager, "AboutDialogFragment")
            true
        }
    }
}