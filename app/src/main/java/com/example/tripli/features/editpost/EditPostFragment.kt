package com.example.tripli.features.editpost

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.tripli.R
import com.example.tripli.databinding.FragmentEditPostBinding
import com.example.tripli.base.ServiceLocator
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class EditPostFragment : Fragment() {

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditPostViewModel by viewModels {
        EditPostViewModel.Factory(ServiceLocator.provideHomePostRepository(requireContext()))
    }

    private val args: EditPostFragmentArgs by navArgs()

    private var compressedImageBytes: ByteArray? = null
    private var currentRating = 0f
    private var searchJob: Job? = null
    private val cityList = mutableListOf<String>()
    private lateinit var cityAdapter: ArrayAdapter<String>

    private val postId: String get() = args.postId
    private val existingImageUrl: String get() = args.imageUrl

    private val stars by lazy {
        listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            compressedImageBytes = compressImage(it)
            binding.llPhotoPlaceholder.visibility = View.GONE
            binding.ivPhotoPreview.visibility = View.VISIBLE
            binding.ivPhotoPreview.setImageURI(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefillForm()
        setupUI()
        setupLocationAutocomplete()
        observeViewModel()
    }

    private fun prefillForm() {
        val location = args.location
        val rating = args.rating
        val caption = args.caption
        val hashtags = args.hashtags
            ?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

        binding.etLocation.setText(location)
        binding.etCaption.setText(caption)
        currentRating = rating
        updateStars()

        if (existingImageUrl.isNotBlank()) {
            binding.llPhotoPlaceholder.visibility = View.GONE
            binding.ivPhotoPreview.visibility = View.VISIBLE
            if (existingImageUrl.startsWith("data:image")) {
                val b64 = existingImageUrl.substringAfter(",")
                val bytes = Base64.decode(b64, Base64.NO_WRAP)
                binding.ivPhotoPreview.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } else {
                Picasso.get().load(existingImageUrl).fit().centerCrop().into(binding.ivPhotoPreview)
            }
        }

        val defaultChipIds = setOf(R.id.chipAdventure, R.id.chipFood, R.id.chipRelaxation, R.id.chipCulture)
        val chipGroup = binding.chipGroupCategories
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.id in defaultChipIds) {
                val tag = chip.text.toString().removePrefix("#")
                chip.isChecked = hashtags.any { it.equals(tag, ignoreCase = true) }
            }
        }

        val defaultTags = setOf("Adventure", "Food", "Relaxation", "Culture")
        hashtags.filter { tag -> defaultTags.none { it.equals(tag, ignoreCase = true) } }
            .forEach { addCustomChip(it) }
    }

    private fun setupUI() {
        binding.tvCancel.setOnClickListener { findNavController().popBackStack() }
        binding.flPhotoArea.setOnClickListener { pickImage.launch("image/*") }
        binding.btnSelectImages.setOnClickListener { pickImage.launch("image/*") }
        binding.btnSharePost.setOnClickListener { submitEdit() }
        binding.chipAdd.setOnClickListener { showAddCategoryDialog() }

        binding.etCaption.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) binding.tilCaption.error = null
            }
        })

        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                currentRating = (index + 1).toFloat()
                updateStars()
                binding.tvRatingError.visibility = View.GONE
            }
        }
    }

    private fun setupLocationAutocomplete() {
        cityAdapter = object : ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_dropdown_item_1line, cityList
        ) {
            override fun getFilter() = object : Filter() {
                override fun performFiltering(c: CharSequence?) = FilterResults().apply {
                    values = cityList; count = cityList.size
                }
                override fun publishResults(c: CharSequence?, r: FilterResults?) = notifyDataSetChanged()
            }
        }
        binding.etLocation.setAdapter(cityAdapter)
        binding.etLocation.threshold = 2

        binding.etLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) binding.tilLocation.error = null
                val query = s?.toString()?.trim() ?: return
                if (query.length < 2) return
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(350)
                    val cities = fetchCities(query)
                    cityList.clear()
                    cityList.addAll(cities)
                    cityAdapter.notifyDataSetChanged()
                    if (cities.isNotEmpty() && binding.etLocation.isAttachedToWindow) {
                        binding.etLocation.showDropDown()
                    }
                }
            }
        })
    }

    private suspend fun fetchCities(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://photon.komoot.io/api/?q=$encoded&limit=8&lang=en")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Accept", "application/json")
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            parseCities(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseCities(json: String): List<String> {
        val results = mutableListOf<String>()
        val features = JSONObject(json).getJSONArray("features")
        for (i in 0 until features.length()) {
            val props = features.getJSONObject(i).getJSONObject("properties")
            val type = props.optString("type")
            if (type !in setOf("city", "town", "village", "municipality")) continue
            val name = props.optString("name").takeIf { it.isNotBlank() } ?: continue
            val country = props.optString("country").takeIf { it.isNotBlank() }
            val state = props.optString("state").takeIf { it.isNotBlank() && it != name }
            val display = buildString {
                append(name)
                if (state != null) append(", $state")
                if (country != null) append(", $country")
            }
            if (display !in results) results.add(display)
            if (results.size >= 5) break
        }
        return results
    }

    private fun updateStars() {
        stars.forEachIndexed { index, star ->
            star.setImageResource(
                if (index < currentRating.toInt()) R.drawable.ic_star_active else R.drawable.ic_star_inactive
            )
        }
        binding.tvRatingValue.text = String.format("%.1f", currentRating)
    }

    private fun compressImage(uri: Uri): ByteArray {
        val stream = requireContext().contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(stream)
        stream?.close()
        val scaled = scaleBitmap(original, maxW = 800, maxH = 600)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
        if (scaled != original) scaled.recycle()
        original.recycle()
        return out.toByteArray()
    }

    private fun scaleBitmap(bitmap: Bitmap, maxW: Int, maxH: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxW && h <= maxH) return bitmap
        val scale = minOf(maxW.toFloat() / w, maxH.toFloat() / h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    private fun showAddCategoryDialog() {
        val pad = (24 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(requireContext()).apply { setPadding(pad, pad / 2, pad, 0) }
        val inputLayout = TextInputLayout(
            requireContext(), null,
            com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox
        ).apply {
            isHintEnabled = false
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val input = TextInputEditText(requireContext()).apply { hint = "e.g. Nature" }
        inputLayout.addView(input)
        container.addView(inputLayout)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Category")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val tag = input.text.toString().trim()
                if (tag.isNotEmpty()) addCustomChip(tag)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addCustomChip(tag: String) {
        val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
            text = "#$tag"
            isCheckable = true
            isChecked = true
            isCloseIconVisible = true
            setOnCloseIconClickListener { binding.chipGroupCategories.removeView(this) }
        }
        val addIndex = binding.chipGroupCategories.indexOfChild(binding.chipAdd)
        binding.chipGroupCategories.addView(chip, addIndex)
    }

    private fun collectHashtags(): List<String> {
        val result = mutableListOf<String>()
        val chipGroup = binding.chipGroupCategories
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.id == R.id.chipAdd) continue
            if (chip.isChecked) result.add(chip.text.toString().removePrefix("#"))
        }
        return result
    }

    private fun submitEdit() {
        val location = binding.etLocation.text?.toString()?.trim() ?: ""
        val caption = binding.etCaption.text?.toString()?.trim() ?: ""
        var valid = true

        if (location.isBlank()) {
            binding.tilLocation.error = "Please enter a destination"
            valid = false
        } else {
            binding.tilLocation.error = null
        }

        if (currentRating == 0f) {
            binding.tvRatingError.visibility = View.VISIBLE
            valid = false
        } else {
            binding.tvRatingError.visibility = View.GONE
        }

        if (caption.isBlank()) {
            binding.tilCaption.error = "Please write a review"
            valid = false
        } else {
            binding.tilCaption.error = null
        }

        if (!valid) return
        viewModel.savePost(postId, compressedImageBytes, existingImageUrl, location, currentRating, caption, collectHashtags())
    }

    private fun observeViewModel() {
        viewModel.isSaving.observe(viewLifecycleOwner) { saving ->
            binding.btnSharePost.isEnabled = !saving
            binding.btnSharePost.text = if (saving) "Saving..." else "Save Changes  →"
        }
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (!success) return@observe
            viewModel.resetSaveSuccess()
            val newImageUrl = compressedImageBytes?.let {
                "data:image/jpeg;base64," + Base64.encodeToString(it, Base64.NO_WRAP)
            } ?: existingImageUrl
            setFragmentResult(
                RESULT_KEY,
                bundleOf(
                    RESULT_POST_ID to postId,
                    RESULT_IMAGE_URL to newImageUrl,
                    RESULT_LOCATION to (binding.etLocation.text?.toString()?.trim() ?: ""),
                    RESULT_RATING to currentRating,
                    RESULT_CAPTION to (binding.etCaption.text?.toString()?.trim() ?: ""),
                    RESULT_HASHTAGS to collectHashtags().joinToString("|")
                )
            )
            findNavController().popBackStack()
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
        }
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val RESULT_KEY = "edit_post_result"
        const val RESULT_POST_ID = "postId"
        const val RESULT_IMAGE_URL = "imageUrl"
        const val RESULT_LOCATION = "location"
        const val RESULT_RATING = "rating"
        const val RESULT_CAPTION = "caption"
        const val RESULT_HASHTAGS = "hashtags"
    }
}
