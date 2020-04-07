package name.lmj0011.courierlocker.fragments

import android.content.SharedPreferences
import android.location.Address
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import br.com.simplepass.loadingbutton.presentation.State
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.Stop
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.databinding.FragmentEditMapFeedsBinding
import name.lmj0011.courierlocker.databinding.FragmentEditTripBinding
import name.lmj0011.courierlocker.factories.TripViewModelFactory
import name.lmj0011.courierlocker.fragments.dialogs.DeleteTripDialogFragment
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import name.lmj0011.courierlocker.helpers.getTripDate
import timber.log.Timber

/**
 * A simple [Fragment] subclass.
 *
 */
class EditMapFeedsFragment : Fragment() {
    private lateinit var binding: FragmentEditMapFeedsBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_edit_map_feeds, container, false)

        mainActivity = activity as MainActivity

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        binding.editMapFeedsSaveCircularProgressButton.setOnClickListener(this::saveButtonOnClickListener)

        mainActivity.hideFab()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.supportActionBar?.subtitle = null
        binding.editMapFeedsEditText.setText(sharedPreferences.getString(resources.getString(R.string.sp_key_map_feed_list), ""))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun saveButtonOnClickListener(v: View) {
        binding.editMapFeedsSaveCircularProgressButton.isEnabled = false
        binding.editMapFeedsSaveCircularProgressButton.startAnimation()

        sharedPreferences.edit().apply {
            putString(resources.getString(R.string.sp_key_map_feed_list), binding.editMapFeedsEditText.text.toString())
            commit()
        }

        binding.editMapFeedsSaveCircularProgressButton.revertAnimation()
        binding.editMapFeedsSaveCircularProgressButton.isEnabled = true

        this.findNavController().popBackStack()
        mainActivity.showToastMessage("List Saved")
    }
}