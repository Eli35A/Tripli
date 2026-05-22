package com.example.tripli.features.addpost

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.tripli.R
import com.example.tripli.databinding.FragmentAddPostBinding
import com.example.tripli.base.ServiceLocator
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
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
import android.widget.Filter

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddPostViewModel by viewModels {
        AddPostViewModel.Factory(ServiceLocator.provideHomePostRepository(requireContext()))
    }

    private var compressedImageBytes: ByteArray? = null
    private var currentRating = 0f
    private var searchJob: Job? = null
    private val cityList = mutableListOf<String>()
    private lateinit var cityAdapter: ArrayAdapter<String>

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
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupLocationAutocomplete()
        observeViewModel()
    }

    private fun setupUI() {
        binding.tvCancel.setOnClickListener { findNavController().popBackStack() }
        binding.flPhotoArea.setOnClickListener { pickImage.launch("image/*") }
        binding.btnSelectImages.setOnClickListener { pickImage.launch("image/*") }
        binding.btnSharePost.setOnClickListener { submitPost() }
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
        // AutoCompleteTextView's built-in filter re-filters our API results away,
        // so we bypass it entirely and show all items we put in the list.
        cityAdapter = object : ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_dropdown_item_1line, cityList
        ) {
            override fun getFilter() = object : Filter() {
                override fun performFiltering(c: CharSequence?) = FilterResults().apply {
                    values = cityList
                    count = cityList.size
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
                if (index < currentRating.toInt()) R.drawable.ic_star_active
                else R.drawable.ic_star_inactive
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
        val container = FrameLayout(requireContext()).apply {
            setPadding(pad, pad / 2, pad, 0)
        }
        val inputLayout = TextInputLayout(
            requireContext(), null,
            com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox
        ).apply {
            isHintEnabled = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val input = TextInputEditText(requireContext()).apply {
            hint = "e.g. Nature"
        }
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

    private fun submitPost() {
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

        val hashtags = mutableListOf<String>()
        val chipGroup = binding.chipGroupCategories
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.id == R.id.chipAdd) continue
            if (chip.isChecked) hashtags.add(chip.text.toString().removePrefix("#"))
        }
        viewModel.submitPost(compressedImageBytes, location, currentRating, caption, hashtags)
    }

    private fun observeViewModel() {
        viewModel.isSubmitting.observe(viewLifecycleOwner) { submitting ->
            binding.btnSharePost.isEnabled = !submitting
            binding.btnSharePost.text = if (submitting) "Sharing..." else "Share Post  →"
        }
        viewModel.submitSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.resetSubmitSuccess()
                findNavController().navigate(
                    R.id.homeFragment,
                    null,
                    NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.homeFragment, false)
                        .build()
                )
            }
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
}
