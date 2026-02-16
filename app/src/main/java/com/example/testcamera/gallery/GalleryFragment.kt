package com.example.testcamera.gallery

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.testcamera.gallery.MediaAdapter
import com.example.testcamera.databinding.FragmentGalleryBinding
import java.io.File

class GalleryFragment : Fragment() {

    private lateinit var binding: FragmentGalleryBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentGalleryBinding.inflate(layoutInflater)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = MediaAdapter(loadMedia()) { file ->
            findNavController().navigate(
                GalleryFragmentDirections.actionGalleryFragmentToViewerFragment(
                    file.absolutePath
                )
            )
        }
        return binding.root
    }

    private fun loadMedia(): List<File> {
        val mediaList = mutableListOf<File>()
        if (File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "TestCamera"
            ).exists()) {
            mediaList.addAll(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "TestCamera"
                ).listFiles()?.filter { it.extension == "jpg" } ?: emptyList())
        }
        if (File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "TestCamera"
            ).exists()) {
            mediaList.addAll(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "TestCamera"
                ).listFiles()?.filter { it.extension == "mp4" } ?: emptyList())
        }
        return mediaList.sortedByDescending { it.lastModified() }
    }

    override fun onResume() {
        super.onResume()
        val files = loadMedia()
        binding.recyclerView.adapter = MediaAdapter(files) { file ->
            findNavController().navigate(
                GalleryFragmentDirections.actionGalleryFragmentToViewerFragment(
                    file.absolutePath
                )
            )
        }
    }
}