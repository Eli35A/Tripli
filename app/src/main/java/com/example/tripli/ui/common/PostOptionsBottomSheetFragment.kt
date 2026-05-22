package com.example.tripli.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.example.tripli.databinding.FragmentPostOptionsSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PostOptionsBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPostOptionsSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostOptionsSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val postId = requireArguments().getString(ARG_POST_ID)!!

        binding.editOption.setOnClickListener {
            setFragmentResult(RESULT_EDIT, bundleOf(
                ARG_POST_ID to postId,
                ARG_LOCATION to requireArguments().getString(ARG_LOCATION),
                ARG_RATING to requireArguments().getFloat(ARG_RATING),
                ARG_CAPTION to requireArguments().getString(ARG_CAPTION),
                ARG_HASHTAGS to requireArguments().getString(ARG_HASHTAGS),
                ARG_IMAGE_URL to requireArguments().getString(ARG_IMAGE_URL)
            ))
            dismiss()
        }

        binding.deleteOption.setOnClickListener {
            setFragmentResult(RESULT_DELETE, bundleOf(ARG_POST_ID to postId))
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PostOptionsSheet"
        const val RESULT_EDIT = "post_options_edit"
        const val RESULT_DELETE = "post_options_delete"

        const val ARG_POST_ID = "postId"
        const val ARG_LOCATION = "location"
        const val ARG_RATING = "rating"
        const val ARG_CAPTION = "caption"
        const val ARG_HASHTAGS = "hashtags"
        const val ARG_IMAGE_URL = "imageUrl"

        fun newInstance(
            postId: String,
            location: String,
            rating: Float,
            caption: String,
            hashtags: String,
            imageUrl: String
        ) = PostOptionsBottomSheetFragment().apply {
            arguments = bundleOf(
                ARG_POST_ID to postId,
                ARG_LOCATION to location,
                ARG_RATING to rating,
                ARG_CAPTION to caption,
                ARG_HASHTAGS to hashtags,
                ARG_IMAGE_URL to imageUrl
            )
        }
    }
}
