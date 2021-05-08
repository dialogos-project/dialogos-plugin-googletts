package de.saar.coli.dialogos.googletts;

import com.clt.dialogos.plugin.PluginRuntime;
import com.clt.dialogos.plugin.PluginSettings;
import com.clt.diamant.IdMap;
import com.clt.diamant.graph.Graph;
import com.clt.gui.GUI;
import com.clt.gui.OptionPane;
import com.clt.properties.*;
import com.clt.speech.tts.VoiceName;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Locale;

public class Settings extends PluginSettings {
    private final EnumProperty<VoiceName> defaultVoice;

    public Settings() {
        List<VoiceName> voices = Plugin.getAvailableVoices();

        if (voices.isEmpty()) {
            // plugin disabled because of an error in initialize()
            VoiceName dummy = new VoiceName(Resources.getString("DisabledPlugin"), null);
            voices.add(dummy);
        }


        defaultVoice = new DefaultEnumProperty<VoiceName>(
                "voice",
                Resources.getString("DefaultVoice"),
                null,
                voices.toArray(new VoiceName[voices.size()]), voices.get(0)) {

            @Override
            public String getName() {
                return Resources.getString("DefaultVoice");
            }

            @Override
            public void setValueFromString(String value) {
                for (VoiceName n : this.getPossibleValues()) {
                    if (n.toString().equals(value) || n.getName().equals(value)) {
                        this.setValue(n);
                        break;
                    }
                }
            }
        };

        if (!voices.isEmpty()) {
            this.defaultVoice.setValue(voices.iterator().next());
        }
    }

    @Override
    public void writeAttributes(XMLWriter out, IdMap uidMap) {
        Graph.printAtt(out, "defaultVoice", defaultVoice.getValue().getName());
    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) throws SAXException {
        if( name.equals("defaultVoice")) {
            VoiceName voice = Plugin.findVoice(value);

            if( voice != null ) {
                defaultVoice.setValue(voice);
            }
        }
    }

    @Override
    public JComponent createEditor() {
        JPanel p = new JPanel(new BorderLayout(12, 12));

        p.add(new PropertySet<Property<?>>(this.defaultVoice).createPropertyPanel(false),
                BorderLayout.NORTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));

        final JButton tryPrompt = new JButton(Resources.getString("Try"));
        tryPrompt.addActionListener(new TryPromptActionListener(tryPrompt));
        bottom.add(tryPrompt);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    @Override
    protected PluginRuntime createRuntime(Component parent) throws Exception {
        return null;
    }


    public VoiceName getDefaultVoice() {
        return this.defaultVoice.getValue();
    }


    private class TryPromptActionListener implements ActionListener {
        JButton tryPrompt;
        boolean speaking = false;

        TryPromptActionListener(JButton tryPrompt) {
            this.tryPrompt = tryPrompt;
        }

        private void reset() {
            Plugin.stopSpeaking();
            speaking = false;
            tryPrompt.setText(Resources.getString("Try"));
        }

        public synchronized void actionPerformed(ActionEvent e) {

            if (speaking) {
                this.reset();
            } else {
                new Thread(() -> {
                    try {
                        speaking = true;
                        tryPrompt.setText(GUI.getString("Cancel"));

                        Locale language = Settings.this.defaultVoice.getValue().getVoice().getLanguage().getLocale();
                        if (language.equals(Locale.UK) || language.equals(Locale.US)) {
                            language = new Locale("", "");
                        }

                        String prompt = Resources.format("VoiceSample", language, Settings.this.defaultVoice.getValue().getNormalizedName());
                        Plugin.speak(prompt, Settings.this.defaultVoice.getValue().getVoice(), true);
                    } catch (Exception exn) {
                        String msg = exn.getLocalizedMessage();
                        if ((msg == null) || (msg.length() == 0)) {
                            msg = exn.getClass().getName();
                        }
                        OptionPane.error(tryPrompt, msg);
                    }
                    reset();
                }).start();
            }
        }

    }
}
