/*
 * Copyright 2023 Michael Johnston
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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object SystemCommands {
    private val logger: Logger = LoggerFactory.getLogger(SystemCommands::class.java)

    fun execute(vararg arg: String): Int {
        val process = ProcessBuilder(*arg)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(1, TimeUnit.HOURS)
        val exitValue = process.exitValue()
        if (exitValue != 0) {
            logger.error(process.inputStream.bufferedReader().readText().trim())
        }
        return process.exitValue()
    }
}