import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("Aim Trainer");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1280, 720);
                frame.setLocationRelativeTo(null);
                frame.setResizable(false);

                GameWindow gameWindow = new GameWindow(frame);
                frame.setJMenuBar(gameWindow.getMenuBar());
                frame.add(gameWindow);
                frame.setVisible(true);

            } catch (AWTException e) {
                e.printStackTrace();
            }
        });
    }
}