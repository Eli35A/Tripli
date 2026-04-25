package com.example.tripli.ui.addpost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tripli.R
import com.example.tripli.databinding.FragmentAddPostBinding

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!

    private var currentRating = 4
    private val starViews get() = listOf(
        binding.ivStar1,
        binding.ivStar2,
        binding.ivStar3,
        binding.ivStar4,
        binding.ivStar5
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupStarRating()
        setupChips()
        setupButtons()
    }

    private fun setupStarRating() {
        updateStars(currentRating)
        starViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                currentRating = index + 1
                updateStars(currentRating)
            }
        }
    }

    private fun updateStars(rating: Int) {
        val filledColor = ContextCompat.getColor(requireContext(), R.color.accentBlue)
        starViews.forEachIndexed { index, imageView ->
            imageView.alpha = if (index < rating) 1f else 0.3f
            imageView.setColorFilter(filledColor)
        }
        val ratingText = if (rating % 1 == 0) "$rating.0" else "$rating"
        binding.tvRatingValue.text = ratingText
    }

    private fun setupChips() {
        val chips = listOf(
            binding.chipAdventure,
            binding.chipFood,
            binding.chipRelaxation,
            binding.chipCulture
        )
        chips.forEach { chip ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    chip.setChipBackgroundColorResource(R.color.accentBlue)
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    chip.setChipBackgroundColorResource(R.color.white)
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
                }
            }
        }
    }

    private fun setupButtons() {
        binding.tvCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.tvSelectImages.setOnClickListener {
            // Image picker will be wired up when ready
        }

        binding.tvSharePost.setOnClickListener {
            // Submit logic will be wired up when ready
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
