package com.example.tripli.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tripli.R
import com.example.tripli.databinding.FragmentMyPostsBinding
import com.example.tripli.ui.common.PostOptionsBottomSheetFragment
import com.example.tripli.ui.editpost.EditPostFragment

class MyPostsFragment : Fragment() {

    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var adapter: ProfilePostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ProfilePostAdapter(
            onPostClick = { },
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
        binding.postsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.postsRecyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshMyPosts() }

        // Bottom sheet is shown via childFragmentManager, so listen on childFragmentManager
        childFragmentManager.setFragmentResultListener(PostOptionsBottomSheetFragment.RESULT_EDIT, viewLifecycleOwner) { _, bundle ->
            requireParentFragment().findNavController().navigate(
                R.id.editPostFragment,
                bundleOf(
                    EditPostFragment.ARG_POST_ID to bundle.getString(PostOptionsBottomSheetFragment.ARG_POST_ID),
                    EditPostFragment.ARG_LOCATION to bundle.getString(PostOptionsBottomSheetFragment.ARG_LOCATION),
                    EditPostFragment.ARG_RATING to bundle.getFloat(PostOptionsBottomSheetFragment.ARG_RATING),
                    EditPostFragment.ARG_CAPTION to bundle.getString(PostOptionsBottomSheetFragment.ARG_CAPTION),
                    EditPostFragment.ARG_HASHTAGS to bundle.getString(PostOptionsBottomSheetFragment.ARG_HASHTAGS),
                    EditPostFragment.ARG_IMAGE_URL to bundle.getString(PostOptionsBottomSheetFragment.ARG_IMAGE_URL)
                )
            )
        }

        childFragmentManager.setFragmentResultListener(PostOptionsBottomSheetFragment.RESULT_DELETE, viewLifecycleOwner) { _, bundle ->
            val postId = bundle.getString(PostOptionsBottomSheetFragment.ARG_POST_ID) ?: return@setFragmentResultListener
            val post = viewModel.myPosts.value?.find { it.id == postId } ?: return@setFragmentResultListener
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete") { _, _ -> viewModel.deletePost(post) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewModel.myPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            binding.emptyText.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.isLoadingMyPosts.observe(viewLifecycleOwner) { loading ->
            binding.loadingIndicator.visibility =
                if (loading && adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
