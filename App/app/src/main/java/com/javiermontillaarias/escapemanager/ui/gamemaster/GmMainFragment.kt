package com.javiermontillaarias.escapemanager.ui.gamemaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.databinding.FragmentGmMainBinding
import com.javiermontillaarias.escapemanager.ui.gamemaster.dashboard.GmDashboardFragment
import com.javiermontillaarias.escapemanager.ui.gamemaster.incidents.CreateIncidentFragment
import com.javiermontillaarias.escapemanager.ui.gamemaster.qrscanner.QrScannerFragment
import com.javiermontillaarias.escapemanager.util.appSessionManager
import com.javiermontillaarias.escapemanager.util.performLogout

class GmMainFragment : Fragment() {

    private var _binding: FragmentGmMainBinding? = null
    private val binding get() = _binding!!

    private val fragmentMap = mutableMapOf<Int, Fragment>()

    // B-5: variable de instancia para guardar el tab activo antes de que _binding se anule,
    // evitando NPE si onSaveInstanceState() se llama tras onDestroyView().
    private var activeTabId: Int = R.id.nav_gm_dashboard

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGmMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "Hola, ${appSessionManager.userName ?: "Game Master"}"

        if (savedInstanceState == null) {
            showFragment(R.id.nav_gm_dashboard, TAG_DASHBOARD) { GmDashboardFragment() }
            binding.bottomNav.selectedItemId = R.id.nav_gm_dashboard
        } else {
            // CAL-03: restaurar qué tab estaba activo antes de la recreación
            val restoredTabId = savedInstanceState.getInt(KEY_ACTIVE_TAB, R.id.nav_gm_dashboard)
            activeTabId = restoredTabId

            // BUG-02: repoblar el mapa con los fragments restaurados por el sistema
            childFragmentManager.findFragmentByTag(TAG_DASHBOARD)?.let { fragmentMap[R.id.nav_gm_dashboard] = it }
            childFragmentManager.findFragmentByTag(TAG_QR)?.let { fragmentMap[R.id.nav_scan_qr] = it }
            childFragmentManager.findFragmentByTag(TAG_INCIDENTS)?.let { fragmentMap[R.id.nav_gm_incidents] = it }

            // Mostrar el fragment que estaba activo
            val activeTag = tagForId(restoredTabId)
            showFragment(restoredTabId, activeTag) { createFragmentForId(restoredTabId) }
            binding.bottomNav.selectedItemId = restoredTabId
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            activeTabId = item.itemId
            when (item.itemId) {
                R.id.nav_gm_dashboard -> showFragment(R.id.nav_gm_dashboard, TAG_DASHBOARD) { GmDashboardFragment() }
                R.id.nav_scan_qr      -> showFragment(R.id.nav_scan_qr,      TAG_QR)        { QrScannerFragment() }
                R.id.nav_gm_incidents -> showFragment(R.id.nav_gm_incidents,  TAG_INCIDENTS) { CreateIncidentFragment() }
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
        R.id.nav_gm_dashboard -> TAG_DASHBOARD
        R.id.nav_scan_qr      -> TAG_QR
        R.id.nav_gm_incidents -> TAG_INCIDENTS
        else                  -> TAG_DASHBOARD
    }

    private fun createFragmentForId(id: Int): Fragment = when (id) {
        R.id.nav_gm_dashboard -> GmDashboardFragment()
        R.id.nav_scan_qr      -> QrScannerFragment()
        R.id.nav_gm_incidents -> CreateIncidentFragment()
        else                  -> GmDashboardFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG_DASHBOARD = "gm_dashboard"
        private const val TAG_QR        = "gm_scan_qr"
        private const val TAG_INCIDENTS = "gm_incidents"
        private const val KEY_ACTIVE_TAB = "active_tab"
    }
}
