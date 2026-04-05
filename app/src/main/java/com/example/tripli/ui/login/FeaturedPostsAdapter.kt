package com.example.tripli.ui.login

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripli.data.model.TravelPost
import com.example.tripli.databinding.ItemFeaturedPostBinding

class FeaturedPostsAdapter :
    ListAdapter<TravelPost, FeaturedPostsAdapter.FeaturedPostViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedPostViewHolder {
        val binding = ItemFeaturedPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeaturedPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeaturedPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FeaturedPostViewHolder(
        private val binding: ItemFeaturedPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: TravelPost) {
            binding.postImageView.setImageResource(post.imageResId)
            binding.locationTextView.text = post.location
            binding.quoteTextView.text = "\"${post.quote}\""
            binding.authorNameTextView.text = post.authorName
            binding.authorInitialTextView.text = post.authorInitial
            binding.authorInitialTextView.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(post.authorAccentHex))
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TravelPost>() {
        override fun areItemsTheSame(oldItem: TravelPost, newItem: TravelPost): Boolean {
            return oldItem.location == newItem.location && oldItem.authorName == newItem.authorName
        }

        override fun areContentsTheSame(oldItem: TravelPost, newItem: TravelPost): Boolean {
            return oldItem == newItem
        }
    }
}
