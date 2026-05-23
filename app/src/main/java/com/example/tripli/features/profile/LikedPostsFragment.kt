package com.example.tripli.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.tripli.databinding.FragmentLikedPostsBinding
import com.example.tripli.model.HomePost

class LikedPostsFragment : Fragment() {

    private var _binding: FragmentLikedPostsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var adapter: ProfilePostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLikedPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ProfilePostAdapter(onPostClick = { post -> navigateToPostDetail(post) })
        binding.postsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.postsRecyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshLikedPosts() }

        viewModel.likedPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            binding.emptyText.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.isLoadingLikedPosts.observe(viewLifecycleOwner) { loading ->
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
