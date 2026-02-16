package com.example.testcamera.gallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.testcamera.databinding.FragmentMediaViewerBinding
import java.io.File

class MediaViewerFragment : Fragment() {
    private lateinit var binding: FragmentMediaViewerBinding
    private val args: MediaViewerFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentMediaViewerBinding.inflate(layoutInflater)

        if (args.path == null){
            findNavController().navigate(MediaViewerFragmentDirections.actionViewerFragmentToGalleryFragment())
        }

        val file = File(args.path.toString())

        if (file.extension != "mp4") {
            binding.videoView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            Glide.with(this).load(file).into(binding.imageView) // отображение превью
        } else {
            binding.imageView.visibility = View.GONE
            binding.videoView.visibility = View.VISIBLE
            binding.videoView.setVideoPath(args.path.toString())
            binding.videoView.setMediaController(MediaController(requireContext()))
            binding.videoView.start()
        }

        binding.btnDelete.setOnClickListener {
            if (file.exists()) {
                try{
                    file.delete()
                    findNavController().navigate(MediaViewerFragmentDirections.actionViewerFragmentToGalleryFragment())
                } catch (e: Exception){
                    Log.d("Media", "Ошибка при удалении файла: ", e)
                }
            }
        }
        return binding.root
    }
}