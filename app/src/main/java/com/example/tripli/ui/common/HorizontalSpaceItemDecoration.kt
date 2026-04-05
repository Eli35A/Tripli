package com.example.tripli.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalSpaceItemDecoration(
    private val horizontalSpace: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val lastPosition = (parent.adapter?.itemCount ?: 0) - 1

        outRect.right = if (position == lastPosition) 0 else horizontalSpace
    }
}
