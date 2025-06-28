package com.example.quickpdf.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.quickpdf.data.database.RecentFile
import com.example.quickpdf.databinding.ItemRecentFileBinding
import com.example.quickpdf.utils.FileUtil

class RecentFilesAdapter(
    private val onFileClick: (RecentFile) -> Unit,
    private val onMoreClick: (RecentFile) -> Unit
) : ListAdapter<RecentFile, RecentFilesAdapter.RecentFileViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentFileViewHolder {
        val binding = ItemRecentFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentFileViewHolder, position: Int) {
        val recentFile = getItem(position)
        holder.bind(recentFile)
    }

    inner class RecentFileViewHolder(
        private val binding: ItemRecentFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recentFile: RecentFile) {
            binding.apply {
                textViewFileName.text = recentFile.fileName
                textViewFileSize.text = FileUtil.formatFileSize(recentFile.fileSize)
                textViewPageCount.text = if (recentFile.pageCount > 0) {
                    "${recentFile.pageCount} pages"
                } else {
                    "Unknown pages"
                }
                textViewLastAccessed.text = "Opened ${FileUtil.formatDate(recentFile.lastAccessed)}"

                root.setOnClickListener {
                    onFileClick(recentFile)
                }

                buttonMore.setOnClickListener {
                    onMoreClick(recentFile)
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RecentFile>() {
        override fun areItemsTheSame(oldItem: RecentFile, newItem: RecentFile): Boolean {
            return oldItem.filePath == newItem.filePath
        }

        override fun areContentsTheSame(oldItem: RecentFile, newItem: RecentFile): Boolean {
            return oldItem == newItem
        }
    }
}