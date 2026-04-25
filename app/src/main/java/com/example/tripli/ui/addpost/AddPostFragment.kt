package com.example.tripli.ui.addpost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tripli.R
import com.example.tripli.databinding.FragmentAddPostBinding
import com.example.tripli.di.ServiceLocator
import com.google.android.material.snackbar.Snackbar

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddPostViewModel by viewModels {
        AddPostViewModel.Factory(ServiceLocator.provideHomePostRepository())
    }

    private lateinit var imageAdapter: SelectedImageAdapter

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(uris)
        }
    }

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
        setupImageRecyclerView()
        setupStarRating()
        setupChips()
        setupButtons()
        observeViewModel()
    }

    private fun setupImageRecyclerView() {
        imageAdapter = SelectedImageAdapter()
        binding.rvSelectedImages.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
        }
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
        val accentColor = ContextCompat.getColor(requireContext(), R.color.accentBlue)
        starViews.forEachIndexed { index, imageView ->
            imageView.alpha = if (index < rating) 1f else 0.3f
            imageView.setColorFilter(accentColor)
        }
        binding.tvRatingValue.text = "$rating.0"
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
            pickImages.launch("image/*")
        }

        binding.tvSharePost.setOnClickListener {
            val selectedHashtags = buildList {
                if (binding.chipAdventure.isChecked) add("#Adventure")
                if (binding.chipFood.isChecked) add("#Food")
                if (binding.chipRelaxation.isChecked) add("#Relaxation")
                if (binding.chipCulture.isChecked) add("#Culture")
            }
            viewModel.submitPost(
                location = binding.etLocation.text.toString(),
                rating = currentRating.toFloat(),
                caption = binding.etReview.text.toString(),
                hashtags = selectedHashtags
            )
        }
    }

    private fun observeViewModel() {
        viewModel.selectedImages.observe(viewLifecycleOwner) { uris ->
            val hasImages = uris.isNotEmpty()
            binding.layoutPhotoPlaceholder.isVisible = !hasImages
            binding.rvSelectedImages.isVisible = hasImages
            binding.tvSelectImages.text = if (hasImages) "Add More" else "Select Images"
            imageAdapter.submitList(uris)
        }

        viewModel.isSubmitting.observe(viewLifecycleOwner) { isSubmitting ->
            binding.tvSharePost.isEnabled = !isSubmitting
            binding.tvSharePost.alpha = if (isSubmitting) 0.6f else 1f
        }

        viewModel.submitSuccess.observe(viewLifecycleOwner) { success ->
            if (!success) return@observe
            findNavController().navigate(R.id.action_addPostFragment_to_homeFragment)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message == null) return@observe
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            viewModel.onErrorShown()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvSelectedImages.adapter = null
        _binding = null
    }
}
