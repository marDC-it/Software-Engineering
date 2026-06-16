package it.univaq.caffeine.catan;

import it.univaq.caffeine.catan.controller.GameController;
import it.univaq.caffeine.catan.view.GameWindow;
import it.univaq.caffeine.catan.view.SetupWizard;

import javax.swing.*;


public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}

            GameController gc     = new GameController();
            SetupWizard    wizard = new SetupWizard(gc);

            if (!wizard.run()) return;   // Setup cancelled

            GameWindow window = new GameWindow(gc);
            window.setVisible(true);
            window.getBoardPanel().checkAndScheduleBotMove();
        });
    }
}
