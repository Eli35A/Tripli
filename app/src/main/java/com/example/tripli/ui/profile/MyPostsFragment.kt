package com.example.tripli.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tripli.databinding.FragmentMyPostsBinding

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
        adapter = ProfilePostAdapter { }
        binding.postsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.postsRecyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshMyPosts() }

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
