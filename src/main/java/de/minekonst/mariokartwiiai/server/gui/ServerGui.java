package de.minekonst.mariokartwiiai.server.gui;

import de.minekonst.mariokartwiiai.shared.utils.FileUtils;
import de.minekonst.mariokartwiiai.shared.utils.TimeUtils;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.server.AIServer;
import de.minekonst.mariokartwiiai.server.RemoteDriver;
import de.minekonst.mariokartwiiai.server.ai.AI;
import de.minekonst.mariokartwiiai.server.ai.types.ArchivedNetwork;
import de.minekonst.mariokartwiiai.server.gui.dialogs.CreateAI;
import de.minekonst.mariokartwiiai.server.gui.dialogs.EditAI;
import de.minekonst.mariokartwiiai.server.gui.dialogs.TaskDialog;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.ConsoleHandler;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public class ServerGui extends javax.swing.JFrame {

    public static final String AI_DIR = Main.getDataDir() + File.separator + "AIs";
    public static final int UPDATE_AI_TABLE_INTERVALL = 40_000;

    private final AIServer server;

    private final ArrayList<AI> savedAIs = new ArrayList<>(5);
    private long lastAITableUpdate;

    public ServerGui(AIServer server) {
        this.server = server;
        initComponents();

        serverMapViewer1.setServer(server);

        DefaultTableModel model = (DefaultTableModel) clients.getModel();
        resizeTable(model, 0);

        Thread t = new Thread(() -> {
            while (true) {
                // Update Client Table
                List<RemoteDriver> cl = server.getRemoteDrivers();
                if (cl.size() != clients.getRowCount()) {
                    resizeTable(model, cl.size());
                }

                int x = 0;
                try {
                    for (RemoteDriver s : cl) {
                        model.setValueAt("Driver #" + s.getServerClient().getID(), x, 0);
                        model.setValueAt(s.getServerClient().getIP(), x, 1);
                        model.setValueAt(s.getState().toString(), x, 2);
                        model.setValueAt(s.getMessage(), x, 3);
                        model.setValueAt(s.getFps(), x, 4);
                        model.setValueAt(s.getPing() + " ms", x, 5);
                        model.setValueAt(s.isUsedForAI(), x, 6);
                        x++;
                    }
                }
                catch (ConcurrentModificationException ex) {
                }

                String def = "/";
                AI ai = server.getAI();

                // Gray out buttons etc
                loadAIButton.setEnabled(ais.getSelectedRow() != -1);
                editAI.setEnabled(ais.getSelectedRow() != -1);
                unload.setEnabled(ai != null);
                export.setEnabled(ai != null);

                // Current AI
                if (ai != null) {
                    long s = ai.getFileSize();
                    double mb = s / 1_000_000.0;
                    cFile.setText(String.format("%s (%.2f MB)", ai.getName(), mb));
                }
                else {
                    cFile.setText(def);
                }
                cNetwork.setText(ai != null ? ai.toString() : def);
                cState.setText(ai != null ? ai.getState() : "Load an AI");
                cFov.setText(ai != null ? ai.getLearningMethod().getInputMethod().toString() : def);

                cEra.setText(ai != null ? "" + ai.getProperties().getEra().getValue() : def);
                cSpecies.setText(ai != null ? "" + ai.getProperties().getSpecies().getValue() : def);
                cGeneration.setText(ai != null ? "" + ai.getProperties().getGeneration().getValue() : def);
                cGenomesPerGen.setText(ai != null ? "" + ai.getProperties().getGenomesPerGeneration().getValue() : def);

                cEndTime.setText(ai != null ? (ai.getBestTime() != null ? ai.getBestTime().toString() : "-") : def);

                cTrack.setText(ai != null ? ai.getProperties().getTrack().toString() : def);
                cScore.setText(ai != null ? String.format("%.2f (%.2f)", ai.getProperties().getScore().getValue(), ai.getProperties().getMaxScore().getValue()) : def);
                if (ai != null) {
                    cTime.setText(ai.getLearnTime());
                }
                else {
                    cTime.setText(def);
                }

                jButton1.setEnabled(server.getAI() != null);

                // Controll
                sState.setText("Scheduler: " + (server.isAIActive() ? "Active" : "Off"));
                sStart.setText(server.isAIActive() ? "Pause" : "Start");
                sStart.setEnabled(server.getAI() != null);
                sTask.setEnabled(server.getAI() != null && !server.isAIActive());
                sBest.setEnabled(!server.isAIActive() && server.getAI() instanceof AI);

                if (System.currentTimeMillis() - lastAITableUpdate > UPDATE_AI_TABLE_INTERVALL) {
                    int index = ais.getSelectedRow();
                    updateSavedAITable();
                    if (index != -1) {
                        ais.setRowSelectionInterval(index, index);
                    }
                }

                if (serverMapViewer1.isShowing()) {
                    serverMapViewer1.repaint();
                }

                TimeUtils.sleep(50);
            }
        }, "GUI Updater");
        t.setDaemon(true);
        t.start();

        File dir = new File(AI_DIR);
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".xml")) {
                try {
                    savedAIs.add(new AI<>(f.getName()));
                }
                catch (Exception ex) {
                    Main.log("Cannot read AI File %s: %s", f.getAbsoluteFile(), ex.toString());
                    ex.printStackTrace();
                }
            }
        }

        updateSavedAITable();
    }

    public void addAI(String path) {
        savedAIs.add(new AI<>(new File(path).getName()));

        updateSavedAITable();
    }

    private void updateSavedAITable() {
        lastAITableUpdate = System.currentTimeMillis();

        DefaultTableModel model = (DefaultTableModel) ais.getModel();
        int rowCount = model.getRowCount();
        for (int x = 0; x < rowCount; x++) {
            model.removeRow(0);
        }

        for (AI<?, ?> ai : savedAIs) {
            double time = ai.getProperties().getTotalTime().getValue();
            int minutes = (int) (time / 60) % 60;
            int hours = (int) ((time / 60) / 60);

            model.addRow(new Object[]{ai.getName(), ai.getProperties().getTrack().getValue().toString(),
                ai.getProperties().getEra().getValue(), ai.getProperties().getSpecies().getValue(), ai.getProperties().getGeneration().getValue(),
                String.format("%d h %d min", hours, minutes),
                String.format("%.2f", ai.getProperties().getScore().getValue())});
        }
    }

    private void resizeTable(DefaultTableModel model, int size) {
        int rowCount = model.getRowCount();
        for (int x = 0; x < rowCount; x++) {
            model.removeRow(0);
        }
        for (int x = 0; x < size; x++) {
            model.addRow(new Object[]{"/", "/", "/", "/", "/", "/"});
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        clients = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        ais = new javax.swing.JTable();
        loadAIButton = new javax.swing.JButton();
        editAI = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        cFileLab = new javax.swing.JLabel();
        cNetwork = new javax.swing.JLabel();
        cFov = new javax.swing.JLabel();
        unload = new javax.swing.JButton();
        cSpecies = new javax.swing.JLabel();
        cEra = new javax.swing.JLabel();
        cGeneration = new javax.swing.JLabel();
        cGenomesPerGen = new javax.swing.JLabel();
        cTrack = new javax.swing.JLabel();
        cScore = new javax.swing.JLabel();
        cFile = new javax.swing.JLabel();
        cFileLab1 = new javax.swing.JLabel();
        cFileLab2 = new javax.swing.JLabel();
        cFileLab3 = new javax.swing.JLabel();
        cFileLab4 = new javax.swing.JLabel();
        cFileLab5 = new javax.swing.JLabel();
        cFileLab6 = new javax.swing.JLabel();
        cFileLab7 = new javax.swing.JLabel();
        cFileLab8 = new javax.swing.JLabel();
        cFileLab9 = new javax.swing.JLabel();
        cTime = new javax.swing.JLabel();
        export = new javax.swing.JButton();
        cFileLab10 = new javax.swing.JLabel();
        cEndTime = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        cState = new javax.swing.JLabel();
        sState = new javax.swing.JLabel();
        sStart = new javax.swing.JButton();
        sTask = new javax.swing.JButton();
        sBest = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        console = new javax.swing.JTextPane();
        jPanel7 = new javax.swing.JPanel();
        serverMapViewer1 = new de.minekonst.mariokartwiiai.server.gui.ServerMapViewer();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Clients", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 18))); // NOI18N

        clients.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Client", "IP", "Status", "Score", "FPS", "Ping", "Use for AI"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        clients.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                clientsPropertyChange(evt);
            }
        });
        jScrollPane1.setViewportView(clients);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 477, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "AIs", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 18))); // NOI18N

        ais.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Track", "Era", "Species", "Generation", "Learntime", "Score"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(ais);

        loadAIButton.setText("Load");
        loadAIButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadAIButtonActionPerformed(evt);
            }
        });

        editAI.setText("Edit...");
        editAI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editAIActionPerformed(evt);
            }
        });

        jButton2.setText("New...");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editAI)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(loadAIButton)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(loadAIButton)
                    .addComponent(editAI)
                    .addComponent(jButton2))
                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Current AI", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 18))); // NOI18N

        cFileLab.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab.setText("File:");

        cNetwork.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cNetwork.setText("<No AI>");

        cFov.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFov.setText("<No AI>");

        unload.setText("Unload");
        unload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unloadActionPerformed(evt);
            }
        });

        cSpecies.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cSpecies.setText("<No AI>");

        cEra.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cEra.setText("<No AI>");

        cGeneration.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cGeneration.setText("<No AI>");

        cGenomesPerGen.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cGenomesPerGen.setText("<No AI>");

        cTrack.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cTrack.setText("<No AI>");

        cScore.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cScore.setText("<No AI>");

        cFile.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFile.setText("<No AI>");

        cFileLab1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab1.setText("Type:");

        cFileLab2.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab2.setText("Input Method:");

        cFileLab3.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab3.setText("Era:");

        cFileLab4.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab4.setText("Species:");

        cFileLab5.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab5.setText("Generation:");

        cFileLab6.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab6.setText("Genomes / Gen:");

        cFileLab7.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab7.setText("Track:");

        cFileLab8.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab8.setText("Score:");

        cFileLab9.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab9.setText("Time:");

        cTime.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cTime.setText("<No AI>");

        export.setText("Export...");
        export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportActionPerformed(evt);
            }
        });

        cFileLab10.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cFileLab10.setText("End Time:");

        cEndTime.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cEndTime.setText("<No AI>");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cFileLab, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cFileLab1, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cFileLab2, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(cFov, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                                    .addComponent(cNetwork, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(cFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cFileLab3, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cFileLab4, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cFileLab5, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cFileLab6, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cFileLab7, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cFileLab8, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(cGenomesPerGen, javax.swing.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                                    .addComponent(cGeneration, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(cEra, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cSpecies, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(cTrack, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(cScore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(cFileLab9, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addContainerGap(144, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(unload)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(export)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(cFileLab10, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cEndTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cFileLab)
                    .addComponent(cFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cNetwork)
                    .addComponent(cFileLab1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cFov)
                    .addComponent(cFileLab2))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cEra)
                    .addComponent(cFileLab3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cSpecies)
                    .addComponent(cFileLab4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cGeneration)
                    .addComponent(cFileLab5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cGenomesPerGen)
                    .addComponent(cFileLab6))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cTrack)
                    .addComponent(cFileLab7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cScore)
                    .addComponent(cFileLab8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cFileLab9)
                    .addComponent(cTime))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cFileLab10)
                    .addComponent(cEndTime))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 49, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(unload)
                    .addComponent(export))
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Controll", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 18))); // NOI18N

        cState.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        cState.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cState.setText("<State>");
        cState.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        cState.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        sState.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        sState.setText("Scheduler: <State>");

        sStart.setText("Start");
        sStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sStartActionPerformed(evt);
            }
        });

        sTask.setText("Create Task...");
        sTask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sTaskActionPerformed(evt);
            }
        });

        sBest.setText("Set Best...");
        sBest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sBestActionPerformed(evt);
            }
        });

        jButton1.setText("Statistics");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cState, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sState, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(sStart)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sBest)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sTask)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cState, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(sState)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sStart)
                    .addComponent(sTask)
                    .addComponent(sBest)
                    .addComponent(jButton1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Console", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 18))); // NOI18N

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane2.setViewportView(console);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Overview", jPanel6);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(serverMapViewer1, javax.swing.GroupLayout.DEFAULT_SIZE, 1029, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(serverMapViewer1, javax.swing.GroupLayout.DEFAULT_SIZE, 619, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Map", jPanel7);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void clientsPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_clientsPropertyChange
        int x = 0;
        for (RemoteDriver s : server.getRemoteDrivers()) {
            s.setUsedForAI((boolean) clients.getValueAt(x, 6));
            x++;
        }
    }//GEN-LAST:event_clientsPropertyChange

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        new CreateAI(this);
        updateSavedAITable();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void loadAIButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadAIButtonActionPerformed
        if (ais.getSelectedRow() != -1) {
            server.setAI(savedAIs.get(ais.getSelectedRow()), this);
        }
    }//GEN-LAST:event_loadAIButtonActionPerformed

    private void unloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unloadActionPerformed
        server.setAI(null, this);
    }//GEN-LAST:event_unloadActionPerformed

    private void editAIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editAIActionPerformed
        int id = ais.getSelectedRow();
        if (id == -1) {
            return;
        }
        new EditAI(savedAIs.get(id), this);
        updateSavedAITable();
    }//GEN-LAST:event_editAIActionPerformed

    private void sStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sStartActionPerformed
        server.setAIActive(!server.isAIActive());
    }//GEN-LAST:event_sStartActionPerformed

    private void sTaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sTaskActionPerformed
        if (server.getAI() != null)
            new TaskDialog(this, server, server.getAI());
    }//GEN-LAST:event_sTaskActionPerformed

    private void exportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportActionPerformed
        if (server.getAI() == null) {
            return;
        }

        AI ai = server.getAI();
        Object net = ai.getProperties().getDataHolder().getBaseNetwork();
        if (net == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();

        File dir = new File(AI_DIR);

        fileChooser.setCurrentDirectory(dir);

        fileChooser.setDialogTitle("Save best network");
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        FileFilter filter = new FileNameExtensionFilter("Neural Neat Network", "neat");
        fileChooser.setFileFilter(filter);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileHidingEnabled(true);

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(fileChooser,
                        "The file at \"" + file.getAbsolutePath()
                        + "\"already exists, do you want to override it?",
                        "Existing file", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                String s = file.getAbsolutePath();
                if (!s.endsWith(".neat")) {
                    s += ".neat";
                }
                FileUtils.writeObject(s, (Serializable) net);
            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Cannot save the network: " + ex.toString(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_exportActionPerformed

    private void sBestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sBestActionPerformed
        if (JOptionPane.showConfirmDialog(this,
                "Editing the best network can break the AI. Are you sure you want to continue?",
                "Cirtical Warning", JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        if (server.getAI() == null || server.isAIActive()) {
            return;
        }

        if (!(server.getAI() instanceof AI)) {
            return;
        }

        AI<?, ?> ai = (AI) server.getAI();

        ArrayList<ArchivedNetwork<?, ?>> list = new ArrayList<>(ai.getArchivedNetworks());
        Object[] arr = list.toArray();

        ArchivedNetwork<?, ?> a = (ArchivedNetwork) JOptionPane.showInputDialog(
                this, "", "Choose the network",
                JOptionPane.PLAIN_MESSAGE,
                null,
                arr,
                null);

        if (a != null) {
            ai.getProperties().getDataHolder().setBaseNetwork(a.getNetworkUnchecked(ai.getLearningMethod()));
        }
    }//GEN-LAST:event_sBestActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if (server.getAI() != null) {
            server.getAI().showStatistics();
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    public ServerMapViewer getMapViewer() {
        return serverMapViewer1;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable ais;
    private javax.swing.JLabel cEndTime;
    private javax.swing.JLabel cEra;
    private javax.swing.JLabel cFile;
    private javax.swing.JLabel cFileLab;
    private javax.swing.JLabel cFileLab1;
    private javax.swing.JLabel cFileLab10;
    private javax.swing.JLabel cFileLab2;
    private javax.swing.JLabel cFileLab3;
    private javax.swing.JLabel cFileLab4;
    private javax.swing.JLabel cFileLab5;
    private javax.swing.JLabel cFileLab6;
    private javax.swing.JLabel cFileLab7;
    private javax.swing.JLabel cFileLab8;
    private javax.swing.JLabel cFileLab9;
    private javax.swing.JLabel cFov;
    private javax.swing.JLabel cGeneration;
    private javax.swing.JLabel cGenomesPerGen;
    private javax.swing.JLabel cNetwork;
    private javax.swing.JLabel cScore;
    private javax.swing.JLabel cSpecies;
    private javax.swing.JLabel cState;
    private javax.swing.JLabel cTime;
    private javax.swing.JLabel cTrack;
    private javax.swing.JTable clients;
    private javax.swing.JTextPane console;
    private javax.swing.JButton editAI;
    private javax.swing.JButton export;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JButton loadAIButton;
    private javax.swing.JButton sBest;
    private javax.swing.JButton sStart;
    private javax.swing.JLabel sState;
    private javax.swing.JButton sTask;
    private de.minekonst.mariokartwiiai.server.gui.ServerMapViewer serverMapViewer1;
    private javax.swing.JButton unload;
    // End of variables declaration//GEN-END:variables

}
