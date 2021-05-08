package de.saar.coli.dialogos.googletts;

import com.clt.speech.Language;
import com.clt.speech.tts.Voice;
import com.clt.speech.tts.VoiceName;

public class GoogleVoiceWrapper implements Voice {
    private com.google.cloud.texttospeech.v1.Voice voice;

    public GoogleVoiceWrapper(com.google.cloud.texttospeech.v1.Voice voice) {
        this.voice = voice;
        System.err.printf("NEW VOICE %s: %s\n", getLanguage(), getName());
    }

    @Override
    public String getName() {
        return voice.getName();
    }

    @Override
    public Language getLanguage() {
        return new Language(voice.getLanguageCodes(0).replaceAll("-", "_"));
    }

    public com.google.cloud.texttospeech.v1.Voice getGoogleVoice() {
        return voice;
    }
}
