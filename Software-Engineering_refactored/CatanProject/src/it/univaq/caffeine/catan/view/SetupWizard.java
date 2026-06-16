package it.univaq.caffeine.catan.view;

import it.univaq.caffeine.catan.controller.GameController;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * View — Setup wizard for UC 1.0 (SD1-SD5).
 *
 * Collects user input step by step and calls the GameController
 * with system operations in the correct order:
 * startNewGame() → selectGameMode() → setNumberOfPlayers()
 * → setNumHumans() → setBotDifficulty() → addPlayerName() → confirmGameSetup()
 */
public class SetupWizard {

    // ── 1. Instance Variables ─────────────────────────────────────

    private final GameController gc;


    // ── 2. Constructor ────────────────────────────────────────────

    public SetupWizard(GameController gc) {
        this.gc = gc;
    }


    // ── 3. Core Wizard Logic ──────────────────────────────────────

    /**
     * Executes the interactive setup wizard.
     * @return true if the setup is completed, false if the user cancelled.
     */
    public boolean run() {

        int modeIdx = 0;
        int nIdx    = 0;
        int hIdx    = 0;
        int dIdx    = 0;
        List<String> names = new ArrayList<>();

        // Populated in step 0 (SD1) — used throughout the wizard
        List<String> modes = null;
        int[]        range = null;   // {minPlayers, maxPlayers} from GameConfig

        int step = 0;
        boolean done = false;

        // ── Setup State Machine ───────────────────────────────────────
        while (!done) {
            switch (step) {

                // ── STEP 0: SD1 — startNewGame() ──
                case 0: {
                    modes = gc.startNewGame();      // Only call: creates the Game instance
                    range = gc.getPlayerRange();    // Fetched from GameConfig, no hardcoding

                    String[] opts = modes.toArray(new String[0]);
                    int r = JOptionPane.showOptionDialog(null,
                            "Select game mode:",
                            "New Game — Step 1 of 4",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                            null, opts, opts[modeIdx]);

                    if (r < 0) return false;
                    modeIdx = r;
                    step = 1;
                    break;
                }

                // ── STEP 1: SD3 — setNumberOfPlayers() ──
                case 1: {
                    boolean botMode = (modeIdx == 1);
                    int min = range[0], max = range[1];
                    String[] opts = new String[max - min + 1];
                    for (int i = 0; i < opts.length; i++) {
                        opts[i] = (min + i) + " players total";
                    }

                    String msg = botMode
                            ? "How many players in total? (1 human + bots)"
                            : "How many players in total (humans + bots)?";

                    int r = showWithBack(msg, "New Game — Step 2 of 4", opts, nIdx);
                    if (r == -2) return false;
                    if (r == -1) { step = 0; break; }

                    nIdx = r;
                    step = 2;
                    break;
                }

                // ── STEP 2: setNumHumans() ──
                case 2: {
                    boolean botMode = (modeIdx == 1);

                    // In bot mode there is always exactly 1 human, skip this step
                    if (botMode) {
                        hIdx = 0;   // 1 human, the rest are bots
                        step = 3;
                        break;
                    }

                    // Local mode: everything proceeds normally
                    int n    = range[0] + nIdx;
                    int maxH = n;  // In local mode, everyone can be human

                    String[] opts = new String[maxH];
                    for (int i = 0; i < maxH; i++) {
                        opts[i] = (i + 1) + " human player" + (i > 0 ? "s" : "");
                    }

                    int r = showWithBack(
                            "How many are human players? (the rest will be bots)",
                            "New Game — Step 3 of 4", opts, Math.min(hIdx, maxH - 1));

                    if (r == -2) return false;
                    if (r == -1) { step = 1; break; }

                    hIdx = r;
                    step = 3;
                    break;
                }

                // ── STEP 3: setBotDifficulty() (Skipped if no bots) ──
                case 3: {
                    int numBots = (range[0] + nIdx) - (hIdx + 1);
                    if (numBots == 0) { step = 4; break; }

                    String[] diffs = gc.getBotDifficulties().toArray(new String[0]);
                    int r = showWithBack(
                            "Bot difficulty (" + numBots + " bot" + (numBots > 1 ? "s" : "") + "):",
                            "New Game — Step 4 of 4", diffs, dIdx);

                    if (r == -2) return false;

                    // In bot mode, 'Back' returns to step 1 (step 2 is skipped)
                    if (r == -1) { step = (modeIdx == 1) ? 1 : 2; break; }

                    dIdx = r;
                    step = 4;
                    break;
                }

                // ── STEP 4: SD4 — addPlayerName() ──
                case 4: {
                    names.clear();
                    int          numHumans = hIdx + 1;
                    List<String> colors    = gc.getPlayerColors();  // Fetched from GameConfig
                    boolean      back      = false;
                    boolean      cancelled = false;

                    for (int i = 0; i < numHumans; i++) {
                        String color = (i < colors.size()) ? colors.get(i) : "Player " + (i + 1);
                        String[] nameOut = {""};

                        int r = showNameInput(
                                "Player " + (i + 1) + " of " + numHumans,
                                "Player name " + (i + 1) + " (" + color + "):",
                                nameOut);

                        if (r == -2) { cancelled = true; break; }
                        if (r == -1) { back      = true; break; }

                        String name = nameOut[0].trim();
                        if (name.isBlank()) name = "Player " + (i + 1);
                        names.add(name);
                    }

                    if (cancelled) return false;
                    if (back) {
                        // In bot mode there are always bots → return to difficulty selection
                        if (modeIdx == 1) {
                            step = 3;
                        } else {
                            int numBots = (range[0] + nIdx) - (hIdx + 1);
                            step = (numBots > 0) ? 3 : 2;
                        }
                        break;
                    }

                    int n = range[0] + nIdx;
                    for (int i = numHumans; i < n; i++) {
                        names.add("Bot " + (i - numHumans + 1));
                    }

                    done = true;
                    break;
                }
            }
        }

        // ── APPLICATION OF COLLECTED CONFIGURATIONS (SD2-SD5) ─────────

        int n         = range[0] + nIdx;
        int numHumans = hIdx + 1;
        int numBots   = n - numHumans;

        gc.selectGameMode(modes.get(modeIdx));  // SD2
        gc.setNumberOfPlayers(n);               // SD3
        gc.setNumHumans(numHumans);

        List<String> botDiffs = gc.getBotDifficulties();
        String difficulty = botDiffs.get(0);    // default = first element ("Easy")

        if (numBots > 0) {
            difficulty = botDiffs.get(dIdx);    // reads from config based on the chosen index
            gc.setBotDifficulty(difficulty);
        }

        for (String name : names) {
            gc.addPlayerName(name);             // SD4
        }

        gc.confirmGameSetup();                  // SD5

        showSetupSummary(numHumans, numBots, difficulty);
        return true;
    }


    // ── 4. Summary Dialog ─────────────────────────────────────────

    private void showSetupSummary(int numHumans, int numBots, String difficulty) {
        StringBuilder sb = new StringBuilder("Game ready!\n\n");
        sb.append("Mode: ").append(gc.getGame().getMode()).append("\n");
        sb.append("Players: ").append(numHumans).append(" human(s), ")
                .append(numBots).append(" bot(s)");

        if (numBots > 0) {
            sb.append(" (").append(difficulty).append(")");
        }

        sb.append("\n\nTurn order:\n");
        gc.getGame().getPlayers().forEach(p ->
                sb.append("  ").append(p.getDisplayIcon())
                        .append(" ").append(p).append("\n"));

        JOptionPane.showMessageDialog(null, sb.toString(),
                "Setup Complete", JOptionPane.INFORMATION_MESSAGE);
    }


    // ── 5. Custom UI Builders ─────────────────────────────────────

    /**
     * UI Helper — dialog with options + "← Back" + "Cancel".
     * Returns: 0+ = chosen option, -1 = Back, -2 = Cancel/Closed
     */
    private int showWithBack(String message, String title, String[] options, int defaultIdx) {
        final int[] result = {-2};
        Font paneFont = UIManager.getFont("OptionPane.messageFont");
        Font btnFont  = UIManager.getFont("OptionPane.buttonFont");

        JDialog dlg = new JDialog((Frame) null, title, true);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setLayout(new BorderLayout(10, 8));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBorder(BorderFactory.createEmptyBorder(15, 15, 8, 15));
        JLabel icon = new JLabel(UIManager.getIcon("OptionPane.questionIcon"));
        JLabel msg  = new JLabel("<html><body style='width:260px'>" + message + "</body></html>");
        if (paneFont != null) msg.setFont(paneFont);

        top.add(icon, BorderLayout.WEST);
        top.add(msg,  BorderLayout.CENTER);
        dlg.add(top, BorderLayout.NORTH);

        JPanel middle = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        middle.setBorder(BorderFactory.createEmptyBorder(0, 15, 4, 15));
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            JButton btn = new JButton(options[i]);
            if (btnFont != null) btn.setFont(btnFont);
            btn.addActionListener(e -> { result[0] = idx; dlg.dispose(); });
            if (i == defaultIdx) dlg.getRootPane().setDefaultButton(btn);
            middle.add(btn);
        }
        dlg.add(middle, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        JButton backBtn   = new JButton("← Back");
        JButton cancelBtn = new JButton("Cancel");
        if (btnFont != null) backBtn.setFont(btnFont);
        if (btnFont != null) cancelBtn.setFont(btnFont);

        backBtn.addActionListener(e   -> { result[0] = -1; dlg.dispose(); });
        cancelBtn.addActionListener(e -> { result[0] = -2; dlg.dispose(); });

        bottom.add(backBtn);
        bottom.add(cancelBtn);
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.pack();
        dlg.setMinimumSize(new Dimension(380, 0));
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
        return result[0];
    }

    /**
     * UI Helper — name input dialog.
     * Returns: 0 = OK, -1 = Back, -2 = Cancel/Closed
     */
    private int showNameInput(String title, String message, String[] nameOut) {
        final int[] result = {-2};
        Font paneFont = UIManager.getFont("OptionPane.messageFont");
        Font btnFont  = UIManager.getFont("OptionPane.buttonFont");

        JDialog dlg = new JDialog((Frame) null, title, true);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setLayout(new BorderLayout(10, 8));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBorder(BorderFactory.createEmptyBorder(15, 15, 8, 15));
        JLabel icon = new JLabel(UIManager.getIcon("OptionPane.questionIcon"));
        JLabel lbl  = new JLabel(message);
        if (paneFont != null) lbl.setFont(paneFont);

        top.add(icon, BorderLayout.WEST);
        top.add(lbl,  BorderLayout.CENTER);
        dlg.add(top, BorderLayout.NORTH);

        JTextField field = new JTextField(16);
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        center.add(field);
        dlg.add(center, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        JPanel okRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        JButton okBtn = new JButton("OK");
        if (btnFont != null) okBtn.setFont(btnFont);
        okBtn.addActionListener(e -> {
            nameOut[0] = field.getText();
            result[0]  = 0;
            dlg.dispose();
        });
        dlg.getRootPane().setDefaultButton(okBtn);
        okRow.add(okBtn);
        south.add(okRow);

        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        navRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        JButton backBtn   = new JButton("← Back");
        JButton cancelBtn = new JButton("Cancel");
        if (btnFont != null) backBtn.setFont(btnFont);
        if (btnFont != null) cancelBtn.setFont(btnFont);

        backBtn.addActionListener(e   -> { result[0] = -1; dlg.dispose(); });
        cancelBtn.addActionListener(e -> { result[0] = -2; dlg.dispose(); });

        navRow.add(backBtn);
        navRow.add(cancelBtn);
        south.add(navRow);

        dlg.add(south, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(320, 0));
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
        return result[0];
    }
}
