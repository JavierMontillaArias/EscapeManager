package com.javiermontillaarias.escapemanager.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.AuthRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentLoginBinding
import com.javiermontillaarias.escapemanager.util.Resource

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val viewModel: LoginViewModel by viewModels {
        val api = RetrofitClient.getApiService(sessionManager)
        LoginViewModelFactory(AuthRepository(api, sessionManager))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Si ya está logueado, navega directamente
        if (sessionManager.isLoggedIn) navigateAfterLogin()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.login(email, password)
        }

        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Idle -> Unit
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    navigateAfterLogin()
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.tvError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun navigateAfterLogin() {
        val destination = if (sessionManager.isManager) {
            R.id.managerMainFragment
        } else {
            R.id.gmMainFragment
        }
        findNavController().navigate(destination)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}