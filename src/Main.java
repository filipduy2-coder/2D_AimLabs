import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true);
        window.setTitle("2D AimLabs");
        window.setLocationRelativeTo(null);

        GameWindow gameWindow = new GameWindow();
        window.add(gameWindow);
        window.setVisible(true);
    }
}