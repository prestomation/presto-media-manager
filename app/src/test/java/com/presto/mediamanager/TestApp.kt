package com.presto.mediamanager

import android.app.Application

/**
 * Minimal Application for Robolectric so tests don't run [PrestoApp.onCreate],
 * which schedules WorkManager jobs that aren't initialized in the JVM test env.
 * The UI tests exercise the stateless `*Content` composables, which need no DI.
 */
class TestApp : Application()
