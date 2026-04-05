package com.example.tripli.ui.login

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Log
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
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.tripli.R
import com.example.tripli.databinding.FragmentLoginBinding
import com.example.tripli.di.ServiceLocator
import com.example.tripli.ui.common.HorizontalSpaceItemDecoration
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory(
            userRepository = ServiceLocator.provideUserRepository(requireContext()),
            featuredPostsRepository = ServiceLocator.provideFeaturedPostsRepository()
        )
    }

    private val featuredPostsAdapter = FeaturedPostsAdapter()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "googleSignInLauncher: result received, code=${result.resultCode}")
        if (result.resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "googleSignInLauncher: sign-in cancelled or failed")
            viewModel.onGoogleFlowFailed(getString(R.string.google_sign_in_cancelled))
            return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            Log.d(TAG, "googleSignInLauncher: successfully got account, idToken present=${!idToken.isNullOrBlank()}")
            if (idToken.isNullOrBlank()) {
                viewModel.onGoogleFlowFailed(getString(R.string.google_token_missing))
                return@registerForActivityResult
            }
            viewModel.signInWithGoogleIdToken(idToken)
        } catch (exception: ApiException) {
            Log.e(TAG, "googleSignInLauncher: ApiException code=${exception.statusCode}", exception)
            viewModel.onGoogleFlowFailed(
                getString(R.string.google_sign_in_failed_with_code, exception.statusCode)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFeaturedPostsCarousel()
        setupTermsText()
        bindActions()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.featuredPostsRecyclerView.adapter = null
        _binding = null
    }

    private fun setupFeaturedPostsCarousel() {
        binding.featuredPostsRecyclerView.apply {
            adapter = featuredPostsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            addItemDecoration(
                HorizontalSpaceItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.post_card_spacing)
                )
            )
        }

        PagerSnapHelper().attachToRecyclerView(binding.featuredPostsRecyclerView)
    }

    private fun setupTermsText() {
        val fullText = getString(R.string.login_terms_full_text)
        val termsText = getString(R.string.terms_of_service)
        val privacyText = getString(R.string.privacy_policy)
        val clickableColor = ContextCompat.getColor(requireContext(), R.color.termsLink)
        val spannable = SpannableString(fullText)

        applyLinkSpan(spannable, fullText, termsText, clickableColor)
        applyLinkSpan(spannable, fullText, privacyText, clickableColor)

        binding.termsTextView.text = spannable
        binding.termsTextView.movementMethod = LinkMovementMethod.getInstance()
        binding.termsTextView.highlightColor = Color.TRANSPARENT
    }

    private fun applyLinkSpan(
        spannable: SpannableString,
        fullText: String,
        targetText: String,
        color: Int
    ) {
        val start = fullText.indexOf(targetText)
        if (start == -1) return
        val end = start + targetText.length

        spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                Snackbar.make(binding.root, targetText, Snackbar.LENGTH_SHORT).show()
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun bindActions() {
        binding.signInButton.setOnClickListener {
            Log.d(TAG, "signInButton: clicked, launching Google sign-in")
            launchGoogleSignIn()
        }
    }

    private fun launchGoogleSignIn() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()

        val client = GoogleSignIn.getClient(requireContext(), options)
        client.signOut().addOnCompleteListener {
            Log.d(TAG, "launchGoogleSignIn: client signed out, launching intent")
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            featuredPostsAdapter.submitList(posts)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.v(TAG, "observeViewModel: isLoading=$isLoading")
            binding.signInButton.isEnabled = !isLoading
            binding.signInProgressIndicator.isVisible = isLoading
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message == null) return@observe
            Log.d(TAG, "observeViewModel: showing snackbar message=$message")
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }

        viewModel.navigationUser.observe(viewLifecycleOwner) { user ->
            if (user == null) return@observe
            Log.d(TAG, "observeViewModel: navigating to WelcomeFragment for user=${user.uid}")
            val action = LoginFragmentDirections.actionLoginFragmentToWelcomeFragment(
                uid = user.uid
            )
            findNavController().navigate(action)
            viewModel.onNavigationHandled()
        }
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}
