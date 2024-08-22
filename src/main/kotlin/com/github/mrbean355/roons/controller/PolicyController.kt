/*
 * Copyright 2024 Michael Johnston
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

package com.github.mrbean355.roons.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PolicyController {

    @GetMapping("/terms")
    fun termsOfService(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Terms of Service for Roons Bot</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        margin: 20px;
                        padding: 0;
                        background-color: #f4f4f4;
                    }
                    h1, h2 {
                        color: #333;
                    }
                    .container {
                        max-width: 800px;
                        margin: auto;
                        background: white;
                        padding: 20px;
                        border-radius: 8px;
                        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                    }
                    p {
                        margin: 1em 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Terms of Service for Roons Bot</h1>
                    <p><strong>Effective Date:</strong> 22 August 2024</p>
                    
                    <h2>1. Introduction</h2>
                    <p>Welcome to Roons Bot! By using this bot on any Discord server, you agree to these Terms of Service. 
                    If you do not agree, please refrain from using the bot.</p>
                    
                    <h2>2. Bot Functionality</h2>
                    <p>Roons Bot allows users to play sound bites in voice channels. The bot is provided as-is, and the developer 
                    does not guarantee uninterrupted or error-free operation.</p>
                    
                    <h2>3. User Data Collection</h2>
                    <p>To provide its functionality, Roons Bot stores the following data:</p>
                    <ul>
                        <li>Guild ID (the server's unique identifier)</li>
                        <li>User ID (your unique Discord identifier)</li>
                        <li>Voice Channel ID (the identifier of the voice channel where the bot is used)</li>
                        <li>Volume setting (the user’s preferred volume level)</li>
                    </ul>
                    <p>This data is collected solely to ensure the bot functions correctly and will not be shared with third parties.</p>
                    
                    <h2>4. Data Retention</h2>
                    <p>The data mentioned above is stored for as long as necessary to provide the bot’s services. 
                    You can request the deletion of your data by contacting the bot's developer.</p>
                    
                    <h2>5. Disclaimer</h2>
                    <p>Roons Bot is provided "as is" without warranties of any kind. The bot's developer is not responsible for any damage, 
                    loss of data, or other issues that arise from using the bot.</p>
                    
                    <h2>6. Changes to the Terms</h2>
                    <p>These Terms of Service may be updated from time to time. You are responsible for regularly reviewing the terms, 
                    and your continued use of the bot signifies your acceptance of any changes.</p>
                    
                    <h2>7. Contact Information</h2>
                    <p>If you have any questions or concerns regarding these terms, please contact mrbean355@gmail.com.</p>
                </div>
            </body>
            </html>
            """.trimIndent()
    }

    @GetMapping("/privacy")
    fun privacyPolicy(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Privacy Policy for Roons Bot</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        margin: 20px;
                        padding: 0;
                        background-color: #f4f4f4;
                    }
                    h1, h2 {
                        color: #333;
                    }
                    .container {
                        max-width: 800px;
                        margin: auto;
                        background: white;
                        padding: 20px;
                        border-radius: 8px;
                        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                    }
                    p {
                        margin: 1em 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Privacy Policy for Roons Bot</h1>
                    <p><strong>Effective Date:</strong> 22 August 2024</p>
                    
                    <h2>1. Introduction</h2>
                    <p>This Privacy Policy explains how Roons Bot collects, uses, and protects your information. 
                    By using this bot on any Discord server, you consent to the data practices described in this policy.</p>
                    
                    <h2>2. Data Collection</h2>
                    <p>Roons Bot collects the following data to provide its functionality:</p>
                    <ul>
                        <li><strong>Guild ID:</strong> The unique identifier of the Discord server where the bot is used.</li>
                        <li><strong>User ID:</strong> Your unique Discord identifier.</li>
                        <li><strong>Voice Channel ID:</strong> The identifier of the voice channel where you interact with the bot.</li>
                        <li><strong>Volume Setting:</strong> Your preferred volume level for sound bites.</li>
                    </ul>
                    <p>This data is collected automatically when you use the bot and is essential for delivering the bot's features.</p>
                    
                    <h2>3. Data Usage</h2>
                    <p>The data collected by Roons Bot is used solely for the following purposes:</p>
                    <ul>
                        <li>Ensuring the correct functioning of the bot.</li>
                        <li>Providing users with a personalized experience (e.g., maintaining volume settings).</li>
                    </ul>
                    <p>No personal information beyond the data mentioned is collected, and this data is not shared with any third parties.</p>
                    
                    <h2>4. Data Retention</h2>
                    <p>The data will be stored for as long as necessary to provide the bot’s services. If you wish to have your data deleted, 
                    please contact the bot's developer, and your data will be removed upon request.</p>
                    
                    <h2>5. Data Security</h2>
                    <p>We take the protection of your data seriously. Appropriate measures are in place to prevent unauthorized access, use, 
                    or disclosure of your information.</p>
                    
                    <h2>6. Your Rights</h2>
                    <p>You have the right to:</p>
                    <ul>
                        <li>Request access to the data stored about you.</li>
                        <li>Request the correction or deletion of your data.</li>
                        <li>Withdraw your consent to data collection (though this may limit your ability to use the bot).</li>
                    </ul>
                    <p>To exercise any of these rights, please contact mrbean355@gmail.com.</p>
                    
                    <h2>7. Changes to This Policy</h2>
                    <p>This Privacy Policy may be updated periodically. You are encouraged to review this policy regularly. 
                    Your continued use of the bot signifies your acceptance of any changes.</p>
                    
                    <h2>8. Contact Information</h2>
                    <p>If you have any questions or concerns regarding this Privacy Policy, please contact mrbean355@gmail.com.</p>
                </div>
            </body>
            </html>
            """.trimIndent()
    }
}