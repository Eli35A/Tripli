package com.example.tripli.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripli.data.model.HomePost
import com.example.tripli.databinding.ItemProfilePostBinding
import com.squareup.picasso.Picasso
import java.io.File

class ProfilePostAdapter(
    private val onPostClick: (HomePost) -> Unit,
    private val onEditClick: ((HomePost) -> Unit)? = null,
    private val onDeleteClick: ((HomePost) -> Unit)? = null
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

            val showMore = onEditClick != null || onDeleteClick != null
            binding.moreButton.isVisible = showMore
            if (showMore) {
                binding.moreButton.setOnClickListener { v ->
                    PopupMenu(v.context, v).apply {
                        if (onEditClick != null) menu.add(0, MENU_EDIT, 0, "Edit")
                        if (onDeleteClick != null) menu.add(0, MENU_DELETE, 1, "Delete")
                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                MENU_EDIT -> { onEditClick?.invoke(post); true }
                                MENU_DELETE -> {
                                    AlertDialog.Builder(v.context)
                                        .setTitle("Delete Post")
                                        .setMessage("Are you sure you want to delete this post?")
                                        .setPositiveButton("Delete") { _, _ -> onDeleteClick?.invoke(post) }
                                        .setNegativeButton("Cancel", null)
                                        .show()
                                    true
                                }
                                else -> false
                            }
                        }
                        show()
                    }
                }
            }
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
        private const val MENU_EDIT = 1
        private const val MENU_DELETE = 2

        private val DIFF = object : DiffUtil.ItemCallback<HomePost>() {
            override fun areItemsTheSame(a: HomePost, b: HomePost) = a.id == b.id
            override fun areContentsTheSame(a: HomePost, b: HomePost) = a == b
        }
    }
}
