package name.lmj0011.courierlocker.fragments


import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.GigLabelListAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.GigLabel
import name.lmj0011.courierlocker.databinding.FragmentGigLabelsBinding
import name.lmj0011.courierlocker.factories.GigLabelViewModelFactory
import name.lmj0011.courierlocker.fragments.dialogs.AddNewGigLabelDialogFragment
import name.lmj0011.courierlocker.fragments.dialogs.EditGigLabelNameDialogFragment
import name.lmj0011.courierlocker.viewmodels.GigLabelViewModel
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 *
 */
class GigLabelsFragment : Fragment() {

    private lateinit var binding: FragmentGigLabelsBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var listAdapter: GigLabelListAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModelFactory: GigLabelViewModelFactory
    private lateinit var gigLabelViewModel: GigLabelViewModel
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var backoffListAdapterUpdate = false // set to true if you want the next call to submitListToAdapter to not do anything.
    private var fragmentJob: Job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_gig_labels, container, false)

        mainActivity = activity as MainActivity

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).gigLabelDao
        viewModelFactory = GigLabelViewModelFactory(dataSource, application)
        gigLabelViewModel = ViewModelProvider(this, viewModelFactory).get(GigLabelViewModel::class.java)

        listAdapter = GigLabelListAdapter( GigLabelListAdapter.GigLabelListener(
            mutableMapOf(
                "viewClickListener" to {_: GigLabel -> },
                "visibilityListener" to {g: GigLabel ->
                    backoffListAdapterUpdate = true
                    gigLabelViewModel.updateGig(g)

                    if (g.visible) {
                        mainActivity.showToastMessage(message = "${g.name} is now selectable when creating Trips")
                    } else {
                        mainActivity.showToastMessage(message = "${g.name} is now non-selectable when creating Trips")
                    }

                },
                "deleteListener" to {g: GigLabel ->
                    gigLabelViewModel.deleteGig(g.id)
                },
                "editListener" to {g: GigLabel ->
                    gigLabelViewModel.updateGig(g)
                },
                "addNewListener" to {g: GigLabel ->
                    gigLabelViewModel.insertGig(g)
                }
            )
        ), this)

        binding.gigLabelList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        // ref: https://medium.com/@yfujiki/drag-and-reorder-recyclerview-items-in-a-user-friendly-manner-1282335141e9
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {
            val listOfMovePairs = mutableListOf<Pair<Int, Int>>()
            lateinit var adapter: GigLabelListAdapter

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter = recyclerView.adapter as GigLabelListAdapter
                val dragFromPos =  viewHolder.adapterPosition
                val dragToPos = target.adapterPosition

                listOfMovePairs.add(Pair(dragFromPos, dragToPos))
                adapter.notifyItemMoved(dragFromPos, dragToPos)

                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                /**
                 * quite a bit going on here.
                 *
                 * We're iterating over the drag events collected in listOfMovePairs
                 * and updating the gigs' 'order' as we go, then committing the changes
                 * when done.
                 */
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    var listOfGigLabels = gigLabelViewModel.gigs.value

                    listOfGigLabels?.let {

                        listOfMovePairs.forEach { pair ->
                            val fromGigLabel = listOfGigLabels.find { it.order == pair.first}
                            val toGigLabel = listOfGigLabels.find { it.order == pair.second}

                            listOfGigLabels.remove(fromGigLabel)
                            listOfGigLabels.remove(toGigLabel)

                            fromGigLabel?.order = pair.second
                            toGigLabel?.order = pair.first

                            fromGigLabel?.let{
                                listOfGigLabels.add(it)
                            }
                            toGigLabel?.let{listOfGigLabels.add(it) }
                        }
                    }

                    backoffListAdapterUpdate = true // because changes are already reflected in the recyclerview
                    gigLabelViewModel.updateAllGigs(listOfGigLabels)
                    listOfMovePairs.clear()
                }
            }


            override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                  direction: Int) {
                // 4. Code block for horizontal swipe.
                //    ItemTouchHelper handles horizontal swipe as well, but
                //    it is not relevant with reordering. Ignoring here.
            }
        }

        itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding.gigLabelList)

        binding.gigLabelList.adapter = listAdapter

        binding.lifecycleOwner = this

        binding.gigLabelFloatingActionButton.setOnClickListener {
            val dialog = AddNewGigLabelDialogFragment { newName ->
                val gigLabel = GigLabel().apply {
                    name = newName
                }
                gigLabelViewModel.insertGig(gigLabel)
            }

            dialog.show(this.childFragmentManager, this.tag)
        }

        gigLabelViewModel.gigs.observe(viewLifecycleOwner, Observer {
            this.submitListToAdapter(it)
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.supportActionBar?.subtitle = null
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob?.cancel()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun submitListToAdapter (list: MutableList<GigLabel>) {
        if(backoffListAdapterUpdate) {
            backoffListAdapterUpdate = false

        } else {
            listAdapter.submitList(list)
            listAdapter.notifyDataSetChanged()
        }
    }

}
