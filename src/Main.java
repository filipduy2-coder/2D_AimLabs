import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) throws AWTException {
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(1280, 720);
        window.setResizable(false);
        window.setTitle("2D AimLabs");
        window.setLocationRelativeTo(null);

        GameWindow gameWindow = new GameWindow();
        window.setJMenuBar(gameWindow.menuBar);
        window.add(gameWindow);
        window.setVisible(true);
    }
}