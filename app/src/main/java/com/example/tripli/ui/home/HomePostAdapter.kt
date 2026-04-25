package com.example.tripli.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripli.R
import com.example.tripli.data.model.HomePost
import com.example.tripli.data.repository.HomePostRepository
import com.example.tripli.databinding.ItemHomePostBinding
import com.example.tripli.utils.CircleTransform
import com.squareup.picasso.Picasso

class HomePostAdapter(
    private val onLikeClick: (HomePost) -> Unit,
    private val onSaveClick: (HomePost) -> Unit,
    private val onCommentClick: (HomePost) -> Unit
) : ListAdapter<HomePost, HomePostAdapter.ViewHolder>(DiffCallback()) {

    private val circleTransform = CircleTransform()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomePostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHomePostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: HomePost) {
            bindAvatar(post)
            binding.userNameTextView.text = post.authorName
            binding.timeAgoTextView.text = post.timeAgo

            Picasso.get()
                .load(post.imageUrl.ifBlank { null })
                .fit()
                .centerCrop()
                .placeholder(R.color.screenBackground)
                .into(binding.postImageView)

            binding.ratingTextView.text = String.format("%.1f", post.rating)
            binding.locationTextView.text = post.location
            binding.titleTextView.text = post.title
            binding.captionTextView.text = post.caption
            binding.hashtagsTextView.text = post.hashtags.joinToString(" ")
            binding.likeCountTextView.text = formatCount(post.likeCount)
            binding.commentCountTextView.text = formatCount(post.commentCount)

            updateLikeButton(post.isLiked)
            updateBookmarkButton(post.isSaved)

            binding.likeButton.setOnClickListener { onLikeClick(post) }
            binding.bookmarkButton.setOnClickListener { onSaveClick(post) }
            binding.commentButton.setOnClickListener { onCommentClick(post) }
        }

        private fun bindAvatar(post: HomePost) {
            if (!post.authorPhotoUrl.isNullOrBlank()) {
                binding.userAvatarImageView.isVisible = true
                binding.userInitialTextView.isVisible = false
                Picasso.get()
                    .load(post.authorPhotoUrl)
                    .fit()
                    .centerCrop()
                    .transform(circleTransform)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(binding.userAvatarImageView)
            } else {
                binding.userAvatarImageView.isVisible = false
                binding.userInitialTextView.isVisible = true
                val initial = post.authorName.firstOrNull()?.uppercase() ?: "?"
                val color = Color.parseColor(HomePostRepository.colorForId(post.authorId))
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                binding.userInitialTextView.background = bg
                binding.userInitialTextView.text = initial
            }
        }

        private fun updateLikeButton(isLiked: Boolean) {
            val context = binding.root.context
            if (isLiked) {
                binding.likeButton.setImageResource(R.drawable.ic_heart_filled)
                binding.likeButton.setColorFilter(ContextCompat.getColor(context, R.color.heartRed))
            } else {
                binding.likeButton.setImageResource(R.drawable.ic_heart)
                binding.likeButton.setColorFilter(ContextCompat.getColor(context, R.color.textSecondary))
            }
        }

        private fun updateBookmarkButton(isSaved: Boolean) {
            val context = binding.root.context
            if (isSaved) {
                binding.bookmarkButton.setImageResource(R.drawable.ic_bookmark_filled)
                binding.bookmarkButton.setColorFilter(ContextCompat.getColor(context, R.color.accentBlue))
            } else {
                binding.bookmarkButton.setImageResource(R.drawable.ic_bookmark_outline)
                binding.bookmarkButton.setColorFilter(ContextCompat.getColor(context, R.color.textSecondary))
            }
        }

        private fun formatCount(count: Int): String =
            if (count >= COUNT_THRESHOLD) String.format("%.1fk", count / COUNT_THRESHOLD.toDouble()) else count.toString()
    }

    companion object {
        private const val COUNT_THRESHOLD = 1000
    }

    class DiffCallback : DiffUtil.ItemCallback<HomePost>() {
        override fun areItemsTheSame(oldItem: HomePost, newItem: HomePost) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HomePost, newItem: HomePost) = oldItem == newItem
    }
}
