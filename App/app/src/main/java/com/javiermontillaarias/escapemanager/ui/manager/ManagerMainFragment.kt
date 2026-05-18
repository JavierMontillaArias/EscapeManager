package com.javiermontillaarias.escapemanager.ui.manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.databinding.FragmentManagerMainBinding
import com.javiermontillaarias.escapemanager.ui.manager.bookings.BookingsFragment
import com.javiermontillaarias.escapemanager.ui.manager.dashboard.ManagerDashboardFragment
import com.javiermontillaarias.escapemanager.ui.manager.incidents.IncidentsFragment
import com.javiermontillaarias.escapemanager.ui.manager.rooms.RoomsFragment
import com.javiermontillaarias.escapemanager.ui.manager.stats.StatsFragment
import com.javiermontillaarias.escapemanager.util.performLogout

class ManagerMainFragment : Fragment() {

    private var _binding: FragmentManagerMainBinding? = null
    private val binding get() = _binding!!

    private val fragmentMap = mutableMapOf<Int, Fragment>()

    // B-5: variable de instancia para guardar el tab activo antes de que _binding se anule,
    // evitando NPE si onSaveInstanceState() se llama tras onDestroyView().
    private var activeTabId: Int = R.id.nav_dashboard

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            showFragment(R.id.nav_dashboard, TAG_DASHBOARD) { ManagerDashboardFragment() }
        } else {
            // CAL-03: restaurar qué tab estaba activo antes de la recreación
            val restoredTabId = savedInstanceState.getInt(KEY_ACTIVE_TAB, R.id.nav_dashboard)
            activeTabId = restoredTabId

            // BUG-02: repoblar el mapa con los fragments restaurados por el sistema
            childFragmentManager.findFragmentByTag(TAG_DASHBOARD)?.let { fragmentMap[R.id.nav_dashboard]  = it }
            childFragmentManager.findFragmentByTag(TAG_ROOMS)?.let    { fragmentMap[R.id.nav_rooms]       = it }
            childFragmentManager.findFragmentByTag(TAG_BOOKINGS)?.let { fragmentMap[R.id.nav_bookings]    = it }
            childFragmentManager.findFragmentByTag(TAG_STATS)?.let    { fragmentMap[R.id.nav_stats]       = it }
            childFragmentManager.findFragmentByTag(TAG_INCIDENTS)?.let{ fragmentMap[R.id.nav_incidents]   = it }

            // Mostrar el fragment que estaba activo
            val activeTag = tagForId(restoredTabId)
            showFragment(restoredTabId, activeTag) { createFragmentForId(restoredTabId) }
            binding.bottomNav.selectedItemId = restoredTabId
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            activeTabId = item.itemId
            when (item.itemId) {
                R.id.nav_dashboard -> showFragment(R.id.nav_dashboard, TAG_DASHBOARD) { ManagerDashboardFragment() }
                R.id.nav_rooms     -> showFragment(R.id.nav_rooms,     TAG_ROOMS)     { RoomsFragment() }
                R.id.nav_bookings  -> showFragment(R.id.nav_bookings,  TAG_BOOKINGS)  { BookingsFragment() }
                R.id.nav_stats     -> showFragment(R.id.nav_stats,     TAG_STATS)     { StatsFragment() }
                R.id.nav_incidents -> showFragment(R.id.nav_incidents,  TAG_INCIDENTS) { IncidentsFragment() }
            }
            true
        }

        binding.btnLogout.setOnClickListener { performLogout() }
    }

    // CAL-03: guardar el tab activo para restaurarlo tras recreación de Activity
    // B-5: usar activeTabId (variable de instancia) en lugar de binding.bottomNav.selectedItemId
    //      porque _binding puede ser null cuando onSaveInstanceState se llama tras onDestroyView.
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_ACTIVE_TAB, activeTabId)
    }

    private fun showFragment(id: Int, tag: String, create: () -> Fragment) {
        val fm = childFragmentManager
        val current = fm.fragments.firstOrNull { it.isVisible }
        val target = fragmentMap.getOrPut(id) {
            fm.findFragmentByTag(tag) ?: create()
        }

        fm.beginTransaction().apply {
            current?.let { hide(it) }
            if (!target.isAdded) add(R.id.container, target, tag)
            else show(target)
        }.commit()
    }

    private fun tagForId(id: Int) = when (id) {
        R.id.nav_dashboard -> TAG_DASHBOARD
        R.id.nav_rooms     -> TAG_ROOMS
        R.id.nav_bookings  -> TAG_BOOKINGS
        R.id.nav_stats     -> TAG_STATS
        R.id.nav_incidents -> TAG_INCIDENTS
        else               -> TAG_DASHBOARD
    }

    private fun createFragmentForId(id: Int): Fragment = when (id) {
        R.id.nav_dashboard -> ManagerDashboardFragment()
        R.id.nav_rooms     -> RoomsFragment()
        R.id.nav_bookings  -> BookingsFragment()
        R.id.nav_stats     -> StatsFragment()
        R.id.nav_incidents -> IncidentsFragment()
        else               -> ManagerDashboardFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG_DASHBOARD = "m_dashboard"
        private const val TAG_ROOMS     = "m_rooms"
        private const val TAG_BOOKINGS  = "m_bookings"
        private const val TAG_STATS     = "m_stats"
        private const val TAG_INCIDENTS = "m_incidents"
        private const val KEY_ACTIVE_TAB = "active_tab"
    }
}
