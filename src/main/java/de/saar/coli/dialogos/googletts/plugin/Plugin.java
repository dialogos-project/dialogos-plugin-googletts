package de.saar.coli.dialogos.googletts.plugin;

import com.clt.dialogos.plugin.PluginSettings;
import com.clt.gui.Images;
import com.clt.script.exp.ExecutableFunctionDescriptor;
import com.clt.speech.SpeechException;
import com.clt.speech.tts.VoiceName;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Plugin implements com.clt.dialogos.plugin.Plugin {
    private static List<VoiceName> voices = new ArrayList<>();
    private static TextToSpeechClient client;
    private static AudioPlayer player = new AudioPlayer();

    @Override
    public void initialize() {
        Node.registerNodeTypes(com.clt.speech.Resources.getResources().createLocalizedString("IONode"),
                Arrays.asList(Node.class));


        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("singing-313106-78d694b595e2.json"));
            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
            client = TextToSpeechClient.create(settings);
            ListVoicesResponse response = client.listVoices("en-US");

            for(Voice voice : response.getVoicesList()) {
                GoogleVoiceWrapper wrapped = new GoogleVoiceWrapper(voice);
                voices.add(new VoiceName(wrapped.getName(), wrapped));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        System.err.printf("Saved synthesis to %s\n", f.getAbsolutePath());

        // Play audio
        try {
            player.play(f);
            System.err.println("now playing");

            if( waitUntilFinished ) {
                player.waitUntilFinished();
                System.err.println("done");
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