package com.example.tripli.features.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripli.model.HomePost
import com.example.tripli.databinding.ItemProfilePostBinding
import com.squareup.picasso.Picasso
import java.io.File

class ProfilePostAdapter(
    private val onPostClick: (HomePost) -> Unit,
    private val onMoreClick: ((HomePost) -> Unit)? = null
) : ListAdapter<HomePost, ProfilePostAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemProfilePostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: HomePost) {
            val localFile = post.localImagePath?.let { File(it) }?.takeIf { it.exists() }
            val request = if (localFile != null)
                Picasso.get().load(localFile)
            else
                Picasso.get().load(post.imageUrl.ifBlank { null })
            request.fit().centerCrop().into(binding.postThumbnail)
            binding.root.setOnClickListener { onPostClick(post) }

            binding.moreButton.isVisible = onMoreClick != null
            binding.moreButton.setOnClickListener { onMoreClick?.invoke(post) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProfilePostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HomePost>() {
            override fun areItemsTheSame(a: HomePost, b: HomePost) = a.id == b.id
            override fun areContentsTheSame(a: HomePost, b: HomePost) = a == b
        }
    }
}
