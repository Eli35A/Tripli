package com.example.tripli.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripli.R
import com.example.tripli.data.model.HomePost
import com.example.tripli.databinding.ItemHomePostBinding

class HomePostAdapter(
    private val onLikeClick: (HomePost) -> Unit,
    private val onSaveClick: (HomePost) -> Unit,
    private val onCommentClick: (HomePost) -> Unit
) : ListAdapter<HomePost, HomePostAdapter.ViewHolder>(DiffCallback()) {

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
            // Avatar circle with initial
            val avatarBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(post.userAccentHex))
            }
            binding.userInitialTextView.background = avatarBg
            binding.userInitialTextView.text = post.userInitial

            // Header
            binding.userNameTextView.text = post.userName
            binding.timeAgoTextView.text = post.timeAgo

            // Image
            binding.postImageView.setImageResource(post.imageResId)

            // Rating & location
            binding.ratingTextView.text = String.format("%.1f", post.rating)
            binding.locationTextView.text = post.location

            // Content
            binding.titleTextView.text = post.title
            binding.captionTextView.text = post.caption
            binding.hashtagsTextView.text = post.hashtags.joinToString(" ")

            // Counts
            binding.likeCountTextView.text = formatCount(post.likeCount)
            binding.commentCountTextView.text = formatCount(post.commentCount)

            // Toggle states
            updateLikeButton(post.isLiked)
            updateBookmarkButton(post.isSaved)

            // Click listeners
            binding.likeButton.setOnClickListener { onLikeClick(post) }
            binding.bookmarkButton.setOnClickListener { onSaveClick(post) }
            binding.commentButton.setOnClickListener { onCommentClick(post) }
        }

        private fun updateLikeButton(isLiked: Boolean) {
            val context = binding.root.context
            if (isLiked) {
                binding.likeButton.setImageResource(R.drawable.ic_heart_filled)
                binding.likeButton.setColorFilter(
                    ContextCompat.getColor(context, R.color.heartRed)
                )
            } else {
                binding.likeButton.setImageResource(R.drawable.ic_heart)
                binding.likeButton.setColorFilter(
                    ContextCompat.getColor(context, R.color.textSecondary)
                )
            }
        }

        private fun updateBookmarkButton(isSaved: Boolean) {
            val context = binding.root.context
            if (isSaved) {
                binding.bookmarkButton.setImageResource(R.drawable.ic_bookmark_filled)
                binding.bookmarkButton.setColorFilter(
                    ContextCompat.getColor(context, R.color.accentBlue)
                )
            } else {
                binding.bookmarkButton.setImageResource(R.drawable.ic_bookmark_outline)
                binding.bookmarkButton.setColorFilter(
                    ContextCompat.getColor(context, R.color.textSecondary)
                )
            }
        }

        private fun formatCount(count: Int): String {
            return if (count >= 1000) {
                String.format("%.1fk", count / 1000.0)
            } else {
                count.toString()
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<HomePost>() {
        override fun areItemsTheSame(oldItem: HomePost, newItem: HomePost) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: HomePost, newItem: HomePost) =
            oldItem == newItem
    }
}
