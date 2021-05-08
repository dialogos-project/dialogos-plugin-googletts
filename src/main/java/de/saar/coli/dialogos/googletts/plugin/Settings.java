package de.saar.coli.dialogos.googletts.plugin;

import com.clt.dialogos.plugin.PluginRuntime;
import com.clt.dialogos.plugin.PluginSettings;
import com.clt.diamant.IdMap;
import com.clt.properties.DefaultEnumProperty;
import com.clt.properties.EnumProperty;
import com.clt.properties.Property;
import com.clt.properties.PropertySet;
import com.clt.speech.tts.Voice;
import com.clt.speech.tts.VoiceName;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Settings extends PluginSettings {
    private final EnumProperty<VoiceName> defaultVoice;

    public Settings() {
        List<VoiceName> voices = Plugin.getAvailableVoices();

        this.defaultVoice = new DefaultEnumProperty<VoiceName>("voice",
                Resources.getString("DefaultVoice"), null, voices
                .toArray(new VoiceName[voices.size()]), voices.get(0)) {

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

    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) throws SAXException {

    }

    @Override
    public JComponent createEditor() {
        JPanel p = new JPanel(new BorderLayout(12, 12));

        p.add(new PropertySet<Property<?>>(this.defaultVoice).createPropertyPanel(false),
                BorderLayout.NORTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));

        final JButton tryPrompt = new JButton(Resources.getString("Try"));
//        tryPrompt.addActionListener(new TryPromptActionListener(tryPrompt)); // TODO
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


}
