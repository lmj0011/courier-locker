package name.lmj0011.courierlocker.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.databinding.FragmentEditMapFeedsBinding

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
        binding.editMapFeedsEditText.setText(sharedPreferences.getString(resources.getString(R.string.pref_key_map_feed_list), ""))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun saveButtonOnClickListener(v: View) {
        binding.editMapFeedsSaveCircularProgressButton.isEnabled = false
        binding.editMapFeedsSaveCircularProgressButton.startAnimation()

        sharedPreferences.edit().apply {
            putString(resources.getString(R.string.pref_key_map_feed_list), binding.editMapFeedsEditText.text.toString())
            commit()
        }

        binding.editMapFeedsSaveCircularProgressButton.revertAnimation()
        binding.editMapFeedsSaveCircularProgressButton.isEnabled = true

        this.findNavController().popBackStack()
        mainActivity.showToastMessage("List Saved")
    }
}