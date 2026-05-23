package com.example.tripli.features.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripli.R
import com.example.tripli.model.Comment
import com.example.tripli.data.repository.posts.HomePostRepository
import com.example.tripli.databinding.ItemCommentBinding
import com.example.tripli.utils.CircleTransform
import com.squareup.picasso.Picasso

class CommentAdapter : ListAdapter<Comment, CommentAdapter.ViewHolder>(DiffCallback()) {

    private val circleTransform = CircleTransform()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Comment) {
            if (!comment.authorPhotoUrl.isNullOrBlank()) {
                binding.commentAvatarImageView.isVisible = true
                binding.commentInitialTextView.isVisible = false
                Picasso.get()
                    .load(comment.authorPhotoUrl)
                    .fit().centerCrop()
                    .transform(circleTransform)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(binding.commentAvatarImageView)
            } else {
                binding.commentAvatarImageView.isVisible = false
                binding.commentInitialTextView.isVisible = true
                val initial = comment.userName.firstOrNull()?.uppercase() ?: "?"
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(HomePostRepository.colorForId(comment.authorId)))
                }
                binding.commentInitialTextView.background = bg
                binding.commentInitialTextView.text = initial
            }
            binding.commentUserNameTextView.text = comment.userName
            binding.commentTimeAgoTextView.text = comment.timeAgo
            binding.commentTextView.text = comment.text
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment) = oldItem == newItem
    }
}
