package de.saar.coli.dialogos.googletts.plugin;

import com.clt.diamant.IdMap;
import com.clt.diamant.graph.nodes.AbstractOutputNode;
import com.clt.diamant.gui.NodePropertiesDialog;
import com.clt.gui.GUI;
import com.clt.gui.OptionPane;
import com.clt.speech.SpeechException;
import com.clt.speech.tts.VoiceName;
import com.clt.util.StringTools;
import com.clt.xml.XMLReader;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        System.err.println(voice);
        Plugin.speak(prompt, voice.getVoice(), isWaitUntilFinished());
    }

    private VoiceName getVoice() {
        if( cbVoices.getSelectedIndex() == 0 ) {
            // default voice
            Settings settings = (Settings) this.getGraph().getOwner().getPluginSettings(Plugin.class);
            return settings.getDefaultVoice();
        } else {
            VoiceName selectedVoiceName = (VoiceName) cbVoices.getSelectedItem();
            return selectedVoiceName;
        }
    }

    @Override
    public void stopSynthesis() {
        Plugin.stopSpeaking();
    }








    /*
     * The code below is an almost-exact duplicate of code in AbstractOutputNode.
     * It is a workaround for https://github.com/dialogos-project/dialogos/issues/214
     * and should be removed as soon as that issue is closed.
     */
    private JComboBox cbVoices;

    @Override
    public JComponent createEditorComponent(final Map<String, Object> properties) {

        List<VoiceName> voices = getAvailableVoices();

        Component standardEditor = NodePropertiesDialog.createTextArea(properties, PROMPT);
        Component groovyEditor = NodePropertiesDialog.createGroovyScriptEditor(properties, PROMPT);

        //Create first tab (User text)
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 3, 3, 3);

        p.add(new JLabel(getResourceString("Voice") + ':'), gbc);
        gbc.gridx++;
        cbVoices = NodePropertiesDialog.createComboBox(properties, VOICE, voices);
        p.add(cbVoices, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0.0;

        p.add(new JLabel(getResourceString("PromptType") + ':'), gbc);
        gbc.gridx++;
        gbc.weightx = 1.0;
        JPanel types = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JRadioButton button : NodePropertiesDialog.createRadioButtons(
                properties, PROMPT_TYPE,
                getDefaultPromptType().getValues())) {
            button.addItemListener(e -> {
                if(button.isSelected()) {
                    if (button.getText().equals(getDefaultPromptType().groovy().toString())) {
                        groovyEditor.setVisible(true);
                        standardEditor.setVisible(false);
                    } else {
                        standardEditor.setVisible(true);
                        groovyEditor.setVisible(false);
                    }
                }
            });
            types.add(button);
        }
        p.add(types, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        p.add(new JLabel(getResourceString("Prompt") + ':'), gbc);
        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        p.add(standardEditor, gbc);
        p.add(groovyEditor, gbc);

        if (properties.get(PROMPT_TYPE).equals(getDefaultPromptType().groovy())) {
            standardEditor.setVisible(false);
        } else {
            groovyEditor.setVisible(false);
        }

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        p.add(NodePropertiesDialog.createCheckBox(properties, WAIT,
                getResourceString("WaitUntilDone")), gbc);

        gbc.gridy++;
        p.add(NodePropertiesDialog.createCheckBox(properties, AWAIT_SILENCE,
                getResourceString("LetPreviousOutputFinish")), gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel border = new JPanel(new BorderLayout());
        final JButton tryPrompt = new JButton(getResourceString("Try"));
        tryPrompt.addActionListener(new ActionListener() {
            boolean speaking = false;

            private void reset() {
                stopSynthesis();
                this.speaking = false;
                tryPrompt.setText(getResourceString("Try"));
            }

            public void actionPerformed(ActionEvent e) {
                if (this.speaking) {
                    this.reset();
                }
                else {
                    new Thread(() -> {
                        try {
                            speaking = true;
                            tryPrompt.setText(GUI.getString("Cancel"));
                            speak(properties);
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
        });
        border.add(tryPrompt, BorderLayout.CENTER);
        border.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        p.add(border, gbc);

        //Add tabs
        JTabbedPane jtp = new JTabbedPane();
        jtp.addTab("Speech Synthesis", p);
        jtp = addMoreTabsToEditorComponent(jtp);
        return jtp;
    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) throws SAXException {
        if( name.equals(VOICE)) {
            VoiceName voice = Plugin.findVoice(value);
            if( voice != null ) {
                this.setProperty(VOICE, voice);
            }
        } else {
            super.readAttribute(r, name, value, uid_map);
        }
    }
}
