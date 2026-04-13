package com.example.tripli.ui.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tripli.databinding.FragmentCommentBottomSheetBinding
import com.example.tripli.di.ServiceLocator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CommentBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCommentBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        factoryProducer = { HomeViewModel.Factory(ServiceLocator.provideHomePostRepository()) }
    )

    private val adapter = CommentAdapter()
    private val postId: String by lazy { requireArguments().getString(ARG_POST_ID)!! }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let {
                BottomSheetBehavior.from(it).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommentBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupInput()
        homeViewModel.loadComments(postId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.commentsRecyclerView.adapter = null
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.commentsRecyclerView.apply {
            this.adapter = this@CommentBottomSheetFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        homeViewModel.comments.observe(viewLifecycleOwner) { comments ->
            binding.commentCountTextView.text = comments.size.toString()
            adapter.submitList(comments)
            binding.commentsRecyclerView.isVisible = comments.isNotEmpty()
            binding.emptyTextView.isVisible = comments.isEmpty()
            if (comments.isNotEmpty()) binding.commentsRecyclerView.scrollToPosition(comments.size - 1)
        }
    }

    private fun setupInput() {
        binding.sendButton.isEnabled = false
        binding.commentInput.addTextChangedListener { text ->
            binding.sendButton.isEnabled = !text.isNullOrBlank()
        }
        binding.sendButton.setOnClickListener {
            val text = binding.commentInput.text?.toString()?.trim() ?: return@setOnClickListener
            if (text.isBlank()) return@setOnClickListener
            homeViewModel.addComment(postId, text)
            binding.commentInput.text?.clear()
        }
    }

    companion object {
        const val TAG = "CommentBottomSheet"
        private const val ARG_POST_ID = "post_id"

        fun newInstance(postId: String) = CommentBottomSheetFragment().apply {
            arguments = Bundle().apply { putString(ARG_POST_ID, postId) }
        }
    }
}
