package com.trackasia.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.trackasia.sample.adapter.WaypointAdapter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MapDirectionPointFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapDirectionPointFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map_direction_point, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager: ViewPager = view.findViewById(R.id.vpDirectionpoint)
        val tabLayout: TabLayout = view.findViewById(R.id.tlDirectionPoint)

        // Create an adapter for the ViewPager (you need to implement this adapter)
        val pagerAdapter = WaypointAdapter(childFragmentManager)

        // Set the adapter to the ViewPager
        viewPager.adapter = pagerAdapter

        // Connect the TabLayout to the ViewPager
        tabLayout.setupWithViewPager(viewPager)
    }
}