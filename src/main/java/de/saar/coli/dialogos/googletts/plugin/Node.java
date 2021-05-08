package de.saar.coli.dialogos.googletts.plugin;

import com.clt.diamant.graph.nodes.AbstractOutputNode;
import com.clt.speech.SpeechException;
import com.clt.speech.tts.VoiceName;
import com.clt.util.StringTools;

import java.util.List;
import java.util.Map;

public class Node extends AbstractOutputNode {
    public Node() {
    }

    public static String getNodeTypeName(Class x) {
        return "Google TTS";
    }

    @Override
    public String getResourceString(String key) {
        return Resources.getString(key);
    }

    @Override
    public List<VoiceName> getAvailableVoices() {
        List<VoiceName> voices = Plugin.getAvailableVoices();
        voices.add(0, new VoiceName("", null));

        if (!voices.contains(properties.get(VOICE))) {
            voices.add(1, (VoiceName) properties.get(VOICE));
        }
        return voices;
    }

    private boolean isWaitUntilFinished() {
        return (Boolean) properties.get(WAIT);
    }

    private boolean isLetPreviousOutputFinish() {
        return (Boolean) properties.get(AWAIT_SILENCE);
    }

    @Override
    public void speak(String prompt, Map<String, Object> properties) throws SpeechException {
        // ensure silence
        if( isLetPreviousOutputFinish() ) {
            try {
                Plugin.waitUntilFinished();
            } catch (InterruptedException e) {
                throw new SpeechException(e);
            }
        } else {
            Plugin.stopSpeaking();
        }

        VoiceName voice = getVoice();
        Plugin.speak(prompt, voice.getVoice(), isWaitUntilFinished());
    }

    private VoiceName getVoice() {
        Settings settings = (Settings) this.getGraph().getOwner().getPluginSettings(Plugin.class);
        VoiceName voicename = (VoiceName) properties.get(VOICE);
        if ((voicename == null) || StringTools.isEmpty(voicename.getName())) {
            voicename = settings.getDefaultVoice();
        }
        return voicename;
    }

    @Override
    public void stopSynthesis() {
        Plugin.stopSpeaking();
    }
}
