# Discord Roons Bot
[![Build Status](https://travis-ci.org/MrBean355/discord-roons-bot.svg?branch=master)](https://travis-ci.org/MrBean355/discord-roons-bot)

## About
This is a Discord bot for Dota 2. It will join your voice channel and play Admiral Bulldog's "ROONS" sound effect 15 seconds before the bounty runes spawn.

The sound effect can be previewed on the [official PlaySounds page](http://chatbot.admiralbulldog.live/playsounds). Search the list for "roons" and play it within your browser.

## Initial Setup
1. Invite the bot to your Discord server: [click me](https://discordapp.com/api/oauth2/authorize?client_id=602822492695953491&scope=bot&permissions=1)
2. Navigate to your Dota 2 installation folder (e.g. `C:\Program Files (x86)\Steam\steamapps\common\dota 2 beta`)
3. Within there, navigate to `game\dota\cfg\gamestate_integration` (create any folders that don't exist)
4. Create a new file called `gamestate_integration_roons.cfg`
5. Open the file in a text editor.
6. Paste this content:
    ```
    "Dota 2 Roons Discord Bot"
    {
        "uri"           "http://TODO:26382"
        "timeout"       "5.0"
        "buffer"        "0.1"
        "throttle"      "0.1"
        "heartbeat"     "30.0"
        "auth"
        {
           "token" "YOUR MAGIC NUMBER"
        }
        "data"
        {
            "provider"      "1"
            "map"           "1"
        }
    }
    ```
7. Type this command in a text channel in Discord: `!magic`
8. Replace `YOUR MAGIC NUMBER` on line 10 with the magic number the bot private messaged you 
9. Save & close the file
10. Start up Dota 2 and enter hero demo mode
11. Once the demo mode has loaded, type this command: `!test`
12. If you hear the ROONS sound in Discord, you're all set up! Go and queue for a match!
13. If you didn't hear anything, carefully run through the above steps again.

## Bot Usage
Once you've completed the above setup, join a voice channel. Make sure the bot has the correct Discord permissions to join it as well!

Type the `!roons` command. The bot will join your voice channel and will play the ROONS sound every 5 minutes.

## Available Commands
- `!help` - provides a link to this web page
- `!roons` - join your current voice channel
- `!seeya` - leave the current voice channel
- `!magic` - private message you with your magic number
- `!test` - play a sound as soon as Dota sends a game state update
- `!volume` - check the bot's current volume
- `!volume {value}` - set the bot's volume (e.g. `!volume 50`), min is `0`, max is `100`, default is `35`

## Bugs
If you encounter any issues, please file a bug at the "Issues" tab above!

## Troubleshooting
If you aren't hearing the sound effect in Discord, there are a few things to try:

#### Discord Permissions
If the bot isn't replying to your commands, make sure that it has the correct Discord permissions to be able to **view and send** messages in your text channel.

Similarly, if the bot is replying but not joining the voice channel, make sure that it has the correct Discord permissions to be able to **join and speak** in your voice channel.

#### Restart Dota
If you had Dota 2 open while creating the file, the game will ignore your file until it next starts up.

#### Check your magic number
Make sure you correctly inserted your magic number into the text file. There should be double quotations around it.

For example: line 10 in the text file should look similar to: `"token" "0088231z-ea6f-4af4-aa02-9d5e79cac2d6"` (substituted with your magic number).

#### Something else?
Carefully run through the setup instructions again.
 
If you are still having no luck, please open a new issue under the "Issues" tab above. It will help others if I can add your issue to this troubleshooting list. 