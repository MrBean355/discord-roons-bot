/*
 * Copyright 2022 Michael Johnston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mrbean355.roons.component

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import java.io.File

internal class SoundBiteConverterTest {
    @MockK(relaxed = true)
    private lateinit var logger: Logger

    private var initialOsName = ""

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)

        mockkObject(SystemCommands)
        every { SystemCommands.execute(*anyVararg()) } returns 0

        initialOsName = System.getProperty("os.name")
        System.setProperty("os.name", "unix")
    }

    @AfterEach
    internal fun tearDown() {
        System.setProperty("os.name", initialOsName)
        File(System.getProperty("java.io.tmpdir"), "roons").deleteRecursively()
    }

    @Test
    internal fun testConstruction_WindowsOs_DoesNotInstallExecutable() {
        System.setProperty("os.name", "Windows 10")
        SoundBiteConverter(logger)

        val executable = File(System.getProperty("java.io.tmpdir"), "roons")

        assertFalse(executable.exists())
    }

    @Test
    internal fun testConstruction_NonWindowsOs_InstallsExecutable() {
        SoundBiteConverter(logger)

        val executable = File(System.getProperty("java.io.tmpdir"), "roons/ffmpeg")

        assertTrue(executable.exists())
    }
}