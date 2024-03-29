package com.banuba.sdk.example.videocall

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.banuba.sdk.example.videocall.databinding.ActivityMainBinding
import com.banuba.sdk.frame.FramePixelBuffer
import com.banuba.sdk.input.CameraDevice
import com.banuba.sdk.input.CameraDeviceConfigurator
import com.banuba.sdk.input.CameraInput
import com.banuba.sdk.manager.BanubaSdkManager
import com.banuba.sdk.output.FrameOutput
import com.banuba.sdk.output.SurfaceOutput
import com.banuba.sdk.player.Player
import com.banuba.sdk.player.PlayerTouchListener
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.AgoraVideoFrame
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    companion object {
        private const val TAG = "MainActivity"

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private lateinit var binding: ActivityMainBinding
    private var effectsAdapter: CustomEffectsListAdapter? = null

    private var banubaPlayer: Player? = null
    private var lensSelector = CameraDeviceConfigurator.LensSelector.FRONT
    private var cameraDevice: CameraDevice? = null

    private var surfaceOutput: SurfaceOutput? = null
    private var frameOutput: FrameOutput? = null

    private var agoraRtc: RtcEngine? = null

    private var muteAudio = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        initBanuba()
        initAgora()
    }

    override fun onStart() {
        super.onStart()
        if (checkAllPermissionsGranted()) {
            onPermissionsGranted()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        banubaPlayer?.play()
    }

    override fun onPause() {
        super.onPause()
        banubaPlayer?.pause()
    }

    override fun onStop() {
        cameraDevice?.stop()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraRtc?.leaveChannel()
        RtcEngine.destroy()
        cameraDevice?.close()
        banubaPlayer?.close()
        surfaceOutput?.close()
        frameOutput?.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        if (checkAllPermissionsGranted()) {
            onPermissionsGranted()
        } else {
            Toast.makeText(
                applicationContext,
                "Please grant all required permissions to proceed.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun onPermissionsGranted() {
        prepareBanuba()
        prepareAgora()

        cameraDevice?.start()

        joinVideoCall()
    }

    private fun initBanuba() {
        banubaPlayer = Player()
        surfaceOutput = SurfaceOutput(binding.localSurfaceView.holder)
        frameOutput = FrameOutput { _, pb ->
            pb?.let { processFrame(it) }

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun prepareBanuba() {
        cameraDevice = CameraDevice(applicationContext, this)

        if (banubaPlayer == null) {
            Log.w(TAG, "Cannot prepare Banuba SDK: Banuba SDK is not initialized!")
            return
        }

        // Set layer will take input frames from and where the player will display the result
        banubaPlayer?.use(
            CameraInput(requireNotNull(cameraDevice)),
            arrayOf(surfaceOutput, frameOutput)
        )

        binding.localSurfaceView.setOnTouchListener(
            PlayerTouchListener(
                this,
                requireNotNull(banubaPlayer)
            )
        )
        binding.localSurfaceView.setZOrderMediaOverlay(true)

        val effects = BanubaSdkManager.loadEffects()
        effectsAdapter?.submitList(effects)

        // Apply first effect
        applyEffect(effects[0].path)
    }

    private fun toggleCameraFacing() {
        lensSelector = if (lensSelector == CameraDeviceConfigurator.LensSelector.BACK) {
            CameraDeviceConfigurator.LensSelector.FRONT
        } else {
            CameraDeviceConfigurator.LensSelector.BACK
        }
        cameraDevice?.configurator?.setLens(lensSelector)?.commit()
    }


    private fun applyAudioVolume() {
        val audioVolume = if (muteAudio) 0F else 1F
        banubaPlayer?.setEffectVolume(audioVolume)
    }

    private fun applyEffect(effectPath: String) {
        banubaPlayer?.loadAsync(effectPath)
    }

    private fun initAgora() {
        agoraRtc = RtcEngine.create(this, AGORA_APP_ID, object : IRtcEngineEventHandler() {
            override fun onFirstRemoteVideoDecoded(
                uid: Int,
                width: Int,
                height: Int,
                elapsed: Int
            ) {
                runOnUiThread {
                    val surfaceView = setupRemoteVideo(uid)
                    binding.remoteVideoContainer.removeAllViews()
                    binding.remoteVideoContainer.addView(surfaceView)
                }
            }
        })
    }

    private fun prepareAgora() {
        val rtc = agoraRtc ?: return

        rtc.setExternalVideoSource(true, false, Constants.ExternalVideoSourceType.VIDEO_FRAME)
        rtc.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        rtc.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        rtc.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_1280x720,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
        rtc.enableVideo()
        rtc.enableAudio()
    }

    private fun joinVideoCall() {
        Log.d(TAG, "Join video call")
        agoraRtc?.setDefaultAudioRoutetoSpeakerphone(true)
        agoraRtc?.joinChannel(AGORA_CLIENT_TOKEN, AGORA_CHANNEL_ID, null, 0)
    }

    private fun setupRemoteVideo(uid: Int): SurfaceView =
        SurfaceView(this).apply {
            val videoCanvas = VideoCanvas(this, VideoCanvas.RENDER_MODE_HIDDEN, uid)
            agoraRtc?.setupRemoteVideo(videoCanvas)
        }

    private fun processFrame(pb: FramePixelBuffer) {
        val pixelData = ByteArray(pb.buffer.remaining())
        pb.buffer.get(pixelData)
        val videoFrame = AgoraVideoFrame().apply {
            timeStamp = System.currentTimeMillis()
            format = AgoraVideoFrame.FORMAT_RGBA
            height = pb.height
            stride = pb.bytesPerRow / pb.bytesPerPixel
            buf = pixelData
        }
        agoraRtc?.pushExternalVideoFrame(videoFrame)
    }

    private fun checkAllPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun initViews() {
        invalidateViewState()

         effectsAdapter = CustomEffectsListAdapter(
            resources.displayMetrics.widthPixels
        ) { effectPath, position ->
            applyEffect(effectPath)

            binding.effectsList.smoothScrollToPosition(position)
        }

        binding.effectsList.layoutManager = CustomEffectsListAdapter.CenterLayoutManager(this)
        binding.effectsList.adapter = effectsAdapter

        binding.switchCameraImage.setOnClickListener {
            toggleCameraFacing()
            invalidateViewState()
        }

        binding.muteAudioView.setOnClickListener {
            muteAudio = !muteAudio
            applyAudioVolume()
            invalidateViewState()
        }
    }

    private fun invalidateViewState() {
        with(binding) {
            muteAudioView.setImageResource(
                if (muteAudio) {
                    R.drawable.ic_audio_off
                } else {
                    R.drawable.ic_audio_on
                }
            )
        }
    }
}
