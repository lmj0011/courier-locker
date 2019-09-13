package name.lmj0011.courierlocker.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import name.lmj0011.courierlocker.R


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}