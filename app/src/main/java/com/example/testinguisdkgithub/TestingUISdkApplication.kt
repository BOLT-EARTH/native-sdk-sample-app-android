package com.example.testinguisdkgithub

import android.app.Application
import com.boltearthsdk.BoltEarthUiSdk
import dagger.hilt.android.HiltAndroidApp

/**
 * Host Application for the TestingUISdk sample.
 *
 * Must be annotated with [HiltAndroidApp] because bolt-earth-ui-sdk exposes
 * `@AndroidEntryPoint` fragments internally; without a Hilt application the
 * SDK's component graph would not aggregate at build time.
 *
 * SDK credentials are seeded here with sensible defaults; the gate activity
 * ([SdkUserGateActivity]) re-invokes [BoltEarthUiSdk.initialize] with any
 * colour or locale changes the user picks before entering a flow.
 */
@HiltAndroidApp
class TestingUISdkApplication : Application()