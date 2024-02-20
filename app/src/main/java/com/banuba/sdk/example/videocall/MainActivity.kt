package com.banuba.sdk.example.videocall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.banuba.sdk.frame.FramePixelBuffer
import com.banuba.sdk.input.CameraDevice
import com.banuba.sdk.input.CameraInput
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
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    companion object {
        private const val MASK_NAME = "effects/TrollGrandma"
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    // The player executes the main pipeline
    private val player by lazy(LazyThreadSafetyMode.NONE) {
        Player()
    }

    // This camera device will pass frames to the CameraInput
    private val cameraDevice by lazy(LazyThreadSafetyMode.NONE) {
        CameraDevice(requireNotNull(this.applicationContext), this@MainActivity)
    }

    // The result will be displayed on the surface
    private val surfaceOutput by lazy(LazyThreadSafetyMode.NONE) {
        SurfaceOutput(localSurfaceView.holder)
    }

    // The result also will be passed to agora as an array of pixels
    private val frameOutput by lazy(LazyThreadSafetyMode.NONE) {
        FrameOutput(object : FrameOutput.IFramePixelBufferProvider {
            override fun onFrame(output: IOutput, pb: FramePixelBuffer?) {
                pushCustomFrame(pb!!)
            }
        })
    }

    // Agora RTC engine makes a videocall
    private val agoraRtc: RtcEngine by lazy(LazyThreadSafetyMode.NONE) {
        RtcEngine.create(this, AGORA_APP_ID, agoraEventHandler)
    }

    private val agoraEventHandler = object : IRtcEngineEventHandler() {
        override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            runOnUiThread {
                val surfaceView = setupRemoteVideo(uid)
                remoteVideoContainer.removeAllViews()
                remoteVideoContainer.addView(surfaceView)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureBanubaSdk()
    }

    private fun start() {
        configureRtcEngine()
        cameraDevice.start()
    }

    override fun onStart() {
        super.onStart()
        if (checkAllPermissionsGranted()) {
            start()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
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
            Toast.makeText(applicationContext, "Please grant permission.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun configureBanubaSdk() {
        localSurfaceView.setOnTouchListener(PlayerTouchListener(this, player))
        // Turn off audio effects
        player.setEffectVolume(0F)
        // Set layer will take input frames from and where the player will display the result
        player.use(CameraInput(cameraDevice), arrayOf(surfaceOutput, frameOutput))
        // Loading the effect
        player.loadAsync(MASK_NAME)

        localSurfaceView.setZOrderMediaOverlay(true)
    }

    private fun configureRtcEngine() {
        // Initialize the Agora RTC SDK
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

        // Join a video call
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
}
