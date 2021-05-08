# Google TTS Plugin for DialogOS

This plugin adds a node for speech synthesis with [Google Text-to-Speech](https://cloud.google.com/text-to-speech). The quality of the synthesized speech is dramatically improved, compared to the built-in MaryTTS nodes.

Google TTS is a commercial product that may incur costs. You can find pricing details [here](https://cloud.google.com/text-to-speech/pricing); at the time of development, the first one million characters you synthesize each month are free.

## Usage

Drag the node "Google TTS" from the toolbar to the Canvas and use it as you would use the regular speech synthesis nodes in DialogOS.

## Credentials

You will need to configure your Google Account to permit the use of Google TTS. Follow [these instructions](https://cloud.google.com/text-to-speech/docs/quickstart-protocol) to set up a service account. 

After following the instructions, you will have downloaded a JSON key file to your computer. Copy this file to one of the following locations to make it accessible to DialogOS:

* `googletts-credentials.json` in your current working directory
* `.googletts-credentials.json` in your home directory (`~` on MacOS and Linux, `C:\Users\<Username>` on Windows)
* `googletts-credentials.json` in your home directory

If DialogOS cannot find one of these files when it starts up, the Google TTS plugin is disabled.
