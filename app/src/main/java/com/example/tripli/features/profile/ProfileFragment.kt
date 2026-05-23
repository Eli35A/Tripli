package com.example.tripli.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tripli.R
import com.example.tripli.databinding.FragmentProfileBinding
import com.example.tripli.features.editpost.EditPostFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import java.io.File

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViewPager()
        observeViewModel()
        setFragmentResultListener(EditPostFragment.RESULT_KEY) { _, bundle ->
            val postId = bundle.getString(EditPostFragment.RESULT_POST_ID) ?: return@setFragmentResultListener
            val imageUrl = bundle.getString(EditPostFragment.RESULT_IMAGE_URL) ?: ""
            val location = bundle.getString(EditPostFragment.RESULT_LOCATION) ?: ""
            val rating = bundle.getFloat(EditPostFragment.RESULT_RATING)
            val caption = bundle.getString(EditPostFragment.RESULT_CAPTION) ?: ""
            val hashtags = bundle.getString(EditPostFragment.RESULT_HASHTAGS)
                ?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
            viewModel.onPostUpdated(postId, imageUrl, location, rating, caption, hashtags)
        }

        binding.editProfileButton.setOnClickListener {
            EditProfileBottomSheetFragment().show(childFragmentManager, "edit_profile")
        }

        binding.logoutButton.setOnClickListener { viewModel.signOut() }

        viewModel.loggedOut.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    private fun setupViewPager() {
        binding.profileViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment =
                if (position == 0) MyPostsFragment() else LikedPostsFragment()
        }
        TabLayoutMediator(binding.profileTabLayout, binding.profileViewPager) { tab, position ->
            tab.text = if (position == 0) "My Posts" else "Liked Posts"
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAll()
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            binding.profileNameTextView.text = user.displayName

            if (user.bio.isNullOrBlank()) {
                binding.profileBioTextView.visibility = View.GONE
            } else {
                binding.profileBioTextView.text = user.bio
                binding.profileBioTextView.visibility = View.VISIBLE
            }

            val localFile = user.localPhotoPath?.let { File(it) }?.takeIf { it.exists() }
            when {
                !user.photoUrl.isNullOrBlank() -> Picasso.get().load(user.photoUrl).fit().centerCrop().into(binding.profileImageView)
                localFile != null -> Picasso.get().load(localFile).fit().centerCrop().into(binding.profileImageView)
            }
        }

        viewModel.isLoadingProfile.observe(viewLifecycleOwner) { loading ->
            binding.profileLoadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
