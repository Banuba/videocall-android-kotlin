Quick start examples for [Banuba SDK on Android](https://docs.banuba.com/docs/android/android_overview) and [Agora.io](https://www.agora.io/en/) SDK integration to enhance video calls with real-time face filters and virtual backgrounds.

# Getting Started

1. Get the latest Banuba SDK archive for Android and the client token. Please fill in our form on [form on banuba.com](https://www.banuba.com/face-filters-sdk) website, or contact us via [info@banuba.com](mailto:info@banuba.com).
2. Copy `aar` files from the Banuba SDK archive into `libs` dir:
    `BNBEffectPlayer/bin/banuba_sdk/banuba_sdk-release.aar` => `videocall-android-kotlin/libs/`
    `BNBEffectPlayer/banuba_effect_player-release.aar` => `videocall-android-kotlin/libs/`
3. Copy and Paste your banuba client token into appropriate section of `videocall-android-kotlin/app/src/main/java/com/banuba/sdk/example/videocall/BanubaClientToken.kt` with “” symbols. For example: banuba_token = “place_your_token_here”
4. Visit agora.io to sign up and get token, app and channel ID
5. Copy and Paste your agora token, app and chanel ID into appropriate section of `videocall-android-kotlin/app/src/main/java/com/banuba/sdk/example/videocall/AgoraClientToken.kt` with “” symbols. For example: agora_token = “place_your_token_here”
6. Open the project in Android Studio and run the necessary target using the usual steps.

# Contributing

Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
