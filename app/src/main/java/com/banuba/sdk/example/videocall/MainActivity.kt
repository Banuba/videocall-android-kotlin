package com.banuba.sdk.example.videocall

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.banuba.sdk.frame.FramePixelBuffer
import com.banuba.sdk.input.CameraDevice
import com.banuba.sdk.input.CameraDeviceConfigurator
import com.banuba.sdk.input.CameraInput
import com.banuba.sdk.manager.BanubaSdkManager
import com.banuba.sdk.output.FrameOutput
import com.banuba.sdk.output.IOutput
import com.banuba.sdk.output.SurfaceOutput
import com.banuba.sdk.player.Player
import com.banuba.sdk.player.PlayerTouchListener
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.AgoraVideoFrame
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import com.banuba.sdk.example.videocall.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private lateinit var binding: ActivityMainBinding

    // The player executes the main pipeline
    private lateinit var player: Player

    // This camera device will pass frames to the CameraInput
    private lateinit var cameraDevice: CameraDevice

    // The result will be displayed on the surface
    private lateinit var surfaceOutput: SurfaceOutput

    // The result also will be passed to agora as an array of pixels
    private lateinit var frameOutput: FrameOutput

    // Agora RTC engine makes a videocall
    private lateinit var agoraRtc: RtcEngine

    private var lensSelector = CameraDeviceConfigurator.DEFAULT_LENS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBanubaSdk()
        initRtcEngine()
        initViews()
    }

    override fun onStart() {
        super.onStart()
        if (checkAllPermissionsGranted()) {
            start()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        player.play()
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onStop() {
        cameraDevice.stop()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraRtc.leaveChannel()
        RtcEngine.destroy()
        cameraDevice.close()
        player.close()
        surfaceOutput.close()
        frameOutput.close()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        if (checkAllPermissionsGranted()) {
            start()
        } else {
            Toast.makeText(applicationContext, "Please grant all required permissions to proceed.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun start() {
        configureBanubaSdk()
        configureRtcEngine()
        joinVideoCall()
        cameraDevice.start()
    }

    private fun initViews() {
        val effectsListAdapter = CustomEffectsListAdapter(
            Resources.getSystem().displayMetrics.widthPixels) { effectPath, position ->
            binding.effectsList.smoothScrollToPosition(position)
            applyEffect(effectPath)
        }

        effectsListAdapter.submitList(BanubaSdkManager.loadEffects())
        binding.effectsList.adapter = effectsListAdapter
        binding.effectsList.layoutManager = CustomEffectsListAdapter.CenterLayoutManager(this)

        binding.switchCameraButton.setOnClickListener {
            switchCamera()
        }

        binding.muteEffectAudioButton.setOnClickListener {
            muteEffectAudio()
        }
    }

    private fun initBanubaSdk() {
        player = Player()
        cameraDevice = CameraDevice(requireNotNull(this.applicationContext), this@MainActivity)
        surfaceOutput = SurfaceOutput(binding.localSurfaceView.holder)
        frameOutput = FrameOutput(object : FrameOutput.IFramePixelBufferProvider {
            override fun onFrame(output: IOutput, pb: FramePixelBuffer?) = pushCustomFrame(pb!!)
        })
    }

    private fun configureBanubaSdk() {
        // Set layer will take input frames from and where the player will display the result
        player.use(CameraInput(cameraDevice), arrayOf(surfaceOutput, frameOutput))

        binding.localSurfaceView.setOnTouchListener(PlayerTouchListener(this, player))
        binding.localSurfaceView.setZOrderMediaOverlay(true)
    }

    private fun switchCamera() {
        lensSelector = if (lensSelector == CameraDeviceConfigurator.LensSelector.BACK) {
            CameraDeviceConfigurator.LensSelector.FRONT
        } else {
            CameraDeviceConfigurator.LensSelector.BACK
        }
        cameraDevice.configurator.setLens(lensSelector).commit()
    }

    private fun muteEffectAudio() {
        val audioVolume = if (!binding.audioOnImage.isVisible()) 1F else 0F
        player.setEffectVolume(audioVolume)
        updateMuteEffectAudioButtonUI()
    }

    private fun applyEffect(effectPath: String) {
        player.loadAsync(effectPath)
    }

    private fun updateMuteEffectAudioButtonUI() = with(binding) {
        val effectAudioEnabled = !audioOnImage.isVisible()
        audioBackgroundImage.visibility(!effectAudioEnabled)
        audioOnImage.visibility(effectAudioEnabled)
        audioOffImage.visibility(!effectAudioEnabled)
    }

    private fun initRtcEngine() {
        agoraRtc = RtcEngine.create(this, AGORA_APP_ID, object : IRtcEngineEventHandler() {
            override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
                runOnUiThread {
                    val surfaceView = setupRemoteVideo(uid)
                    binding.remoteVideoContainer.removeAllViews()
                    binding.remoteVideoContainer.addView(surfaceView)
                }
            }
        })
    }

    private fun configureRtcEngine() {
        agoraRtc.setExternalVideoSource(true, false, Constants.ExternalVideoSourceType.VIDEO_FRAME)
        agoraRtc.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agoraRtc.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        agoraRtc.setVideoEncoderConfiguration(VideoEncoderConfiguration(
            VideoEncoderConfiguration.VD_1280x720,
            VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
            VideoEncoderConfiguration.STANDARD_BITRATE,
            VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
        ))
        agoraRtc.enableVideo()
        agoraRtc.enableAudio()
    }

    private fun joinVideoCall() {
        agoraRtc.setDefaultAudioRoutetoSpeakerphone(true)
        agoraRtc.joinChannel(AGORA_CLIENT_TOKEN, AGORA_CHANNEL_ID, null, 0)
    }

    private fun setupRemoteVideo(uid: Int): SurfaceView {
        val surfaceView = SurfaceView(this)
        val videoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        agoraRtc.setupRemoteVideo(videoCanvas)
        return surfaceView
    }

    private fun pushCustomFrame(pb: FramePixelBuffer) {
        val pixelData = ByteArray(pb.buffer.remaining())
        pb.buffer.get(pixelData)
        val videoFrame = AgoraVideoFrame().apply {
            timeStamp = System.currentTimeMillis()
            format = AgoraVideoFrame.FORMAT_RGBA
            height = pb.height
            stride = pb.bytesPerRow / pb.bytesPerPixel
            buf = pixelData
        }
        agoraRtc.pushExternalVideoFrame(videoFrame)
    }

    private fun checkAllPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}
