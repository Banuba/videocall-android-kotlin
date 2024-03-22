package com.banuba.sdk.example.videocall

import com.banuba.sdk.manager.BanubaSdkManager

/**
 * Client token for Banuba SDK. Consider obfuscation in release app.
 */
const val BANUBA_CLIENT_TOKEN: String = <#Place your token here#>

/**
 * App id for Agora SDK. Consider obfuscation in release app.
 */
const val AGORA_APP_ID = <#Place your agora app ID here#>
const val AGORA_CLIENT_TOKEN = <#Place your agora token here#>
const val AGORA_CHANNEL_ID = <#Place your agora channel ID here#>


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

        BanubaSdkManager.initialize(this, BANUBA_CLIENT_TOKEN)
    }
}
