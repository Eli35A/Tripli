package com.example.tripli.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tripli.databinding.FragmentMyPostsBinding
import com.example.tripli.features.common.PostOptionsBottomSheetFragment
import com.example.tripli.model.HomePost

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
            onPostClick = { post -> navigateToPostDetail(post) },
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
            val postId = bundle.getString(PostOptionsBottomSheetFragment.ARG_POST_ID) ?: return@setFragmentResultListener
            requireParentFragment().findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentToEditPostFragment(
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

    private fun navigateToPostDetail(post: HomePost) {
        requireParentFragment().findNavController().navigate(
            ProfileFragmentDirections.actionProfileFragmentToPostDetailFragment(
                postId = post.id,
                authorId = post.authorId,
                authorName = post.authorName,
                authorPhotoUrl = post.authorPhotoUrl,
                timeAgo = post.timeAgo,
                imageUrl = post.imageUrl,
                location = post.location,
                rating = post.rating,
                title = post.title,
                caption = post.caption,
                hashtags = post.hashtags.joinToString("|"),
                likeCount = post.likeCount,
                commentCount = post.commentCount
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
