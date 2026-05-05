package com.example.tripli.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.tripli.R
import com.example.tripli.databinding.FragmentWelcomeBinding
import com.example.tripli.di.ServiceLocator
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WelcomeViewModel by viewModels {
        WelcomeViewModel.Factory(
            ServiceLocator.provideUserRepository(requireContext())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindActions()
        observeViewModel()
        viewModel.loadCurrentUser()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindActions() {
        binding.signOutButton.setOnClickListener {
            viewModel.signOut()
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user == null) return@observe

            binding.userNameTextView.text = user.displayName
            binding.userEmailTextView.text = user.email ?: getString(R.string.email_not_available)
            binding.savedStateTextView.text = getString(R.string.user_saved_message)

            if (!user.photoUrl.isNullOrBlank()) {
                Picasso.get()
                    .load(user.photoUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .fit()
                    .centerCrop()
                    .into(binding.profileImageView)
            } else {
                binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }

        viewModel.isSigningOut.observe(viewLifecycleOwner) { isSigningOut ->
            binding.signOutButton.isEnabled = !isSigningOut
            binding.signOutProgressIndicator.isVisible = isSigningOut
        }

        viewModel.signedOut.observe(viewLifecycleOwner) { signedOut ->
            if (!signedOut) return@observe

            findNavController().navigate(R.id.loginFragment)
            viewModel.onSignedOutNavigated()
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message == null) return@observe
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }
}
