package it.univaq.caffeine.catan.view;

import it.univaq.caffeine.catan.controller.GameController;
import it.univaq.caffeine.catan.model.Bank;
import it.univaq.caffeine.catan.model.board.Port;
import it.univaq.caffeine.catan.model.cards.CardPlayContext;
import it.univaq.caffeine.catan.model.cards.Deck;
import it.univaq.caffeine.catan.model.cards.DevelopmentCard;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.ResourceType;
import it.univaq.caffeine.catan.model.player.Player;
import it.univaq.caffeine.catan.model.cards.ResourceCard;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application window for The Settlers of Catan.
 * Manages the main layout, the right-side control panel, dialogs,
 * and delegates board rendering to CatanBoardPanel.
 */
public class GameWindow extends JFrame {

    // ── 1. Constants & Palette ────────────────────────────────────

    private static final Color BG        = new Color(245, 235, 210);
    private static final Color SECTION   = new Color(100, 60, 20);
    private static final Color CARD_BLUE = new Color(60, 90, 200);
    private static final Color CARD_GREY = new Color(100, 100, 110);
    private static final Color CARD_ORG  = new Color(210, 120, 20);

    private static final Color COL_ROLL      = new Color(100, 160, 230);
    private static final Color COL_COLONY    = new Color(80,  160,  80);
    private static final Color COL_CITY      = new Color(60,  130,  60);
    private static final Color COL_STREET    = new Color(180, 140,  60);
    private static final Color COL_CARD      = new Color(150,  80, 180);
    private static final Color COL_END       = new Color(200,  80,  80);
    private static final Color COL_DIMMED    = new Color(150, 150, 150);
    private static final Color COL_PLAY_CARD = new Color(160,  50, 130);


    // ── 2. Instance Variables ─────────────────────────────────────

    // Core references
    private final GameController  gc;
    private final CatanBoardPanel boardPanel;

    // Dynamic labels & panels
    private JLabel    lblPlayer;
    private JLabel    lblTurn;
    private JTextArea txtInstruction;
    private JPanel    resPanel;
    private JPanel    devCardPanel;
    private JLabel    lblDeck;
    private JTextArea txtPlayerList;
    private JLabel    lblLongestRoad;
    private JLabel    lblLargestArmy;

    // Action buttons
    private JButton btnRollDice;
    private JButton btnBuildColony;
    private JButton btnBuildCity;
    private JButton btnBuildStreet;
    private JButton btnBuyCard;
    private JButton btnEndTurn;
    private JButton btnPlayCard;


    // ── 3. Constructor ────────────────────────────────────────────

    public GameWindow(GameController gc) {
        this.gc = gc;
        this.boardPanel = new CatanBoardPanel(gc, this);

        setTitle("The Settlers of Catan");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        add(boardPanel, BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);

        updateStatus();
        pack();
        setLocationRelativeTo(null);
    }


    // ── 4. UI Initialization & Builders ───────────────────────────

    private JPanel buildRightPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.setPreferredSize(new Dimension(295, 780));

        // Current player
        p.add(sectionLabel("▶ Current Turn"));
        lblPlayer = boldLabel("—", 15);
        lblTurn   = plainLabel("—", 11);
        p.add(Box.createVerticalStrut(3));
        p.add(lblPlayer);
        p.add(lblTurn);
        p.add(Box.createVerticalStrut(8));
        p.add(hLine());

        // Phase instruction
        p.add(Box.createVerticalStrut(6));
        p.add(sectionLabel("📋 Phase"));
        txtInstruction = new JTextArea(4, 26);
        txtInstruction.setEditable(false);
        txtInstruction.setLineWrap(true);
        txtInstruction.setWrapStyleWord(true);
        txtInstruction.setFont(new Font("SansSerif", Font.PLAIN, 11));
        txtInstruction.setBackground(new Color(255, 252, 240));
        txtInstruction.setBorder(new CompoundBorder(
                new LineBorder(new Color(180, 140, 80), 1, true),
                new EmptyBorder(5, 6, 5, 6)));
        p.add(Box.createVerticalStrut(3));
        p.add(txtInstruction);
        p.add(Box.createVerticalStrut(8));
        p.add(hLine());

        // Resources
        p.add(Box.createVerticalStrut(6));
        p.add(sectionLabel("💰 Resources (current player)"));
        resPanel = new JPanel(new GridLayout(1, 5, 4, 0));
        resPanel.setBackground(BG);
        resPanel.setMaximumSize(new Dimension(275, 54));
        p.add(Box.createVerticalStrut(3));
        p.add(resPanel);
        p.add(Box.createVerticalStrut(8));
        p.add(hLine());

        // Dev cards in hand
        p.add(Box.createVerticalStrut(6));
        p.add(sectionLabel("📜 Development Cards in Hand"));
        devCardPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2));
        devCardPanel.setBackground(BG);
        devCardPanel.setPreferredSize(new Dimension(275, 60));
        devCardPanel.setMaximumSize(new Dimension(275, 60));
        p.add(Box.createVerticalStrut(3));
        p.add(devCardPanel);
        p.add(Box.createVerticalStrut(8));
        p.add(hLine());

        // Bonus cards
        p.add(Box.createVerticalStrut(8));
        p.add(hLine());
        p.add(Box.createVerticalStrut(6));
        p.add(sectionLabel("🏅 Bonus Cards"));

        lblLongestRoad = plainLabel("🛤 Longest Road:  —", 11);
        lblLargestArmy = plainLabel("⚔ Largest Army:  —", 11);
        p.add(Box.createVerticalStrut(3));
        p.add(lblLongestRoad);
        p.add(Box.createVerticalStrut(2));
        p.add(lblLargestArmy);

        // Deck status
        p.add(Box.createVerticalStrut(6));
        p.add(sectionLabel("🃏 Development Deck"));
        lblDeck = plainLabel("—", 11);
        p.add(Box.createVerticalStrut(2));
        p.add(lblDeck);
        p.add(Box.createVerticalStrut(4));
        p.add(buildDeckLegend());
        p.add(Box.createVerticalStrut(8));
        p.add(hLine());

        // Action buttons (UC 3.0)
        p.add(Box.createVerticalStrut(6));
        p.add(sectionLabel("⚡ Actions"));
        p.add(Box.createVerticalStrut(3));
        p.add(buildActionPanel());
        p.add(Box.createVerticalStrut(8));
        p.add(hLine());

        // Bank button
        p.add(Box.createVerticalStrut(6));
        JButton bankBtn = new JButton("🏦  Trade with Bank");
        bankBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        bankBtn.setBackground(new Color(200, 230, 200));
        bankBtn.setFocusPainted(false);
        bankBtn.setMaximumSize(new Dimension(275, 34));
        bankBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        bankBtn.addActionListener(e -> openBankDialog());
        p.add(bankBtn);
        p.add(Box.createVerticalStrut(8));
        p.add(hLine());

        // Player list
        p.add(Box.createVerticalStrut(6));
        p.add(sectionLabel("👥 Players"));
        txtPlayerList = new JTextArea(6, 26);
        txtPlayerList.setEditable(false);
        txtPlayerList.setFont(new Font("Monospaced", Font.PLAIN, 10));
        txtPlayerList.setBackground(new Color(250, 248, 240));
        txtPlayerList.setLineWrap(true);
        txtPlayerList.setWrapStyleWord(true);
        txtPlayerList.setBorder(new EmptyBorder(4, 4, 4, 4));
        JScrollPane sp = new JScrollPane(txtPlayerList);
        sp.setBorder(new LineBorder(new Color(180, 140, 80)));
        sp.setMaximumSize(new Dimension(275, 120));
        p.add(Box.createVerticalStrut(3));
        p.add(sp);

        p.add(Box.createVerticalGlue());
        JLabel ver = new JLabel("Caffeine Coders");
        ver.setFont(new Font("SansSerif", Font.ITALIC, 9));
        ver.setForeground(Color.GRAY);
        p.add(ver);

        return p;
    }

    private JPanel buildActionPanel() {
        JPanel p = new JPanel(new GridLayout(4, 2, 4, 4));  // 4 rows, 2 columns
        p.setBackground(BG);
        p.setMaximumSize(new Dimension(275, 155));  // Fixed height for 4 rows

        // All buttons initially disabled (COL_DIMMED) — setBtn enables them in updateStatus()
        btnRollDice    = actionButton("🎲 Roll Dice");
        btnEndTurn     = actionButton("⏭ End Turn");
        btnBuildColony = actionButton("🏠 Build Colony");
        btnBuildCity   = actionButton("🏙 Build City");
        btnBuildStreet = actionButton("🛤 Build Street");
        btnBuyCard     = actionButton("🃏 Buy Card");
        btnPlayCard    = actionButton("🎴 Play Card");

        btnPlayCard.addActionListener(e -> handlePlayCard());

        addHoverEffect(btnRollDice,    COL_ROLL);
        addHoverEffect(btnEndTurn,     COL_END);
        addHoverEffect(btnBuildColony, COL_COLONY);
        addHoverEffect(btnBuildCity,   COL_CITY);
        addHoverEffect(btnBuildStreet, COL_STREET);
        addHoverEffect(btnBuyCard,     COL_CARD);
        addHoverEffect(btnPlayCard,    COL_PLAY_CARD);

        btnRollDice.addActionListener(e    -> handleRollDice());
        btnBuildColony.addActionListener(e -> handleBeginBuild(BuildingType.COLONY, CatanBoardPanel.BoardInteractionMode.BUILD_COLONY));
        btnBuildCity.addActionListener(e   -> handleBeginBuild(BuildingType.CITY, CatanBoardPanel.BoardInteractionMode.BUILD_CITY));
        btnBuildStreet.addActionListener(e -> handleBeginBuild(BuildingType.STREET, CatanBoardPanel.BoardInteractionMode.BUILD_STREET));
        btnBuyCard.addActionListener(e     -> handleBuyCard());
        btnEndTurn.addActionListener(e     -> handleEndTurn());

        p.add(btnRollDice);    p.add(btnEndTurn);
        p.add(btnBuildColony); p.add(btnBuildCity);
        p.add(btnBuildStreet); p.add(btnBuyCard);
        p.add(btnPlayCard);    p.add(new JLabel()); // Empty cell to balance the grid

        return p;
    }

    private JPanel buildDeckLegend() {
        JPanel p = new JPanel(new GridLayout(3, 1, 0, 2));
        p.setBackground(BG);
        p.setMaximumSize(new Dimension(275, 54));

        Map<String, Integer> comp = gc.getGame().getConfig().getDeckComposition();
        int knights  = comp.getOrDefault("KNIGHT", 0);
        int progress = comp.getOrDefault("MONOPOLY", 0)
                + comp.getOrDefault("DISCOVERY", 0)
                + comp.getOrDefault("ROAD_BUILDING", 0);
        int vp       = comp.getOrDefault("TOWN_HALL", 0)
                + comp.getOrDefault("MARKET_SQUARE", 0)
                + comp.getOrDefault("CATHEDRAL", 0)
                + comp.getOrDefault("LIBRARY", 0)
                + comp.getOrDefault("UNIVERSITY", 0);

        p.add(deckTag("Knight ×" + knights, CARD_BLUE));
        p.add(deckTag("Progress ×" + progress + " (Monopoly·Discovery·Road Building)", CARD_GREY));
        p.add(deckTag("Victory Points ×" + vp + " (Town Hall·Market·…)", CARD_ORG));
        return p;
    }


    // ── 5. Status & UI Updates ────────────────────────────────────

    public void updateStatus() {
        if (gc.getGame() == null) return;

        Player cur = gc.getGame().getCurrentPlayer();
        CatanBoardPanel.BoardInteractionMode interactionMode = boardPanel.getInteractionMode();

        boolean isTurnPhase = gc.getGame().isTurnPhase();
        boolean canTakeTurnActions = gc.getGame().isActivePhase();
        boolean canChangeBuildSelection = gc.getGame().isBuildPhase()
                && gc.getGame().getFreeRoadsRemaining() == 0;
        boolean rolled   = isTurnPhase && gc.getGame().isDiceRolled();
        boolean isHuman  = cur != null && cur.isHuman();
        boolean hasPlayableCards = cur != null && cur.getCards().stream()
                .anyMatch(c -> c.canBePlayed(gc.getGame(), cur));

        setBtn(btnPlayCard, canTakeTurnActions && rolled && isHuman && hasPlayableCards, COL_PLAY_CARD);

        // Current Player logic
        if (cur != null) {
            lblPlayer.setForeground(CatanBoardPanel.playerColor(cur.getColor()));
            lblPlayer.setText("  " + cur.getName() + cur.getTurnDisplaySuffix());
            if (isTurnPhase) {
                lblTurn.setText("  Color: " + cur.getColor()
                        + "  ·  Round " + (gc.getGame().getCurrentShift() + 1));
            } else {
                lblTurn.setText("  Color: " + cur.getColor()
                        + " · Turn " + (gc.getGame().getCurrentShift() + 1)
                        + "/" + (gc.getGame().getMaxPlayers() * 2));
            }
        }

        // UI instructions update
        switch (interactionMode) {
            case INITIAL_COLONY    -> txtInstruction.setText(
                    "Initial Placement — COLONY\n\n" +
                            "Click on an intersection to place your settlement.\n" +
                            "Rule: no adjacent settlements.");
            case INITIAL_ROAD      -> txtInstruction.setText(
                    "Initial Placement — ROAD\n\n" +
                            "Click on an edge adjacent to the settlement you just placed.");
            case TURN_ACTIONS     -> txtInstruction.setText(!rolled
                    ? "Your turn!\n\nPress 🎲 Roll Dice to start your turn."
                    : "Dice: " + gc.getGame().getLastDiceResult()
                    + "\n\nBuild, buy cards or end your turn.");
            case BUILD_COLONY -> txtInstruction.setText(
                    "Building Colony\n\nClick an intersection connected to your road network.");
            case BUILD_CITY   -> txtInstruction.setText(
                    "Building City\n\nClick one of your existing colonies to upgrade it.");
            case BUILD_STREET -> {
                int free = gc.getGame().getFreeRoadsRemaining();
                if (free > 0) {
                    txtInstruction.setText(
                            "🛤 Road Building — road " + (3 - free) + " of 2\n\n" +
                                    "Click an edge connected to your road network or settlement.\n" +
                                    free + " free road(s) remaining.");
                } else {
                    txtInstruction.setText(
                            "Building Street\n\n" +
                                    "Click an edge connected to your road network or settlement.");
                }
            }
            case MOVE_ROBBER -> txtInstruction.setText(
                    "🎲 Rolled 7 — Robber!\n\nClick on a tile to place the Robber.\nThen steal from an adjacent player.");
            default -> {}
        }

        // Resources update
        updateResourcePanel(cur);
        updateDevCardPanel(cur);

        // Deck update
        Deck deck = gc.getGame().getDeck();
        if (deck != null) {
            lblDeck.setText("  Cards remaining: " + deck.size() + " / " + deck.getTotalSize());
        }

        // Player List update
        StringBuilder sb = new StringBuilder();
        for (Player pl : gc.getGame().getPlayers()) {
            sb.append(pl.getDisplayIcon()).append(" ")
                    .append(pl.getName());

            if (pl.equals(gc.getLongestRoadOwner())) sb.append(" 🛤");
            if (pl.equals(gc.getLargestArmyOwner())) sb.append(" ⚔");

            sb.append("\n   VP:").append(pl.getVictoryPoints())
                    .append("  Resources:").append(pl.getResources().size())
                    .append("  Buildings:").append(pl.getBuildings().size())
                    .append("\n");
        }

        txtPlayerList.setText(sb.toString());
        txtPlayerList.revalidate();
        txtPlayerList.repaint();

        // Action Buttons state update
        if (btnRollDice != null) {
            boolean notMovingRobber = interactionMode != CatanBoardPanel.BoardInteractionMode.MOVE_ROBBER;
            boolean canChooseBuild = (canTakeTurnActions || canChangeBuildSelection)
                    && rolled && isHuman && notMovingRobber;
            boolean canEndTurn = (canTakeTurnActions || canChangeBuildSelection)
                    && rolled && isHuman && notMovingRobber;

            setBtn(btnRollDice,    canTakeTurnActions && !rolled && isHuman && notMovingRobber, COL_ROLL);
            setBtn(btnBuildColony, canChooseBuild,                                           COL_COLONY);
            setBtn(btnBuildCity,   canChooseBuild,                                           COL_CITY);
            setBtn(btnBuildStreet, canChooseBuild,                                           COL_STREET);
            setBtn(btnBuyCard,     canTakeTurnActions && rolled && isHuman && notMovingRobber
                    && deck != null && !deck.isEmpty(),                  COL_CARD);
            setBtn(btnEndTurn,     canEndTurn,                                               COL_END);
        }

        // Bonus Cards status update
        if (lblLongestRoad != null) {
            Player lrOwner = gc.getLongestRoadOwner();
            if (lrOwner != null) {
                lblLongestRoad.setText("🛤 Longest Road:  " + lrOwner.getName());
                lblLongestRoad.setForeground(CatanBoardPanel.playerColor(lrOwner.getColor()));
            } else {
                lblLongestRoad.setText("🛤 Longest Road:  —");
                lblLongestRoad.setForeground(Color.DARK_GRAY);
            }

            Player laOwner = gc.getLargestArmyOwner();
            if (laOwner != null) {
                lblLargestArmy.setText("⚔ Largest Army:  " + laOwner.getName());
                lblLargestArmy.setForeground(CatanBoardPanel.playerColor(laOwner.getColor()));
            } else {
                lblLargestArmy.setText("⚔ Largest Army:  —");
                lblLargestArmy.setForeground(Color.DARK_GRAY);
            }
        }
    }

    private void updateResourcePanel(Player cur) {
        resPanel.removeAll();
        ResourceType[] types = {
                ResourceType.BRICK, ResourceType.WOOD,
                ResourceType.WHEAT, ResourceType.SHEEP, ResourceType.ORE
        };
        String[] icons = {"🧱", "🌲", "🌾", "🐑", "⛏"};
        String[] names = {"BRI", "WOO", "WHE", "SHE", "ORE"};

        for (int i = 0; i < types.length; i++) {
            final ResourceType rt = types[i];
            int count = cur == null ? 0 :
                    (int) cur.getResources().stream().filter(rc -> rc.getType() == rt).count();

            JPanel cell = new JPanel();
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
            cell.setBackground(CatanBoardPanel.terrainColor(types[i]).darker());
            cell.setBorder(new LineBorder(Color.BLACK, 1, true));

            JLabel ico = new JLabel(icons[i], SwingConstants.CENTER);
            ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            ico.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lbl = new JLabel(names[i], SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 8));
            lbl.setForeground(Color.WHITE);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel cnt = new JLabel(String.valueOf(count), SwingConstants.CENTER);
            cnt.setFont(new Font("SansSerif", Font.BOLD, 16));
            cnt.setForeground(Color.WHITE);
            cnt.setAlignmentX(Component.CENTER_ALIGNMENT);

            cell.add(ico); cell.add(lbl); cell.add(cnt);
            resPanel.add(cell);
        }
        resPanel.revalidate();
        resPanel.repaint();
    }

    private void updateDevCardPanel(Player cur) {
        devCardPanel.removeAll();
        if (cur == null || cur.getCards().isEmpty()) {
            JLabel none = new JLabel("No cards");
            none.setFont(new Font("SansSerif", Font.ITALIC, 10));
            none.setForeground(Color.GRAY);
            devCardPanel.add(none);
        } else {
            java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
            for (DevelopmentCard c : cur.getCards()) {
                if (!c.isPlayed()) {
                    counts.merge(c.getDisplayName().replace("\n", " "), 1L, Long::sum);
                }
            }
            for (var entry : counts.entrySet()) {
                JLabel tag = new JLabel(entry.getKey() + " ×" + entry.getValue());
                tag.setFont(new Font("SansSerif", Font.BOLD, 9));
                tag.setForeground(Color.WHITE);
                tag.setBackground(CARD_BLUE);
                tag.setOpaque(true);
                tag.setBorder(new EmptyBorder(2, 4, 2, 4));
                devCardPanel.add(tag);
            }
        }
        devCardPanel.revalidate();
        devCardPanel.repaint();
    }


    // ── 6. Event Handlers & Dialogs ───────────────────────────────

    private void handleBeginBuild(BuildingType type, CatanBoardPanel.BoardInteractionMode mode) {
        try {
            gc.beginBuildPhase(type);
            boardPanel.setInteractionMode(mode);
            updateStatus();
            boardPanel.repaint();
        } catch (Exception ex) {
            showActionError(ex.getMessage());
        }
    }

    private void handleRollDice() {
        try {
            int result = gc.rollDice();
            JOptionPane.showMessageDialog(this,
                    "🎲 Dice result: " + result +
                            (result == 7 ? "\n⚠ Robber activated!" : "\n✅ Resources distributed!"),
                    "Dice Roll", JOptionPane.INFORMATION_MESSAGE);

            if (result == 7) {
                // 1. Human discards
                Map<Player, Integer> discards = gc.activateRobber();
                discards.forEach(this::showDiscardDialog);

                // 2. Move robber interactionMode
                boardPanel.setInteractionMode(CatanBoardPanel.BoardInteractionMode.MOVE_ROBBER);
            }

            updateStatus();
            boardPanel.repaint();
        } catch (Exception ex) {
            showActionError(ex.getMessage());
        }
    }

    public void showDiscardDialog(Player p, int toDiscard) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 4));
        panel.add(new JLabel("<html><b>" + p.getName() + "</b> must discard "
                + toDiscard + " cards:</html>"));
        panel.add(new JLabel(""));

        ResourceType[] types = {
                ResourceType.BRICK, ResourceType.WOOD,
                ResourceType.WHEAT, ResourceType.SHEEP, ResourceType.ORE
        };
        JSpinner[] spinners = new JSpinner[types.length];

        for (int i = 0; i < types.length; i++) {
            final ResourceType rt = types[i];
            int has = (int) p.getResources().stream()
                    .filter(r -> r.getType() == rt).count();
            panel.add(new JLabel(rt.name() + "  (" + has + ")"));
            spinners[i] = new JSpinner(new SpinnerNumberModel(0, 0, has, 1));
            panel.add(spinners[i]);
        }

        while (true) {
            int res = JOptionPane.showConfirmDialog(this, panel,
                    "Discard " + toDiscard + " cards", JOptionPane.OK_CANCEL_OPTION);

            if (res != JOptionPane.OK_OPTION) break;

            int total = 0;
            for (JSpinner s : spinners) {
                if (s != null) total += (int) s.getValue();
            }

            if (total != toDiscard) {
                JOptionPane.showMessageDialog(this,
                        "You must discard exactly " + toDiscard + " cards (selected: " + total + ").",
                        "Wrong count", JOptionPane.WARNING_MESSAGE);
            } else {
                Map<ResourceType, Integer> toDiscardMap = new LinkedHashMap<>();
                for (int i = 0; i < types.length; i++) {
                    if (spinners[i] != null && (int) spinners[i].getValue() > 0) {
                        toDiscardMap.put(types[i], (int) spinners[i].getValue());
                    }
                }
                gc.discardResources(p, toDiscardMap);
                break;
            }
        }
        updateStatus();
    }

    private void handleBuyCard() {
        try {
            DevelopmentCard card = gc.buyDevelopmentCard();
            JOptionPane.showMessageDialog(this,
                    "🃏 You drew: " + card.getDisplayName(),
                    "Development Card", JOptionPane.INFORMATION_MESSAGE);

            updateStatus();
            checkAndHandleVictory();
        } catch (Exception ex) {
            showActionError(ex.getMessage());
        }
    }

    public void handleRobberSteal() {
        try {
            List<Player> victims = gc.getStealablePlayers();
            if (victims.isEmpty()) {
                // No adjacent players — no stealing
                JOptionPane.showMessageDialog(this,
                        "No adjacent players to steal from.",
                        "Robber", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Random victim — no selection dialog
            Player victim = victims.get(new java.util.Random().nextInt(victims.size()));
            ResourceCard stolen = gc.stealFrom(victim);

            // Notification without revealing which resource was stolen
            if (stolen != null) {
                JOptionPane.showMessageDialog(this,
                        "You stole 1 resource card from " + victim.getName() + ".",
                        "Robber Steal", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        victim.getName() + " has no cards to steal.",
                        "Robber Steal", JOptionPane.INFORMATION_MESSAGE);
            }
        } finally {
            if (gc.getGame().isRobberPhase()) {
                gc.finishRobberPhase();
            }
            updateStatus();
        }
    }

    private void handleEndTurn() {
        try {
            if (gc.getGame().isBuildPhase()
                    && gc.getGame().getFreeRoadsRemaining() == 0) {
                gc.cancelBuildPhase();
            }
            gc.endTurn();
            boardPanel.setInteractionMode(CatanBoardPanel.BoardInteractionMode.TURN_ACTIONS);
            updateStatus();
            boardPanel.repaint();

            if (gc.getGame().isGameOver()) {
                Player winner = gc.getWinner();
                JOptionPane.showMessageDialog(this,
                        "🏆 " + (winner != null ? winner.getName() : "?") + " wins!",
                        "Game Over", JOptionPane.INFORMATION_MESSAGE);
            } else {
                Player next = gc.getGame().getCurrentPlayer();
                // Separator only if the next player is HUMAN
                if (next != null && next.isHuman()) {
                    showTurnSeparator(next);
                }
                boardPanel.checkAndScheduleBotMove();
            }
        } catch (Exception ex) {
            showActionError(ex.getMessage());
        }
    }

    private void showActionError(String msg) {
        Object display = msg != null && msg.startsWith("<html>") ? new JLabel(msg) : msg;
        JOptionPane.showMessageDialog(this, display, "Action Error", JOptionPane.WARNING_MESSAGE);
    }

    private void openBankDialog() {
        JDialog dlg = new JDialog(this, "🏦 Bank — Resources", true);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(12, 16, 12, 16));
        content.setBackground(new Color(245, 255, 245));

        Player currentPlayer = gc.getGame().getCurrentPlayer();
        boolean canTrade = gc.getGame().isActivePhase()
                && gc.getGame().isDiceRolled()
                && currentPlayer != null && !currentPlayer.isBot();
        final boolean[] tradePhaseOpen = {false};
        if (canTrade) {
            try {
                gc.beginTradePhase();
                tradePhaseOpen[0] = true;
            } catch (Exception ex) {
                canTrade = false;
                showActionError(ex.getMessage());
            }
        }

        // Informational Note
        int defaultRate = gc.getGame().getConfig().getBankDefaultRate();
        JLabel note = new JLabel("<html><body style='width:280px'>"
                + (canTrade
                ? "<b>✅ Trading available!</b><br>Select a resource to give and one to receive."
                : "<b>ℹ️ Available after rolling dice.</b><br>Default rate: <b>"
                + defaultRate + ":1</b> (or <b>3:1/2:1</b> with a port).")
                + "</body></html>");
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));
        content.add(note);
        content.add(Box.createVerticalStrut(10));

        // Bank Stock
        Bank bank = gc.getGame().getBank();
        if (bank != null) {
            content.add(boldLabel("Current stock:", 12));
            content.add(Box.createVerticalStrut(4));

            JPanel grid = new JPanel(new GridLayout(1, 5, 6, 0));
            grid.setBackground(new Color(245, 255, 245));
            ResourceType[] types = {
                    ResourceType.BRICK, ResourceType.WOOD,
                    ResourceType.WHEAT, ResourceType.SHEEP, ResourceType.ORE
            };
            String[] names = {"Brick", "Wood", "Wheat", "Sheep", "Ore"};

            for (int i = 0; i < types.length; i++) {
                int qty = bank.getResourceStock().getOrDefault(types[i], 0);
                JPanel cell = new JPanel();
                cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
                cell.setBackground(CatanBoardPanel.terrainColor(types[i]).darker());
                cell.setBorder(new LineBorder(Color.BLACK, 1, true));

                JLabel n = new JLabel(names[i], SwingConstants.CENTER);
                n.setFont(new Font("SansSerif", Font.PLAIN, 9));
                n.setForeground(Color.WHITE);
                n.setAlignmentX(Component.CENTER_ALIGNMENT);

                JLabel q = new JLabel(String.valueOf(qty), SwingConstants.CENTER);
                q.setFont(new Font("SansSerif", Font.BOLD, 18));
                q.setForeground(Color.WHITE);
                q.setAlignmentX(Component.CENTER_ALIGNMENT);

                cell.add(n);
                cell.add(q);
                grid.add(cell);
            }
            content.add(grid);
        }

        content.add(Box.createVerticalStrut(10));

        // Ports Info
        List<Port> ports = gc.getGame().getMap().getPorts();
        content.add(boldLabel("Ports on the board (" + ports.size() + " total):", 12));
        content.add(Box.createVerticalStrut(4));
        long generic = ports.stream().filter(Port::isGeneric).count();

        StringBuilder portText = new StringBuilder();
        portText.append(generic).append(" × Generic port    → 3:1 (any resource)\n");
        ports.stream()
                .filter(port -> !port.isGeneric())
                .forEach(port -> portText
                        .append("1 × ").append(port.getResourceType().name())
                        .append(" port        → ")
                        .append(port.getExchangeRate()).append(":1 ")
                        .append(port.getResourceType().name()).append("\n"));

        JTextArea portInfo = new JTextArea(portText.toString());
        portInfo.setEditable(false);
        portInfo.setFont(new Font("Monospaced", Font.PLAIN, 11));
        portInfo.setBackground(new Color(245, 255, 245));
        portInfo.setBorder(new EmptyBorder(4, 4, 4, 4));
        content.add(portInfo);

        // Trading Section (only if dice rolled)
        if (canTrade) {
            content.add(Box.createVerticalStrut(10));
            content.add(hLine());
            content.add(Box.createVerticalStrut(8));
            content.add(boldLabel("Trade:", 12));
            content.add(Box.createVerticalStrut(6));

            ResourceType[] types = {
                    ResourceType.BRICK, ResourceType.WOOD,
                    ResourceType.WHEAT, ResourceType.SHEEP, ResourceType.ORE
            };
            Map<ResourceType, Integer> rates = gc.getAllTradeRates();

            // "Give" ComboBox
            JPanel giveRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            giveRow.setBackground(new Color(245, 255, 245));
            giveRow.add(new JLabel("Give:"));
            JComboBox<String> giveCombo = new JComboBox<>();

            for (ResourceType rt : types) {
                int has = (int) gc.getGame().getCurrentPlayer().getResources()
                        .stream().filter(r -> r.getType() == rt).count();
                giveCombo.addItem(rt.name() + "  (have " + has + "  →  " + rates.get(rt) + ":1)");
            }
            giveRow.add(giveCombo);
            content.add(giveRow);

            // Dynamic rate label
            JLabel rateLabel = new JLabel();
            rateLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
            rateLabel.setForeground(new Color(60, 120, 200));
            rateLabel.setBorder(new EmptyBorder(0, 6, 0, 0));
            content.add(rateLabel);

            // "Receive" ComboBox
            JPanel recvRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            recvRow.setBackground(new Color(245, 255, 245));
            recvRow.add(new JLabel("Receive:"));
            JComboBox<String> receiveCombo = new JComboBox<>();
            for (ResourceType rt : types) receiveCombo.addItem(rt.name());
            recvRow.add(receiveCombo);
            content.add(recvRow);

            // Update label on selection change
            Runnable updateRate = () -> {
                int idx = giveCombo.getSelectedIndex();
                if (idx >= 0) {
                    ResourceType give = types[idx];
                    int rate = rates.get(give);
                    int has  = (int) gc.getGame().getCurrentPlayer().getResources()
                            .stream().filter(r -> r.getType() == give).count();
                    rateLabel.setText("  Give " + rate + "x " + give.name()
                            + " (you have " + has + ") → receive 1 card");
                }
            };
            giveCombo.addActionListener(e -> updateRate.run());
            updateRate.run();

            // Trade Button
            content.add(Box.createVerticalStrut(8));
            JButton tradeBtn = new JButton("✅  Execute Trade");
            tradeBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
            tradeBtn.setBackground(new Color(80, 160, 80));
            tradeBtn.setForeground(Color.WHITE);
            tradeBtn.setFocusPainted(false);
            tradeBtn.setOpaque(true);
            tradeBtn.setBorderPainted(false);
            tradeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

            tradeBtn.addActionListener(e -> {
                int gi = giveCombo.getSelectedIndex();
                int ri = receiveCombo.getSelectedIndex();
                if (gi < 0 || ri < 0) return;
                try {
                    gc.bankTrade(types[gi], types[ri]);
                    JOptionPane.showMessageDialog(dlg,
                            "Trade complete!\nGave " + rates.get(types[gi]) + "x "
                                    + types[gi].name() + "  →  received 1x " + types[ri].name(),
                            "Trade", JOptionPane.INFORMATION_MESSAGE);
                    closeTradeDialog(dlg, tradePhaseOpen);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, ex.getMessage(),
                            "Trade Error", JOptionPane.WARNING_MESSAGE);
                }
            });
            content.add(tradeBtn);
        }

        JButton close = new JButton("Close");
        close.addActionListener(e -> closeTradeDialog(dlg, tradePhaseOpen));
        dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeTradeDialog(dlg, tradePhaseOpen);
            }
        });

        dlg.add(new JScrollPane(content), BorderLayout.CENTER);
        dlg.add(close, BorderLayout.SOUTH);
        dlg.setSize(360, canTrade ? 580 : 420);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void closeTradeDialog(JDialog dlg, boolean[] tradePhaseOpen) {
        if (tradePhaseOpen[0] && gc.getGame().isTradePhase()) {
            try {
                gc.finishTradePhase();
            } catch (Exception ex) {
                showActionError(ex.getMessage());
            }
        }
        tradePhaseOpen[0] = false;
        updateStatus();
        boardPanel.repaint();
        dlg.dispose();
    }

    /**
     * Shows a dialog marking the start of a player's turn.
     * Pauses the game until the player clicks OK.
     */
    public void showTurnSeparator(Player p) {
        String icon = p.getDisplayIcon();
        JOptionPane.showMessageDialog(this,
                icon + "  " + p.getName() + "'s turn begins.\n\nClick OK to continue.",
                "Next Turn", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Checks if the game is over and shows the winner dialog.
     * Called after any action that could change VP.
     */
    public void checkAndHandleVictory() {
        if (!gc.getGame().isGameOver()) return;

        Player winner = gc.getWinner();
        if (winner == null) return;

        // Public VP
        int publicVP = winner.getVictoryPoints();

        // Hidden VP Cards
        long vpCards = winner.getCards().stream()
                .filter(DevelopmentCard::isVictoryPoint).count();

        String vpCardList = winner.getCards().stream()
                .filter(DevelopmentCard::isVictoryPoint)
                .map(c -> c.getDisplayName().replace("\n", " "))
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");

        // Bonus tracking
        boolean hasLongestRoad = winner.equals(gc.getLongestRoadOwner());
        boolean hasLargestArmy = winner.equals(gc.getLargestArmyOwner());

        StringBuilder msg = new StringBuilder();
        msg.append("🏆  ").append(winner.getName()).append(" wins!\n\n");

        msg.append("── Victory Points breakdown ──\n");
        msg.append("  Public VP (buildings):  ")
                .append(publicVP - (hasLongestRoad ? gc.getGame().getConfig().getLongestRoadBonus() : 0)
                        - (hasLargestArmy ? gc.getGame().getConfig().getLargestArmyBonus() : 0))
                .append("\n");

        if (hasLongestRoad) {
            msg.append("  🛤 Longest Road:        +")
                    .append(gc.getGame().getConfig().getLongestRoadBonus()).append("\n");
        }

        if (hasLargestArmy) {
            msg.append("  ⚔ Largest Army:        +")
                    .append(gc.getGame().getConfig().getLargestArmyBonus()).append("\n");
        }

        if (vpCards > 0) {
            msg.append("  🃏 VP Cards revealed:   +").append(vpCards)
                    .append("  (").append(vpCardList).append(")\n");
        }

        msg.append("\n  Total VP:  ").append(publicVP + vpCards);

        JOptionPane.showMessageDialog(this, msg.toString(),
                "Game Over — " + winner.getName() + " wins!",
                JOptionPane.INFORMATION_MESSAGE);

        updateStatus();
    }

    private void handlePlayCard() {
        Player cur = gc.getGame().getCurrentPlayer();
        if (cur == null) return;

        List<DevelopmentCard> playable = cur.getCards().stream()
                .filter(c -> c.canBePlayed(gc.getGame(), cur))
                .collect(java.util.stream.Collectors.toList());

        if (playable.isEmpty()) {
            showActionError("No playable cards.");
            return;
        }

        String[] options = playable.stream()
                .map(c -> c.getDisplayName().replace("\n", " "))
                .toArray(String[]::new);

        int choice = JOptionPane.showOptionDialog(this,
                "Choose a card to play:", "Play Development Card",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice < 0) return;

        DevelopmentCard selected = playable.get(choice);
        CardPlayContext context = requestCardPlayContext(selected);
        if (context == null) {
            return;
        }

        try {
            gc.playDevelopmentCard(selected, context);
            if (gc.getGame().isRobberPhase() && context.isRobberMoveRequired()) {
                boardPanel.setInteractionMode(CatanBoardPanel.BoardInteractionMode.MOVE_ROBBER);
            } else if (gc.getGame().isBuildPhase() && context.isRoadPlacementRequired()) {
                boardPanel.setInteractionMode(CatanBoardPanel.BoardInteractionMode.BUILD_STREET);
            }

            JOptionPane.showMessageDialog(this,
                    context.getMessage() != null ? context.getMessage() : selected.getDisplayName(),
                    selected.getDisplayName().replace("\n", " "),
                    JOptionPane.INFORMATION_MESSAGE);

            updateStatus();
            boardPanel.repaint();
            checkAndHandleVictory();
        } catch (Exception ex) {
            showActionError(ex.getMessage());
        }
    }

    private CardPlayContext requestCardPlayContext(DevelopmentCard selected) {
        CardPlayContext context = new CardPlayContext();
        ResourceType[] types = playableResourceTypes();
        String[] names = java.util.Arrays.stream(types)
                .map(ResourceType::name).toArray(String[]::new);

        switch (selected.getInputRequirement()) {
            case ONE_RESOURCE -> {
                int choice = JOptionPane.showOptionDialog(this,
                        "Choose resource:", selected.getDisplayName().replace("\n", " "),
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, names, names[0]);
                if (choice < 0) return null;
                context.setSelectedResourceType(types[choice]);
            }
            case TWO_RESOURCES -> {
                int first = JOptionPane.showOptionDialog(this,
                        "First resource:", selected.getDisplayName().replace("\n", " ") + " (1/2)",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, names, names[0]);
                if (first < 0) return null;

                int second = JOptionPane.showOptionDialog(this,
                        "Second resource:", selected.getDisplayName().replace("\n", " ") + " (2/2)",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, names, names[0]);
                if (second < 0) return null;

                context.setSelectedResourceTypes(List.of(types[first], types[second]));
            }
            case NONE -> {
            }
        }

        return context;
    }

    private ResourceType[] playableResourceTypes() {
        return new ResourceType[]{
                ResourceType.BRICK, ResourceType.WOOD,
                ResourceType.WHEAT, ResourceType.SHEEP, ResourceType.ORE
        };
    }

    // ── 7. UI Helper Methods ──────────────────────────────────────

    private JButton actionButton(String label) {
        JButton b = new JButton(label);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setBackground(COL_DIMMED);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setEnabled(false);
        return b;
    }

    private void setBtn(JButton btn, boolean enabled, Color activeColor) {
        btn.setEnabled(enabled);
        btn.setBackground(enabled ? activeColor : COL_DIMMED);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
    }

    private void addHoverEffect(JButton btn, Color activeColor) {
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled())
                    btn.setBackground(activeColor.brighter());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(btn.isEnabled() ? activeColor : COL_DIMMED);
            }
        });
    }

    private JLabel deckTag(String text, Color bg) {
        JLabel l = new JLabel("  " + text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 9));
        l.setForeground(Color.WHITE);
        l.setBackground(bg);
        l.setOpaque(true);
        l.setBorder(new EmptyBorder(2, 2, 2, 2));
        return l;
    }

    private static JLabel sectionLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(SECTION);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel boldLabel(String t, int sz) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, sz));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel plainLabel(String t, int sz) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.PLAIN, sz));
        l.setForeground(Color.DARK_GRAY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JSeparator hLine() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setMaximumSize(new Dimension(275, 2));
        return sep;
    }


    // ── 8. Getters / Setters ──────────────────────────────────────

    public CatanBoardPanel getBoardPanel() {
        return boardPanel;
    }
}
