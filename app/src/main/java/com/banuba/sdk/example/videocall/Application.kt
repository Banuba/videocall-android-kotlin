package com.banuba.sdk.example.videocall

import com.banuba.sdk.manager.BanubaSdkManager

/**
 * Key for Banuba SDK
 */
const val BANUBA_TOKEN: String = SET KEY

/**
 * Keys for Agora SDK
 */
const val AGORA_APP_ID = SET KEY
const val AGORA_CLIENT_TOKEN = SET KEY
const val AGORA_CHANNEL_ID = SET KEY


class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()

        // It crashes if token is empty string with
        //
        // RuntimeException:
        //  Unable to create application com.banuba.sdk.samples.SamplesApp:
        //  java.lang.RuntimeException: Can't parse client token.
        //
        //  Please, contact Banuba for obtain a correct client token.

        BanubaSdkManager.initialize(this, BANUBA_TOKEN)
    }
}
