package com.example.testcamera.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.testcamera.databinding.ItemGalleryBinding
import java.io.File

class MediaAdapter(
    private val files: List<File>,
    private val onFileClick: (File) -> Unit
) : RecyclerView.Adapter<MediaAdapter.GalleryViewHolder>() {

    inner class GalleryViewHolder(val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val file = files[position]

        Glide.with(holder.binding.root).load(file).into(holder.binding.thumb) // для превью медиафайлов

        if (file.extension == "mp4") {
            holder.binding.videoIcon.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            onFileClick(file)
        }
    }

    override fun getItemCount(): Int = files.size
}