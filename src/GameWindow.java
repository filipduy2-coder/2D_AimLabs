import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class GameWindow extends JPanel implements KeyListener, ActionListener {
    Random rnd = new Random();
    final int screenWidth = 500;
    final int screenHeight = 500;

    int centerX = screenWidth / 2;
    int centerY = screenHeight / 2;
    Point centerPoint;

    ArrayList <Block> targetList;
    Image targetImg;
    int targetWidth = 80;
    int targetHeight = 80;
    int targetX = rnd.nextInt(screenWidth);
    int targetY = rnd.nextInt(screenHeight);

    boolean gameOver = false;

    Timer gameLoop;

    boolean isClicked;

    public GameWindow() {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setFocusable(true);
        addKeyListener(this);

        targetList = new ArrayList<>();

        gameLoop = new Timer(1000/120, this);
        gameLoop.start();

        placeTarget();

        //  setCursor(getToolkit().createCustomCursor(
        //      new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank"));
    }

    void placeTarget() {
        targetImg = new ImageIcon("target.png").getImage();
        Block target = new Block(targetX, targetY, targetWidth, targetHeight, targetImg);
        targetList.add(target);
        if (targetList.size() > 1) {
            targetList.remove(0);
        }
    }

    void move
    boolean collision(Block a) {
        centerPoint = new Point(centerX, centerY);
        return a.getBounds().contains(centerPoint);
    }


    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEY_PRESSED) {
            isClicked = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
