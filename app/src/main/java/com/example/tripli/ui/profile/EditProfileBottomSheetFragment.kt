package com.example.tripli.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.example.tripli.databinding.FragmentEditProfileSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import java.io.File

class EditProfileBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentEditProfileSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private var selectedPhotoUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            Picasso.get().load(uri).fit().centerCrop().into(binding.editPhotoPreview)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.user.value?.let { user ->
            binding.nameEditText.setText(user.displayName)
            binding.bioEditText.setText(user.bio.orEmpty())

            val localFile = user.localPhotoPath?.let { File(it) }?.takeIf { it.exists() }
            when {
                localFile != null -> Picasso.get().load(localFile).fit().centerCrop().into(binding.editPhotoPreview)
                !user.photoUrl.isNullOrBlank() -> Picasso.get().load(user.photoUrl).fit().centerCrop().into(binding.editPhotoPreview)
            }
        }

        binding.changePhotoButton.setOnClickListener { pickImage.launch("image/*") }

        binding.saveButton.setOnClickListener {
            val name = binding.nameEditText.text?.toString()?.trim().orEmpty()
            val bio = binding.bioEditText.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                binding.nameEditText.error = "Name cannot be empty"
                return@setOnClickListener
            }
            viewModel.updateProfile(name, bio, selectedPhotoUri)
            dismiss()
        }

        viewModel.isSaving.observe(viewLifecycleOwner) { saving ->
            binding.saveButton.isEnabled = !saving
            binding.saveProgressIndicator.visibility = if (saving) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
