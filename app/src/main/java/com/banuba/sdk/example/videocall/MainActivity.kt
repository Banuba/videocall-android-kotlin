package com.banuba.sdk.example.videocall

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.banuba.sdk.entity.RecordedVideoInfo
import com.banuba.sdk.manager.BanubaSdkManager
import com.banuba.sdk.manager.BanubaSdkTouchListener
import com.banuba.sdk.manager.IEventCallback
import com.banuba.sdk.types.Data
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.AgoraVideoFrame
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MASK_NAME = "TrollGrandma"

        private const val REQUEST_CODE_PERMISSIONS = 1001

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private val banubaSdkManager by lazy(LazyThreadSafetyMode.NONE) {
        BanubaSdkManager(applicationContext)
    }
    private val banubaSdkEventCallback = object : IEventCallback {
        override fun onFrameRendered(data: Data, width: Int, height: Int) {
            pushCustomFrame(data, width, height)
        }

        override fun onImageProcessed(p0: Bitmap) {}

        override fun onEditingModeFaceFound(p0: Boolean) {}

        override fun onCameraOpenError(p0: Throwable) {}

        override fun onVideoRecordingFinished(p0: RecordedVideoInfo) {}

        override fun onVideoRecordingStatusChange(p0: Boolean) {}

        override fun onHQPhotoReady(p0: Bitmap) {}

        override fun onEditedImageReady(p0: Bitmap) {}

        override fun onScreenshotReady(p0: Bitmap) {}

        override fun onCameraStatus(p0: Boolean) {}
    }
    private val maskUri by lazy(LazyThreadSafetyMode.NONE) {
        Uri.parse(BanubaSdkManager.getResourcesBase())
            .buildUpon()
            .appendPath("effects")
            .appendPath(MASK_NAME)
            .build()
    }

    private val agoraRtc: RtcEngine by lazy(LazyThreadSafetyMode.NONE) {
        RtcEngine.create(
            this,
            AGORA_APP_ID,
            agoraEventHandler
        )
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
        setContentView(R.layout.activity_main)
        configureSdkManager()
        configureRtcEngine()
        setupLocalVideo()
        joinRtcChannel()
    }

    override fun onStart() {
        super.onStart()
        banubaSdkManager.attachSurface(localSurfaceView)
        if (checkAllPermissionsGranted()) {
            banubaSdkManager.openCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        banubaSdkManager.effectPlayer.effectManager()?.setEffectVolume(0F)
        banubaSdkManager.effectPlayerPlay()
        banubaSdkManager.startForwardingFrames()
    }

    override fun onPause() {
        super.onPause()
        banubaSdkManager.effectPlayerPause()
        banubaSdkManager.stopForwardingFrames()
    }

    override fun onStop() {
        super.onStop()
        banubaSdkManager.releaseSurface()
        banubaSdkManager.closeCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraRtc.leaveChannel()
        RtcEngine.destroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        if (checkAllPermissionsGranted()) {
            banubaSdkManager.openCamera()
        } else {
            Toast.makeText(applicationContext, "Please grant permission.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun configureSdkManager() {
        val banubaTouchListener = BanubaSdkTouchListener(this, banubaSdkManager.effectPlayer)
        localSurfaceView.setOnTouchListener(banubaTouchListener)
        banubaSdkManager.effectManager.loadAsync(maskUri.toString())
        banubaSdkManager.setCallback(banubaSdkEventCallback)
    }

    private fun configureRtcEngine() {
        agoraRtc.setExternalVideoSource(true, false, Constants.ExternalVideoSourceType.VIDEO_FRAME)
        val videoEncoderConfiguration = VideoEncoderConfiguration(
            VideoEncoderConfiguration.VD_1280x720,
            VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
            VideoEncoderConfiguration.STANDARD_BITRATE,
            VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
        )
        agoraRtc.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agoraRtc.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        agoraRtc.setVideoEncoderConfiguration(videoEncoderConfiguration)
        agoraRtc.enableVideo()
        agoraRtc.enableAudio()
    }

    private fun setupLocalVideo() {
        localSurfaceView.setZOrderMediaOverlay(true)
    }

    private fun setupRemoteVideo(uid: Int): SurfaceView {
        val surfaceView = SurfaceView(this)
        val videoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        agoraRtc.setupRemoteVideo(videoCanvas)
        return surfaceView
    }

    private fun joinRtcChannel() {
        agoraRtc.setDefaultAudioRoutetoSpeakerphone(true)
        agoraRtc.joinChannel(AGORA_CLIENT_TOKEN, AGORA_CHANNEL_ID, null, 0)
    }

    private fun pushCustomFrame(rawData: Data, width: Int, height: Int) {
        val pixelData = ByteArray(rawData.data.remaining())
        rawData.data.get(pixelData)
        rawData.close()
        val videoFrame = AgoraVideoFrame().apply {
            timeStamp = System.currentTimeMillis()
            format = AgoraVideoFrame.FORMAT_RGBA
            this.height = height
            stride = width
            buf = pixelData
        }
        agoraRtc.pushExternalVideoFrame(videoFrame)
    }

    private fun checkAllPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}