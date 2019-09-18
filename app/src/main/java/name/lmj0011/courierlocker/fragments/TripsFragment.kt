package name.lmj0011.courierlocker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.TripListAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentTripsBinding
import name.lmj0011.courierlocker.factories.TripViewModelFactory
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import timber.log.Timber

/**
 * A simple [Fragment] subclass.
 *
 */
class TripsFragment : Fragment() {
    private lateinit var binding: FragmentTripsBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: TripViewModelFactory
    private lateinit var tripViewModel: TripViewModel
    private lateinit var listAdapter: TripListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_trips, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).tripDao
        viewModelFactory = TripViewModelFactory(dataSource, application)
        tripViewModel = ViewModelProviders.of(this, viewModelFactory).get(TripViewModel::class.java)

        listAdapter = TripListAdapter(TripListAdapter.TripListener { tripId ->
            // TODO new Fragment needed - EditTripFragment
//            this.findNavController().navigate(GateCodesFragmentDirections.actionGateCodesFragmentToEditGateCodeFragment(tripId.toInt()))
            Toast.makeText(mainActivity, "tripId: $tripId", Toast.LENGTH_SHORT).show()
        })

        binding.tripList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        binding.tripList.adapter = listAdapter

        binding.tripViewModel = tripViewModel

        binding.lifecycleOwner = this

        tripViewModel.trips.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it)
        })

        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            binding.generateTripBtn.visibility = View.GONE
            binding.clearAllTripsBtn.visibility = View.GONE
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.showFabAndSetListener(this::fabOnClickListenerCallback, R.drawable.ic_fab_add)
        mainActivity.supportActionBar?.title = "Trips"
        mainActivity.supportActionBar?.subtitle = null
    }

    private fun fabOnClickListenerCallback() {
        this.findNavController().navigate(TripsFragmentDirections.actionTripsFragmentToCreateTripFragment())
    }
}