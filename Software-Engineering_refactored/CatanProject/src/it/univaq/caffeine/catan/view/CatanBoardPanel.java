package it.univaq.caffeine.catan.view;

import it.univaq.caffeine.catan.controller.GameController;
import it.univaq.caffeine.catan.model.board.*;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.ResourceType;
import it.univaq.caffeine.catan.model.player.*;
import it.univaq.caffeine.catan.model.cards.ResourceCard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Main UI panel that renders the Catan game board.
 * Handles all mouse interactions for building and robber placement,
 * as well as managing the bot auto-play visual delays.
 */
public class CatanBoardPanel extends JPanel implements MouseMotionListener, MouseListener {

    // ── 1. Constants & Static Assets ──────────────────────────────

    private static final int   INTER_RADIUS    = 10;
    private static final int   CITY_RADIUS     = INTER_RADIUS + 3;
    private static final int   EDGE_CLICK_DIST = 14;

    private static final Color OCEAN           = new Color(59, 120, 180);
    private static final Color HEX_BORDER      = new Color(255, 255, 255);
    private static final Color TOKEN_BG        = new Color(255, 248, 220);
    private static final Color BUILDING_ICON   = new Color(255, 255, 255, 180);

    private static final java.util.Map<ResourceType, java.awt.image.BufferedImage>
            TERRAIN_IMAGES = new EnumMap<>(ResourceType.class);

    private static final String TILES_PATH = "/it/univaq/caffeine/catan/tiles/";

    static {
        loadTile(ResourceType.SHEEP,  TILES_PATH + "pasture.png");
        loadTile(ResourceType.WOOD,   TILES_PATH + "forest.png");
        loadTile(ResourceType.WHEAT,  TILES_PATH + "fields.png");
        loadTile(ResourceType.ORE,    TILES_PATH + "mountains.png");
        loadTile(ResourceType.BRICK,  TILES_PATH + "hills.png");
        loadTile(ResourceType.DESERT, TILES_PATH + "desert.png");
    }

    static final String[] PC_STR = {"Red", "Blue", "Green", "Yellow"};
    static final Color[]  PC_COL = {
            new Color(220, 50, 50), new Color(50, 110, 220),
            new Color(40, 175, 40), new Color(255, 200, 0)
    };


    // ── 2. Enums & Instance Variables ─────────────────────────────

    public enum BoardInteractionMode {
        // UC 2.0 — Initial placement
        INITIAL_COLONY, INITIAL_ROAD,
        // UC 3.0 — Active turn
        TURN_ACTIONS,
        BUILD_COLONY,
        BUILD_CITY,
        BUILD_STREET,
        MOVE_ROBBER
    }

    private final GameController gc;
    private final GameWindow     parent;

    private BoardInteractionMode interactionMode = BoardInteractionMode.INITIAL_COLONY;

    private static final int BOT_DELAY_MS = 900;
    private boolean botPlaying = false;

    // UI Interaction state
    private Intersection  hoveredInter;
    private Edge          hoveredEdge;
    private HexagonalTile hoveredTile;


    // ── 3. Constructor ────────────────────────────────────────────

    public CatanBoardPanel(GameController gc, GameWindow parent) {
        this.gc = gc;
        this.parent = parent;
        setPreferredSize(new Dimension(760, 780));
        setBackground(OCEAN);
        addMouseMotionListener(this);
        addMouseListener(this);
    }


    // ── 4. Core Rendering (Paint Methods) ─────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gc.getGame() == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GameMap map = gc.getGame().getMap();
        drawHexagons(g2, map);
        drawPorts(g2, map);
        drawEdges(g2, map);
        drawIntersections(g2, map);
        drawBotOverlay(g2);
    }

    private void drawHexagons(Graphics2D g2, GameMap map) {
        for (HexagonalTile tile : map.getHexagons()) {
            Polygon hex = hexPolygon(tile.getCenterX(), tile.getCenterY());
            java.awt.image.BufferedImage img = TERRAIN_IMAGES.get(tile.getTerrainType());

            if (img != null) {
                Shape oldClip = g2.getClip();
                g2.setColor(terrainColor(tile.getTerrainType()));
                g2.fillPolygon(hex);
                g2.setClip(hex);
                int cx   = (int) tile.getCenterX();
                int cy   = (int) tile.getCenterY();
                int size = (int) (GameMap.HEX_SIZE * 2.6);
                g2.drawImage(img, cx - size / 2, cy - size / 2, size, size, null);
                g2.setClip(oldClip);
            } else {
                g2.setColor(terrainColor(tile.getTerrainType()));
                g2.fillPolygon(hex);
            }

            g2.setColor(HEX_BORDER);
            g2.setStroke(new BasicStroke(3f));
            g2.drawPolygon(hex);

            // Numeric token
            if (tile.getToken() != null) {
                int n  = tile.getToken().getNumber();
                int cx = (int) tile.getCenterX();
                int cy = (int) tile.getCenterY();

                g2.setColor(TOKEN_BG);
                g2.fillOval(cx - 17, cy - 17, 34, 34);
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(cx - 17, cy - 17, 34, 34);

                g2.setColor((n == 6 || n == 8) ? new Color(200, 20, 20) : Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                String s = String.valueOf(n);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s, cx - fm.stringWidth(s) / 2, cy + fm.getAscent() / 2 - 2);

                // Draw dots representing probability
                int dots = 6 - Math.abs(7 - n);
                for (int d = 0; d < dots; d++) {
                    int dx = cx - (dots - 1) * 3 + d * 6;
                    g2.fillOval(dx - 2, cy + 11, 4, 4);
                }
            } else {
                g2.setColor(new Color(120, 90, 50));
                g2.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 11));
                FontMetrics fm = g2.getFontMetrics();
                String s = "Desert";
                g2.drawString(s, (int) tile.getCenterX() - fm.stringWidth(s) / 2, (int) tile.getCenterY() - 8);
            }

            // Robber
            Robber robber = gc.getGame().getRobber();
            if (robber != null && robber.occupies(tile)) {
                drawRobber(g2, (int) tile.getCenterX(), (int) tile.getCenterY());
            }

            // Robber hover (MOVE_ROBBER interactionMode) — red overlay on tile
            if (interactionMode == BoardInteractionMode.MOVE_ROBBER && tile == hoveredTile && (robber == null || !robber.occupies(tile))) {
                g2.setColor(new Color(255, 0, 0, 80));
                g2.fillPolygon(hex);
                g2.setColor(new Color(220, 30, 30));
                g2.setStroke(new BasicStroke(3f));
                g2.drawPolygon(hex);
            }
        }
    }

    private void drawPorts(Graphics2D g2, GameMap map) {
        for (Port port : map.getPorts()) {
            int px = (int) port.getDisplayX();
            int py = (int) port.getDisplayY();
            int w = 46, h = 36;

            Color bg = port.isGeneric()
                    ? new Color(240, 220, 150)
                    : terrainColor(port.getResourceType()).brighter();
            Color border = port.isGeneric() ? new Color(160, 130, 50) : HEX_BORDER;

            // Dashed lines from port to adjacent intersections
            // Drawn BEFORE the box so they remain underneath
            if (!port.getAdjacentIntersections().isEmpty()) {
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 4f}, 0f));
                for (Intersection inter : port.getAdjacentIntersections()) {
                    g2.drawLine(px, py, (int) inter.getX(), (int) inter.getY());
                }
            }

            // Port box
            g2.setStroke(new BasicStroke(1.8f));
            g2.setColor(bg);
            g2.fillRoundRect(px - w / 2, py - h / 2, w, h, 8, 8);
            g2.setColor(border);
            g2.drawRoundRect(px - w / 2, py - h / 2, w, h, 8, 8);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            String rate = port.getExchangeRate() + ":1";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(rate, px - fm.stringWidth(rate) / 2, py - 1);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
            String sub = port.isGeneric()
                    ? "any"
                    : port.getResourceType().name().substring(0, 3);
            fm = g2.getFontMetrics();
            g2.drawString(sub, px - fm.stringWidth(sub) / 2, py + 11);

            // Colored circles on adjacent nodes (shows where to build to access the port)
            for (Intersection inter : port.getAdjacentIntersections()) {
                int ix = (int) inter.getX();
                int iy = (int) inter.getY();
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 160));
                g2.fillOval(ix - 7, iy - 7, 14, 14);
                g2.setColor(border);
                g2.drawOval(ix - 7, iy - 7, 14, 14);
            }
        }
    }

    private void drawEdges(Graphics2D g2, GameMap map) {
        for (Edge e : map.getEdges()) {
            Intersection a = e.getEndpoints()[0], b = e.getEndpoints()[1];

            if (e.getStreet() != null) {
                g2.setColor(playerColor(e.getStreet().getColor()));
                g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY());
                g2.setColor(new Color(255, 255, 255, 80));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY());

            } else if ((interactionMode == BoardInteractionMode.INITIAL_ROAD || interactionMode == BoardInteractionMode.BUILD_STREET) && e == hoveredEdge) {
                g2.setColor(new Color(255, 255, 80, 210));
                g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY());

            } else {
                g2.setColor(new Color(0, 0, 0, 35));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY());
            }
        }
    }

    private void drawIntersections(Graphics2D g2, GameMap map) {
        for (Intersection inter : map.getIntersections()) {
            int x = (int) inter.getX(), y = (int) inter.getY(), r = INTER_RADIUS;

            if (inter.getBuilding() != null) {
                // Occupied intersection — draw the building
                drawBuilding(g2, inter.getBuilding(), x, y);

            } else if (interactionMode == BoardInteractionMode.INITIAL_COLONY && inter == hoveredInter) {
                // UC 2.0 — initial colony hover (yellow)
                g2.setColor(new Color(255, 230, 0, 220));
                g2.fillOval(x - r, y - r, 2 * r, 2 * r);
                g2.setColor(Color.ORANGE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x - r, y - r, 2 * r, 2 * r);

            } else if (interactionMode == BoardInteractionMode.BUILD_COLONY && inter == hoveredInter) {
                // UC 3.0 — colony construction hover (yellow, same style)
                g2.setColor(new Color(255, 230, 0, 220));
                g2.fillOval(x - r, y - r, 2 * r, 2 * r);
                g2.setColor(Color.ORANGE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x - r, y - r, 2 * r, 2 * r);

            } else if (interactionMode == BoardInteractionMode.BUILD_CITY && inter == hoveredInter) {
                // UC 3.0 — city upgrade hover (green)
                g2.setColor(new Color(50, 200, 50, 220));
                g2.fillOval(x - r, y - r, 2 * r, 2 * r);
                g2.setColor(new Color(20, 140, 20));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x - r, y - r, 2 * r, 2 * r);

            } else {
                // Free intersection not hovered — transparent gray
                g2.setColor(new Color(200, 200, 200, 80));
                g2.fillOval(x - r + 2, y - r + 2, 2 * (r - 2), 2 * (r - 2));
            }
        }
    }

    private void drawBuilding(Graphics2D g2, Building building, int x, int y) {
        Color color = playerColor(building.getColor());
        if (building instanceof City) {
            drawCity(g2, x, y, color);
        } else {
            drawColony(g2, x, y, color);
        }
    }

    private void drawColony(Graphics2D g2, int x, int y, Color color) {
        drawBuildingBase(g2, x, y, INTER_RADIUS, color);
        int[] hx = {x - 5, x, x + 5, x + 5, x - 5};
        int[] hy = {y + 2, y - 5, y + 2, y + 5, y + 5};
        g2.setColor(BUILDING_ICON);
        g2.fillPolygon(hx, hy, 5);
    }

    private void drawCity(Graphics2D g2, int x, int y, Color color) {
        drawBuildingBase(g2, x, y, CITY_RADIUS, color);
        int[] cx = {x - 8, x - 8, x - 5, x - 5, x - 2, x - 2, x + 2, x + 2, x + 5, x + 5, x + 8, x + 8};
        int[] cy = {y + 6, y - 4, y - 4, y - 7, y - 7, y - 4, y - 4, y - 7, y - 7, y - 4, y - 4, y + 6};
        g2.setColor(BUILDING_ICON);
        g2.fillPolygon(cx, cy, cx.length);
        g2.setColor(new Color(255, 255, 255, 210));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawLine(x - 8, y + 6, x + 8, y + 6);
        g2.drawLine(x - 3, y + 6, x - 3, y);
        g2.drawLine(x + 3, y + 6, x + 3, y);
    }

    private void drawBuildingBase(Graphics2D g2, int x, int y, int radius, Color color) {
        g2.setColor(color);
        g2.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
    }

    private void drawRobber(Graphics2D g2, int cx, int cy) {
        int r = 16;
        g2.setColor(new Color(20, 20, 20, 220));
        g2.fillOval(cx - r, cy + 4, 2 * r, r + 6);
        g2.fillOval(cx - r / 2, cy - r, r, r + 4);
        g2.setColor(new Color(80, 0, 0, 200));
        g2.fillArc(cx - r / 2, cy - r, r, r, 0, 180);
        g2.setColor(new Color(255, 80, 80));
        g2.fillOval(cx - 6, cy - r + 5, 4, 4);
        g2.fillOval(cx + 2, cy - r + 5, 4, 4);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.drawString("Rob.", cx - 9, cy + 14);
    }

    private void drawBotOverlay(Graphics2D g2) {
        Player cur = gc.getGame().getCurrentPlayer();
        if (cur instanceof BotPlayer) {
            String msg = cur.getName() + " is thinking...";
            g2.setFont(new Font("SansSerif", Font.BOLD, 17));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(msg) + 24, h = 34, x = (getWidth() - w) / 2, y = 18;
            g2.setColor(new Color(0, 0, 0, 165));
            g2.fillRoundRect(x, y, w, h, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawString(msg, x + 12, y + 23);
        }
    }


    // ── 5. Bot Auto-Play Logic ────────────────────────────────────

    public void checkAndScheduleBotMove() {
        if (gc.getGame().isGameOver()) return;
        if (interactionMode != BoardInteractionMode.INITIAL_COLONY && interactionMode != BoardInteractionMode.INITIAL_ROAD && interactionMode != BoardInteractionMode.TURN_ACTIONS) return;

        // Guard against double scheduling — ONLY in active game interactionMode (UC 3.0)
        // Not needed during UC 2.0 (INITIAL_COLONY, INITIAL_ROAD)
        if (interactionMode == BoardInteractionMode.TURN_ACTIONS && botPlaying) return;

        Player cur = gc.getGame().getCurrentPlayer();
        if (cur instanceof BotPlayer) {
            // Set botPlaying only for UC 3.0
            if (interactionMode == BoardInteractionMode.TURN_ACTIONS) botPlaying = true;
            repaint();
            parent.updateStatus();

            javax.swing.Timer t = new javax.swing.Timer(BOT_DELAY_MS, e -> executeBotMove());
            t.setRepeats(false);
            t.start();
        }
    }

    private void executeBotMove() {
        Player cur = gc.getGame().getCurrentPlayer();
        if (!(cur instanceof BotPlayer bot)) return;

        try {
            // ── UC 2.0 — Initial placement ──
            if (interactionMode == BoardInteractionMode.INITIAL_COLONY) {
                Intersection chosen = bot.chooseInitialPlacement(gc.getGame());
                if (chosen != null) {
                    gc.placeColony(chosen.getId());
                    interactionMode = BoardInteractionMode.INITIAL_ROAD;
                    parent.updateStatus(); repaint();

                    javax.swing.Timer t = new javax.swing.Timer(BOT_DELAY_MS, e -> executeBotMove());
                    t.setRepeats(false); t.start();
                }

            } else if (interactionMode == BoardInteractionMode.INITIAL_ROAD) {
                Colony last = gc.getGame().getLastPlacedColony();
                Edge chosen = bot.chooseInitialRoad(gc.getGame(), last);

                if (chosen != null) {
                    gc.placeRoad(chosen.getId());
                    if (gc.getGame().isActivePhase()) {
                        interactionMode = BoardInteractionMode.TURN_ACTIONS;
                        parent.updateStatus(); repaint();

                        JOptionPane.showMessageDialog(this, buildInitialResourceSummary(),
                                "Initial Resources Distributed", JOptionPane.INFORMATION_MESSAGE);
                        JOptionPane.showMessageDialog(this,
                                "Placement complete!\nInitial resources have been assigned.\nThe game begins!",
                                "Placement Complete", JOptionPane.INFORMATION_MESSAGE);
                        checkAndScheduleBotMove();
                    } else {
                        interactionMode = BoardInteractionMode.INITIAL_COLONY;
                        parent.updateStatus(); repaint();
                        checkAndScheduleBotMove();
                    }
                }

                // ── UC 3.0 — Active bot turn ──
            } else if (interactionMode == BoardInteractionMode.TURN_ACTIONS) {

                if (!gc.getGame().isDiceRolled()) {
                    // ── STEP 1: Roll Dice ──
                    int result = gc.rollDice();
                    JOptionPane.showMessageDialog(parent,
                            "🎲 " + cur.getName() + " rolled: " + result +
                                    (result == 7 ? "\n⚠ Robber activated!" : "\n✅ Resources distributed!"),
                            "Bot Dice Roll", JOptionPane.INFORMATION_MESSAGE);

                    parent.updateStatus(); repaint();
                    bot.resetTurn();

                    if (result == 7) {
                        // Human discards + move robber
                        try {
                            Map<Player, Integer> humanDiscards = gc.activateRobber();
                            humanDiscards.forEach(parent::showDiscardDialog);

                            javax.swing.Timer t = new javax.swing.Timer(BOT_DELAY_MS, ev -> handleBotRobberMove(bot));
                            t.setRepeats(false); t.start();
                        } catch (Exception ex) {
                            System.err.println("R=7 error: " + ex.getMessage());
                            ex.printStackTrace();
                            // Recovery: skip robber and continue turn
                            javax.swing.Timer t = new javax.swing.Timer(BOT_DELAY_MS, ev -> executeBotMove());
                            t.setRepeats(false); t.start();
                        }
                    } else {
                        // ← THIS BRANCH WAS MISSING — caused bot freeze previously
                        javax.swing.Timer t = new javax.swing.Timer(BOT_DELAY_MS, ev -> executeBotMove());
                        t.setRepeats(false); t.start();
                    }

                } else {
                    // ── STEP 2: Try Actions ──
                    boolean didAction = false;
                    try {
                        didAction = bot.chooseAction(gc.getGame());
                    } catch (Exception ex) {
                        System.err.println("Bot action error: " + ex.getMessage());
                    }

                    if (didAction) {
                        // Check if the action was a trade → show dialog
                        String tradeInfo = bot.getAndClearLastTradeInfo();
                        if (tradeInfo != null) {
                            JOptionPane.showMessageDialog(parent,
                                    "🔄 " + cur.getName() + " traded with bank/port:\n\n" + tradeInfo,
                                    "Bot Trade", JOptionPane.INFORMATION_MESSAGE);
                        }

                        parent.updateStatus(); repaint();
                        if (gc.getGame().isGameOver()) {
                            parent.checkAndHandleVictory();
                            return;
                        }

                        javax.swing.Timer t = new javax.swing.Timer(BOT_DELAY_MS, ev -> executeBotMove());
                        t.setRepeats(false); t.start();

                    } else {
                        // ── STEP 3: End Turn ──
                        gc.endTurn();
                        botPlaying = false;
                        interactionMode = BoardInteractionMode.TURN_ACTIONS;

                        parent.updateStatus(); repaint();
                        if (!gc.getGame().isGameOver()) {
                            Player next = gc.getGame().getCurrentPlayer();
                            if (next != null && next.isHuman()) {
                                parent.showTurnSeparator(next);
                            }
                        }
                        checkAndScheduleBotMove();
                    }
                }
            }

        } catch (Exception ex) {
            System.err.println("Bot outer error: " + ex.getMessage());
            ex.printStackTrace();

            // Recovery — always unblock the game in case of an unexpected error
            botPlaying = false;
            interactionMode = BoardInteractionMode.TURN_ACTIONS;
            parent.updateStatus(); repaint();

            javax.swing.Timer recovery = new javax.swing.Timer(BOT_DELAY_MS, ev -> {
                try {
                    if (gc.getGame().isRobberPhase()) {
                        gc.finishRobberPhase();
                    }
                    if (gc.getGame().isTradePhase()) {
                        gc.finishTradePhase();
                    }
                    if (gc.getGame().isBuildPhase()
                            && gc.getFreeRoadsRemaining() == 0) {
                        gc.cancelBuildPhase();
                    }
                    if (gc.getGame().isActivePhase() && gc.getGame().isDiceRolled()) {
                        gc.endTurn();
                    }
                } catch (Exception ignored) {
                    System.err.println("Recovery endTurn failed: " + ignored.getMessage());
                }
                botPlaying = false;
                interactionMode = BoardInteractionMode.TURN_ACTIONS;
                parent.updateStatus();
                checkAndScheduleBotMove();
            });
            recovery.setRepeats(false); recovery.start();
        }
    }

    private void handleBotRobberMove(BotPlayer bot) {
        HexagonalTile current = gc.getGame().getRobber().getCurrentTile();
        HexagonalTile chosen  = bot.chooseRobberTile(gc.getGame(), current);

        try {
            if (chosen != null) {
                gc.moveRobberDirect(chosen);
                List<Player> victims = gc.getStealablePlayers();
                if (!victims.isEmpty()) {
                    gc.stealFrom(victims.get(new java.util.Random().nextInt(victims.size())));
                }
            } else {
                System.err.println("Bot robber: chooseTileForRobber returned null!");
            }
        } catch (Exception e) {
            System.err.println("Bot robber move FAILED: " + e.getMessage());
            e.printStackTrace();  // ← show full stack trace
        } finally {
            if (gc.getGame().isRobberPhase()) {
                gc.finishRobberPhase();
            }
        }

        parent.updateStatus(); repaint();
        javax.swing.Timer t = new javax.swing.Timer(BOT_DELAY_MS, ev -> executeBotMove());
        t.setRepeats(false); t.start();
    }


    // ── 6. Mouse Event Handlers ───────────────────────────────────

    @Override
    public void mouseMoved(MouseEvent e) {
        if (gc.getGame() == null) return;
        Player cur = gc.getGame().getCurrentPlayer();
        if (cur instanceof BotPlayer) return;

        GameMap map = gc.getGame().getMap();
        hoveredInter = null;
        hoveredEdge = null;

        // UC 2.0
        if (interactionMode == BoardInteractionMode.INITIAL_COLONY) {
            for (Intersection i : map.getIntersections()) {
                if (dist(e.getX(), e.getY(), i.getX(), i.getY()) < INTER_RADIUS + 4) {
                    hoveredInter = i;
                    break;
                }
            }
        }

        // UC 2.0 + UC 3.0
        if (interactionMode == BoardInteractionMode.INITIAL_ROAD || interactionMode == BoardInteractionMode.BUILD_STREET) {
            for (Edge ed : map.getEdges()) {
                if (dist(e.getX(), e.getY(), ed.getCenterX(), ed.getCenterY()) < EDGE_CLICK_DIST) {
                    hoveredEdge = ed;
                    break;
                }
            }
        }

        // UC 3.0
        if (interactionMode == BoardInteractionMode.BUILD_COLONY || interactionMode == BoardInteractionMode.BUILD_CITY) {
            for (Intersection i : map.getIntersections()) {
                if (dist(e.getX(), e.getY(), i.getX(), i.getY()) < INTER_RADIUS + 4) {
                    hoveredInter = i;
                    break;
                }
            }
        }

        repaint();

        // Hover robber (MOVE_ROBBER interactionMode)
        if (interactionMode == BoardInteractionMode.MOVE_ROBBER) {
            hoveredTile = null;
            for (HexagonalTile tile : gc.getGame().getMap().getHexagons()) {
                if (dist(e.getX(), e.getY(), tile.getCenterX(), tile.getCenterY()) < GameMap.HEX_SIZE * 0.85) {
                    hoveredTile = tile;
                    break;
                }
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Player cur = gc.getGame() != null ? gc.getGame().getCurrentPlayer() : null;
        if (cur == null || cur instanceof BotPlayer) return;

        // ── UC 2.0 — Initial placement ──
        if (interactionMode == BoardInteractionMode.INITIAL_COLONY && hoveredInter != null) {
            try {
                gc.placeColony(hoveredInter.getId());
                interactionMode = BoardInteractionMode.INITIAL_ROAD;
                parent.updateStatus(); repaint();
            } catch (Exception ex) { showError(ex.getMessage()); }

        } else if (interactionMode == BoardInteractionMode.INITIAL_ROAD && hoveredEdge != null) {
            try {
                gc.placeRoad(hoveredEdge.getId());
                if (gc.getGame().isActivePhase()) {
                    interactionMode = BoardInteractionMode.TURN_ACTIONS;
                    parent.updateStatus(); repaint();

                    JOptionPane.showMessageDialog(this, buildInitialResourceSummary(),
                            "Initial Resources Distributed", JOptionPane.INFORMATION_MESSAGE);
                    JOptionPane.showMessageDialog(this,
                            "Placement complete!\nInitial resources have been assigned.\nThe game begins!",
                            "Placement Complete", JOptionPane.INFORMATION_MESSAGE);
                    checkAndScheduleBotMove();
                } else {
                    interactionMode = BoardInteractionMode.INITIAL_COLONY;
                    parent.updateStatus(); repaint();
                    checkAndScheduleBotMove();
                }
            } catch (Exception ex) { showError(ex.getMessage()); }

            // ── UC 3.0 — Construction during turn ──
        } else if (interactionMode == BoardInteractionMode.BUILD_COLONY && hoveredInter != null) {
            try {
                gc.build(BuildingType.COLONY, hoveredInter.getId());
                interactionMode = BoardInteractionMode.TURN_ACTIONS;
                parent.updateStatus(); repaint();
                parent.checkAndHandleVictory();
            } catch (Exception ex) { showError(ex.getMessage()); }

        } else if (interactionMode == BoardInteractionMode.BUILD_CITY && hoveredInter != null) {
            try {
                gc.build(BuildingType.CITY, hoveredInter.getId());
                interactionMode = BoardInteractionMode.TURN_ACTIONS;
                parent.updateStatus(); repaint();
                parent.checkAndHandleVictory();
            } catch (Exception ex) { showError(ex.getMessage()); }

        } else if (interactionMode == BoardInteractionMode.BUILD_STREET && hoveredEdge != null) {
            // ← UNIQUE unified block — handles both free and normal roads
            try {
                if (gc.getFreeRoadsRemaining() > 0) {
                    // Road Building card — no cost
                    gc.buildFreeStreet(hoveredEdge.getId());
                    hoveredEdge = null;  // reset hover
                    parent.updateStatus(); repaint();
                    parent.checkAndHandleVictory();

                    if (gc.getFreeRoadsRemaining() > 0) {
                        JOptionPane.showMessageDialog(this,
                                "Road 1 placed! Now place the second road.",
                                "Road Building", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        interactionMode = BoardInteractionMode.TURN_ACTIONS;
                        parent.updateStatus(); repaint();
                    }
                } else {
                    // Normal build with resource cost
                    gc.build(BuildingType.STREET, hoveredEdge.getId());
                    interactionMode = BoardInteractionMode.TURN_ACTIONS;
                    parent.updateStatus(); repaint();
                    parent.checkAndHandleVictory();
                }
            } catch (Exception ex) { showError(ex.getMessage()); }

            // ── Robber management ──
        } else if (interactionMode == BoardInteractionMode.MOVE_ROBBER && hoveredTile != null) {
            try {
                gc.moveRobber(hoveredTile.getId());
                hoveredTile = null;
                parent.handleRobberSteal();
                interactionMode = BoardInteractionMode.TURN_ACTIONS;
                parent.updateStatus(); repaint();
            } catch (Exception ex) { showError(ex.getMessage()); }
        }
    }

    // Unused mouse events
    @Override public void mouseDragged(MouseEvent e)  {}
    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}


    // ── 7. Dialogs & UI Helpers ───────────────────────────────────

    private JScrollPane buildInitialResourceSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Resources distributed from each player's settlement:\n\n");

        for (Player p : gc.getGame().getPlayers()) {
            sb.append("  ").append(p.getName()).append(" (").append(p.getColor()).append("):\n");

            Map<ResourceType, Integer> counts = new LinkedHashMap<>();
            for (ResourceCard rc : p.getResources()) {
                counts.merge(rc.getType(), 1, Integer::sum);
            }

            if (counts.isEmpty()) {
                sb.append("    No resources (adjacent tiles are desert)\n");
            } else {
                counts.forEach((type, count) ->
                        sb.append("    ").append(type.name()).append(": \u00d7").append(count).append("\n"));
            }
            sb.append("\n");
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(350, 200));
        return scroll;
    }

    private void showError(String msg) {
        Object display = msg.startsWith("<html>") ? new JLabel(msg) : msg;
        JOptionPane.showMessageDialog(this, display, "Invalid Placement", JOptionPane.WARNING_MESSAGE);
    }


    // ── 8. Utility & Math Helpers ─────────────────────────────────

    private static void loadTile(ResourceType type, String path) {
        try (var stream = CatanBoardPanel.class.getResourceAsStream(path)) {
            if (stream != null) {
                TERRAIN_IMAGES.put(type, javax.imageio.ImageIO.read(stream));
            } else {
                System.err.println("Tile image not found: " + path);
            }
        } catch (Exception e) {
            System.err.println("Error loading tile: " + path + " — " + e.getMessage());
        }
    }

    /** Fallback color used when a tile image is not available. Also used for ports. */
    public static Color terrainColor(ResourceType type) {
        return switch (type) {
            case WOOD   -> new Color(34,  120,  34);
            case SHEEP  -> new Color(60,  175, 148);
            case WHEAT  -> new Color(240, 200,  20);
            case BRICK  -> new Color(185,  65,  30);
            case ORE    -> new Color(120, 120, 145);
            case DESERT -> new Color(210, 190, 140);
        };
    }

    private Polygon hexPolygon(double cx, double cy) {
        int[] xs = new int[6], ys = new int[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(-90 + 60 * i);
            xs[i] = (int) (cx + GameMap.HEX_SIZE * Math.cos(a));
            ys[i] = (int) (cy + GameMap.HEX_SIZE * Math.sin(a));
        }
        return new Polygon(xs, ys, 6);
    }

    private static double dist(double ax, double ay, double bx, double by) {
        return Math.hypot(ax - bx, ay - by);
    }

    public static Color playerColor(String n) {
        for (int i = 0; i < PC_STR.length; i++) {
            if (PC_STR[i].equalsIgnoreCase(n)) return PC_COL[i];
        }
        return Color.GRAY;
    }


    // ── 9. Getters / Setters ──────────────────────────────────────

    public void setInteractionMode(BoardInteractionMode mode) {
        interactionMode = mode;
        repaint();
    }

    public BoardInteractionMode getInteractionMode() {
        return interactionMode;
    }
}
