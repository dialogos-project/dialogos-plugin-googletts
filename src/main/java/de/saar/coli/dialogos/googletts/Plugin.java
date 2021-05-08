package de.saar.coli.dialogos.googletts;

import com.clt.dialogos.plugin.PluginSettings;
import com.clt.gui.Images;
import com.clt.gui.OptionPane;
import com.clt.script.exp.ExecutableFunctionDescriptor;
import com.clt.speech.SpeechException;
import com.clt.speech.tts.VoiceName;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class Plugin implements com.clt.dialogos.plugin.Plugin {
    private static List<VoiceName> voices = new ArrayList<>();
    private static TextToSpeechClient client;
    private static AudioPlayer player = new AudioPlayer();
    private static Map<String,VoiceName> nameToVoice = new HashMap<>();

    @Override
    public void initialize() {
        File credentialsFilename = findCredentialsFile();

        try {
            if (credentialsFilename == null) {
                throw new RuntimeException(Resources.getString("CouldNotFindCredentials"));
            } else {
                GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsFilename));
                TextToSpeechSettings settings = TextToSpeechSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
                client = TextToSpeechClient.create(settings);

                collectVoices(client.listVoices("en-US"));
                collectVoices(client.listVoices("de-DE"));
                Collections.sort(voices, new VoiceNameComparator());

                // not registered if an exception happened above
                Node.registerNodeTypes(com.clt.speech.Resources.getResources().createLocalizedString("IONode"), Arrays.asList(Node.class));
            }
        } catch (Exception e) {
            OptionPane.error(null, Resources.getString("PluginDisabled") + " " + e.getMessage());
        }
    }

    // sorts by language, then voice name
    private static class VoiceNameComparator implements Comparator<VoiceName> {
        @Override
        public int compare(VoiceName o1, VoiceName o2) {
            GoogleVoiceWrapper g1 = (GoogleVoiceWrapper) o1.getVoice();
            GoogleVoiceWrapper g2 = (GoogleVoiceWrapper) o2.getVoice();

            String l1 = g1.getLanguage().getName();
            String l2 = g2.getLanguage().getName();

            if( ! l1.equals(l2) ) {
                return l1.compareTo(l2);
            } else {
                return g1.getName().compareTo(g2.getName());
            }
        }
    }

    private void collectVoices(ListVoicesResponse response) {
        for (Voice voice : response.getVoicesList()) {
            GoogleVoiceWrapper wrapped = new GoogleVoiceWrapper(voice);
            VoiceName vn = new VoiceName(wrapped.getName(), wrapped);
            voices.add(vn);
            nameToVoice.put(wrapped.getName(), vn);
        }
    }

    static VoiceName findVoice(String name) {
        return nameToVoice.get(name);
    }

    private static File findCredentialsFile() {
        File[] candidates = new File[]{
                new File("googletts-credentials.json"),
                new File(System.getProperty("user.home"), ".googletts-credentials.json"),
                new File(System.getProperty("user.home"), "googletts-credentials.json")
        };

        for (File candidate : candidates) {
            if (candidate.exists()) {
                return candidate;
            }
        }

        return null;
    }

    @Override
    public List<ExecutableFunctionDescriptor> registerScriptFunctions() {
        return new ArrayList<>();
    }

    public static List<VoiceName> getAvailableVoices() {
        return voices;
    }

    @Override
    public String getId() {
        return "dialogos-plugin-googletts";
    }


    @Override
    public String getName() {
        return "Google TTS";
    }


    @Override
    public Icon getIcon() {
        return Images.load(this, "TTS.png");
    }


    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public PluginSettings createDefaultSettings() {
        return new Settings();
    }

    public static void speak(String prompt, com.clt.speech.tts.Voice voice, boolean waitUntilFinished) throws SpeechException {
        // Run Google TTS
        SynthesisInput input = SynthesisInput.newBuilder().setText(prompt).build();
        VoiceSelectionParams v = VoiceSelectionParams.newBuilder().setLanguageCode("en-US").setName(voice.getName()).build(); // TODO fix this
        AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).build();
        SynthesizeSpeechResponse response = client.synthesizeSpeech(input, v, audioConfig);

        // Write to file
        File f;
        try {
            ByteString audioContents = response.getAudioContent();
            f = File.createTempFile("dialogos-googletts-", ".wav");
            f.deleteOnExit();
            OutputStream out = new FileOutputStream(f);
            out.write(audioContents.toByteArray());
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new SpeechException(e);
        }

        // Play audio
        try {
            player.play(f);

            if (waitUntilFinished) {
                player.waitUntilFinished();
            }
        } catch (Exception e) {
            throw new SpeechException(e);
        }
    }

    public static void stopSpeaking() {
        player.stop();
    }

    public static void waitUntilFinished() throws InterruptedException {
        player.waitUntilFinished();
    }

}
