package com.example.tripli.features.profile

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.tripli.data.repository.posts.HomePostRepository
import com.example.tripli.databinding.FragmentPostDetailBinding
import com.example.tripli.model.HomePost
import com.example.tripli.utils.CircleTransform
import com.squareup.picasso.Picasso
import java.io.File

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val args: PostDetailFragmentArgs by navArgs()
    private val viewModel: PostDetailViewModel by viewModels()
    private val circleTransform = CircleTransform()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val post = HomePost(
            id = args.postId,
            authorId = args.authorId,
            authorName = args.authorName,
            authorPhotoUrl = args.authorPhotoUrl,
            timeAgo = args.timeAgo,
            imageUrl = args.imageUrl,
            location = args.location,
            rating = args.rating,
            title = args.title,
            caption = args.caption,
            hashtags = args.hashtags?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
            likeCount = args.likeCount,
            commentCount = args.commentCount
        )
        viewModel.init(post)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        viewModel.post.observe(viewLifecycleOwner, ::bindPost)
    }

    private fun bindPost(post: HomePost) {
        bindImage(post)
        bindAvatar(post)
        binding.titleTextView.text = post.title
        binding.authorNameTextView.text = post.authorName
        binding.timeAgoTextView.text = post.timeAgo
        binding.locationTextView.text = post.location
        binding.ratingTextView.text = String.format("%.1f", post.rating)
        binding.captionTextView.text = post.caption
        binding.hashtagsTextView.text = post.hashtags.joinToString(" ") { "#$it" }
        binding.likeCountTextView.text = formatCount(post.likeCount)
        binding.commentCountTextView.text = formatCount(post.commentCount)
    }

    private fun bindImage(post: HomePost) {
        val localFile = post.localImagePath?.let { File(it) }?.takeIf { it.exists() }
        val request = if (localFile != null)
            Picasso.get().load(localFile)
        else
            Picasso.get().load(post.imageUrl.ifBlank { null })
        request.fit().centerCrop().into(binding.postImageView)
    }

    private fun bindAvatar(post: HomePost) {
        if (!post.authorPhotoUrl.isNullOrBlank()) {
            binding.userAvatarImageView.isVisible = true
            binding.userInitialTextView.isVisible = false
            Picasso.get()
                .load(post.authorPhotoUrl)
                .fit().centerCrop()
                .transform(circleTransform)
                .into(binding.userAvatarImageView)
        } else {
            binding.userAvatarImageView.isVisible = false
            binding.userInitialTextView.isVisible = true
            val initial = post.authorName.firstOrNull()?.uppercase() ?: "?"
            val color = Color.parseColor(HomePostRepository.colorForId(post.authorId))
            binding.userInitialTextView.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            binding.userInitialTextView.text = initial
        }
    }

    private fun formatCount(count: Int): String =
        if (count >= 1000) String.format("%.1fk", count / 1000.0) else count.toString()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
