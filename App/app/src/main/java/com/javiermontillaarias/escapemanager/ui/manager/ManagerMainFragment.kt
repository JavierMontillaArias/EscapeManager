package com.javiermontillaarias.escapemanager.ui.manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.databinding.FragmentManagerMainBinding
import com.javiermontillaarias.escapemanager.ui.manager.bookings.BookingsFragment
import com.javiermontillaarias.escapemanager.ui.manager.dashboard.ManagerDashboardFragment
import com.javiermontillaarias.escapemanager.ui.manager.incidents.IncidentsFragment
import com.javiermontillaarias.escapemanager.ui.manager.rooms.RoomsFragment
import com.javiermontillaarias.escapemanager.ui.manager.stats.StatsFragment

class ManagerMainFragment : Fragment() {

    private var _binding: FragmentManagerMainBinding? = null
    private val binding get() = _binding!!
    private val sessionManager by lazy { SessionManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show initial fragment
        if (savedInstanceState == null) showFragment(ManagerDashboardFragment())

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> showFragment(ManagerDashboardFragment())
                R.id.nav_rooms -> showFragment(RoomsFragment())
                R.id.nav_bookings -> showFragment(BookingsFragment())
                R.id.nav_stats -> showFragment(StatsFragment())
                R.id.nav_incidents -> showFragment(IncidentsFragment())
            }
            true
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun showFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}