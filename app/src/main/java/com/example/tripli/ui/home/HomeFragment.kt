package com.example.tripli.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tripli.R
import com.example.tripli.databinding.FragmentHomeBinding
import com.example.tripli.di.ServiceLocator
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(ServiceLocator.provideHomePostRepository())
    }

    private lateinit var adapter: HomePostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.homeRecyclerView.adapter = null
        _binding = null
    }

    private fun setupRecyclerView() {
        adapter = HomePostAdapter(
            onLikeClick = viewModel::onLikeToggled,
            onSaveClick = viewModel::onSaveToggled,
            onCommentClick = { post ->
                CommentBottomSheetFragment.newInstance(post.id)
                    .show(childFragmentManager, CommentBottomSheetFragment.TAG)
            }
        )
        binding.homeRecyclerView.apply {
            this.adapter = this@HomeFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
        }
        binding.swipeRefreshLayout.apply {
            setColorSchemeColors(resources.getColor(R.color.accentBlue, null))
            setOnRefreshListener { viewModel.refresh() }
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            binding.emptyTextView.isVisible = posts.isEmpty() && viewModel.isLoading.value == false
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingIndicator.isVisible = isLoading
            if (!isLoading) {
                binding.emptyTextView.isVisible = viewModel.posts.value.isNullOrEmpty()
            }
        }
        viewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefreshLayout.isRefreshing = isRefreshing
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message == null) return@observe
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            viewModel.onErrorShown()
        }
    }
}
