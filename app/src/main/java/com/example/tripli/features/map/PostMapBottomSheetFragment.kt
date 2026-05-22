package com.example.tripli.features.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tripli.model.HomePost
import com.example.tripli.databinding.FragmentPostMapSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso

class PostMapBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPostMapSheetBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "PostMapSheet"

        private const val ARG_TITLE = "title"
        private const val ARG_CAPTION = "caption"
        private const val ARG_IMAGE_URL = "imageUrl"
        private const val ARG_LOCATION = "location"
        private const val ARG_RATING = "rating"
        private const val ARG_AUTHOR_NAME = "authorName"
        private const val ARG_TIME_AGO = "timeAgo"
        private const val ARG_HASHTAGS = "hashtags"

        fun newInstance(post: HomePost) = PostMapBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, post.title)
                putString(ARG_CAPTION, post.caption)
                putString(ARG_IMAGE_URL, post.imageUrl)
                putString(ARG_LOCATION, post.location)
                putFloat(ARG_RATING, post.rating)
                putString(ARG_AUTHOR_NAME, post.authorName)
                putString(ARG_TIME_AGO, post.timeAgo)
                putString(ARG_HASHTAGS, post.hashtags.joinToString(" "))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostMapSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()

        binding.sheetTitle.text = args.getString(ARG_TITLE)
        binding.sheetCaption.text = args.getString(ARG_CAPTION)
        binding.sheetLocation.text = args.getString(ARG_LOCATION)
        binding.sheetRating.text = String.format("%.1f", args.getFloat(ARG_RATING))
        binding.sheetAuthor.text = "by ${args.getString(ARG_AUTHOR_NAME)} · ${args.getString(ARG_TIME_AGO)}"
        binding.sheetHashtags.text = args.getString(ARG_HASHTAGS)

        val imageUrl = args.getString(ARG_IMAGE_URL)
        if (!imageUrl.isNullOrBlank()) {
            Picasso.get()
                .load(imageUrl)
                .fit()
                .centerCrop()
                .into(binding.sheetPostImage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
