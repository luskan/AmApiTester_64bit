package org.example;

import com.sun.jna.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Main {
    // ---- JNA struct mappings ----
    public static class CVersionInfo extends Structure {
        public short ucMajorVersion;
        public short ucMinorVersion;
        public short ucMajorBuild;
        public short ucMinorBuild;
        public byte  ucPlatform;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "ucMajorVersion","ucMinorVersion","ucMajorBuild","ucMinorBuild","ucPlatform"
            );
        }
    }

    public static class ApiInitOptions extends Structure {
        public boolean bStartAmIfNotRunning;
        public int     dwTimeout;
        public Pointer pbProcessCreated;
        public boolean bFastStartMode;
        public boolean bKeepInBack;
        public byte[]  szInitLang = new byte[16];
        public byte[]  szMapPath  = new byte[512];
        public byte[]  szProfile  = new byte[256];

        public ApiInitOptions() {
            bStartAmIfNotRunning = true;
            dwTimeout = 60000;
            pbProcessCreated = null;
            bFastStartMode = false;
            bKeepInBack = false;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "bStartAmIfNotRunning","dwTimeout","pbProcessCreated",
                    "bFastStartMode","bKeepInBack",
                    "szInitLang","szMapPath","szProfile"
            );
        }
    }

    // ---- JNA interface ----
    public interface AmApi extends Library {
        void      GetAmApiVersion(CVersionInfo pv);
        boolean   GetAmVersion(CVersionInfo pv);
        boolean   GetAmPath(char[] path);
        void      SetAmApiRecieveTimeout(int timeout);
        boolean   AmApiInit(ApiInitOptions opts);
        void      AmApiDone();
        boolean   IsAmAndApiReady();
        boolean   GetAmCurrentLanguage(char[] lang);
        boolean   PostCommandToAm(String command, boolean bBeep);
        boolean   CloseAm(boolean bKillIfNotResponding);
        double    AmMetersToScale(int meters);
    }

    // ---- Fields for dynamic DLL selection ----
    private static String dllPath = "P:\\AmEU\\tpcAmApi_D.dll";
    private static AmApi    api     = Native.load(dllPath, AmApi.class);

    private JTextArea output;

    public Main() {
        final JFrame frame = new JFrame("AutoMapa API Tester");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);

        // Show architecture info and JRE location at bottom
        String bits = System.getProperty("sun.arch.data.model");
        String info = "Running under " + bits + "-bit JRE";
        if ("64".equals(bits)) {
            info += " at " + System.getProperty("java.home");
        }
        JTextField archField = new JTextField(info);
        archField.setEditable(false);

        // Panel for buttons
        JPanel btnPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        output = new JTextArea();
        output.setEditable(false);
        JScrollPane scroll = new JScrollPane(output);

        // Button: Select DLL
        btnPanel.add(makeButton("Select DLL...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select AutoMapa API DLL");
                chooser.setSelectedFile(new File(dllPath));
                int res = chooser.showOpenDialog(frame);
                if (res == JFileChooser.APPROVE_OPTION) {
                    File f = chooser.getSelectedFile();
                    dllPath = f.getAbsolutePath();
                    api = Native.load(dllPath, AmApi.class);
                    append("DLL path set to: " + dllPath);
                }
            }
        }));

        // Button: Get API Version
        btnPanel.add(makeButton("Get API Version", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CVersionInfo vi = new CVersionInfo();
                api.GetAmApiVersion(vi);
                append("API version: " + vi.ucMajorVersion + "." + vi.ucMinorVersion + "." + vi.ucMajorBuild + "." + vi.ucMinorBuild + " (platform=" + (vi.ucPlatform & 0xFF) + ")");
            }
        }));

        // Button: Get installed AutoMapa version
        btnPanel.add(makeButton("Get AutoMapa Version", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CVersionInfo vi = new CVersionInfo();
                if (api.GetAmVersion(vi)) {
                    append("AutoMapa version: " + vi.ucMajorVersion + "." + vi.ucMinorVersion + "." + vi.ucMajorBuild + "." + vi.ucMinorBuild);
                } else {
                    append("AutoMapa not installed or call failed.");
                }
            }
        }));

        // Button: Get AutoMapa install path
        btnPanel.add(makeButton("Get AutoMapa Path", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                char[] buf = new char[512];
                if (api.GetAmPath(buf)) {
                    append("AutoMapa path: " + Native.toString(buf));
                } else {
                    append("Failed to get path.");
                }
            }
        }));

        // Button: Initialize API
        btnPanel.add(makeButton("Init API", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ApiInitOptions opts = new ApiInitOptions();
                if (api.AmApiInit(opts)) {
                    append("API initialized.");
                } else {
                    append("API init failed.");
                }
            }
        }));

        // Button: Check readiness
        btnPanel.add(makeButton("Is API Ready?", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                append("API ready: " + api.IsAmAndApiReady());
            }
        }));

        // Button: Done API
        btnPanel.add(makeButton("Done API", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                api.AmApiDone();
                append("API done.");
            }
        }));

        // Button: Get current language
        btnPanel.add(makeButton("Get Language", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                char[] lang = new char[16];
                if (api.GetAmCurrentLanguage(lang)) {
                    append("Language: " + Native.toString(lang));
                } else {
                    append("Failed to get language.");
                }
            }
        }));

        // Button: Zoom to 1 km
        btnPanel.add(makeButton("Zoom to 1 km", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                append("PostCommand: " + api.PostCommandToAm("showmap %lat %lon 1000", false));
            }
        }));

        // Button: Close AutoMapa
        btnPanel.add(makeButton("Close AutoMapa", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                append("CloseAm: " + api.CloseAm(true));
            }
        }));

        // Button: Meters→Scale (2 km)
        btnPanel.add(makeButton("Meters→Scale (2 km)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                append("Scale for 2000 m: " + api.AmMetersToScale(2000));
            }
        }));

        frame.getContentPane().add(btnPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(archField, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private JButton makeButton(String text, ActionListener act) {
        JButton b = new JButton(text);
        b.addActionListener(act);
        return b;
    }

    private void append(String line) {
        output.append(line + "\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Main();
            }
        });
    }
}
