package com.example.testcamera

import android.Manifest
import android.R
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.testcamera.databinding.FragmentVideoBinding

class VideoFragment : Fragment() {

    private lateinit var binding: FragmentVideoBinding
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var recorder: Recorder? = null
    private var isRecording = false
    private var mediaStoreOutputOptions: MediaStoreOutputOptions? = null

    private val scaleGestureDetector by lazy {  // масштабирование пальцами
        ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val oldZoom = camera?.cameraInfo?.zoomState?.value ?: return false
                val newZoom = oldZoom.zoomRatio * detector.scaleFactor
                camera?.cameraControl?.setZoomRatio(newZoom.coerceIn(oldZoom.minZoomRatio, oldZoom.maxZoomRatio))
                return true
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentVideoBinding.inflate(layoutInflater)

        binding.captureVideo.setOnClickListener { captureVideo() }
        binding.switchCam.setOnClickListener {
            if (isRecording) {
                val currentRecording = recording
                if (currentRecording != null) {
                    currentRecording.pause()
                }

                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                startCamera()

                if (currentRecording != null) {
                    recording?.resume()
                }
            } else {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                startCamera()
            }
        }
        binding.modeVideo.setOnClickListener {
            cameraExecutor.shutdown()
            findNavController().navigate(VideoFragmentDirections.actionVideoFragmentToMainFragment())
        }
        binding.toGallery.setOnClickListener {
            cameraExecutor.shutdown()
            findNavController().navigate(VideoFragmentDirections.actionVideoFragmentToGalleryFragment())
        }
        return binding.root
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()

                if (recorder == null) {
                    recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build()
                }

                videoCapture = VideoCapture.withOutput(recorder!!)
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
                setupGestures()

                if (isRecording && recording == null && mediaStoreOutputOptions != null) {
                    startNewRecording()
                } else if (isRecording) {
                    recording?.resume()
                }
            } catch (exc: Exception) {
                Log.e("CameraX", "Не удалось запустить камеру: ", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureVideo() {
        if (isRecording) {
            recording?.stop()
            recording = null
            isRecording = false
            mediaStoreOutputOptions = null

            binding.captureVideo.setBackgroundResource(R.drawable.btn_default)
            binding.timer.visibility = View.GONE
            binding.toGallery.visibility = View.VISIBLE
            binding.modeVideo.visibility = View.VISIBLE
        } else {
            startNewRecording()
        }
    }

    private fun startNewRecording() {
        val videoCapture = this.videoCapture ?: return

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "TestCamera" + SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(System.currentTimeMillis()))
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TestCamera")
            }
        }

        mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(requireActivity().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        val pendingRecording = videoCapture.output
            .prepareRecording(requireContext(), mediaStoreOutputOptions!!)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val method = this.javaClass.getMethod("asPersistentRecording") // для бесшовного переключения
                    method.invoke(this)
                }
            }

        if (PermissionChecker.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED) {
            pendingRecording.withAudioEnabled()
        }

        recording = pendingRecording.start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
            when(recordEvent) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                    binding.captureVideo.setBackgroundResource(R.drawable.btn_default_small)
                    binding.captureVideo.isEnabled = true
                    binding.timer.visibility = View.VISIBLE
                    binding.toGallery.visibility = View.INVISIBLE
                    binding.modeVideo.visibility = View.INVISIBLE
                }
                is VideoRecordEvent.Status -> {
                    val stats = recordEvent.recordingStats
                    val time = TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
                    binding.timer.text = String.format("%02d:%02d", time / 60, time % 60)
                }
                is VideoRecordEvent.Finalize -> {
                    if (recordEvent.hasError()) {
                        recording?.close()
                        recording = null
                        Log.e("CameraX", "Не удалось записать видео: ${recordEvent.error}")
                    }

                    if (!isRecording) {
                        binding.captureVideo.setBackgroundResource(R.drawable.btn_default)
                        binding.timer.visibility = View.GONE
                        binding.captureVideo.isEnabled = true
                        binding.toGallery.visibility = View.VISIBLE
                        binding.modeVideo.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupGestures() {  // фокус по касанию
        val viewFinder = binding.previewView
        viewFinder.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                val factory = viewFinder.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
                camera?.cameraControl?.startFocusAndMetering(action)
                view.performClick()
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        startCamera()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            recording?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            recording?.stop()
        }
        cameraExecutor.shutdown()
    }
}