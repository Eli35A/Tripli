package com.example.tripli.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tripli.R
import com.example.tripli.databinding.FragmentHomeBinding
import com.example.tripli.base.ServiceLocator
import com.example.tripli.features.common.PostOptionsBottomSheetFragment
import com.example.tripli.features.editpost.EditPostFragment
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(ServiceLocator.provideHomePostRepository(requireContext()))
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

        // Bottom sheet is shown via childFragmentManager, so listen on childFragmentManager
        childFragmentManager.setFragmentResultListener(PostOptionsBottomSheetFragment.RESULT_EDIT, viewLifecycleOwner) { _, bundle ->
            val postId = bundle.getString(PostOptionsBottomSheetFragment.ARG_POST_ID) ?: return@setFragmentResultListener
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToEditPostFragment(
                    postId = postId,
                    imageUrl = bundle.getString(PostOptionsBottomSheetFragment.ARG_IMAGE_URL) ?: "",
                    location = bundle.getString(PostOptionsBottomSheetFragment.ARG_LOCATION) ?: "",
                    rating = bundle.getFloat(PostOptionsBottomSheetFragment.ARG_RATING),
                    caption = bundle.getString(PostOptionsBottomSheetFragment.ARG_CAPTION) ?: "",
                    hashtags = bundle.getString(PostOptionsBottomSheetFragment.ARG_HASHTAGS)
                )
            )
        }

        childFragmentManager.setFragmentResultListener(PostOptionsBottomSheetFragment.RESULT_DELETE, viewLifecycleOwner) { _, bundle ->
            val postId = bundle.getString(PostOptionsBottomSheetFragment.ARG_POST_ID) ?: return@setFragmentResultListener
            val post = viewModel.posts.value?.find { it.id == postId } ?: return@setFragmentResultListener
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete") { _, _ -> viewModel.deletePost(post) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // EditPostFragment is a nav sibling, so its result arrives on parentFragmentManager
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.homeRecyclerView.adapter = null
        _binding = null
    }

    private fun setupRecyclerView() {
        adapter = HomePostAdapter(
            currentUserId = viewModel.currentUserId,
            onLikeClick = viewModel::onLikeToggled,
            onSaveClick = viewModel::onSaveToggled,
            onCommentClick = { post ->
                CommentBottomSheetFragment.newInstance(post.id)
                    .show(childFragmentManager, CommentBottomSheetFragment.TAG)
            },
            onMoreClick = { post ->
                PostOptionsBottomSheetFragment.newInstance(
                    postId = post.id,
                    location = post.location,
                    rating = post.rating,
                    caption = post.caption,
                    hashtags = post.hashtags.joinToString("|"),
                    imageUrl = post.imageUrl
                ).show(childFragmentManager, PostOptionsBottomSheetFragment.TAG)
            }
        )

        val layoutManager = LinearLayoutManager(requireContext())
        binding.homeRecyclerView.apply {
            this.adapter = this@HomeFragment.adapter
            this.layoutManager = layoutManager
            setHasFixedSize(false)
        }

        binding.homeRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = layoutManager.itemCount
                if (total > 0 && lastVisible >= total - 3 && viewModel.canLoadMore.value == true) {
                    viewModel.loadNextPage()
                }
            }
        })

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
        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            binding.loadMoreIndicator.isVisible = isLoadingMore
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message == null) return@observe
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            viewModel.onErrorShown()
        }
    }
}
