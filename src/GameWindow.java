import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class GameWindow extends JPanel implements ActionListener, MouseListener {
    Random rnd = new Random();

    int centerX;
    int centerY;
    Point centerPoint;
    Robot robot = new Robot();
    int mouseDeltaX = 0;
    int mouseDeltaY = 0;
    double worldOffsetX = 0;
    double worldOffsetY = 0;
    double sensivity = 0.3;

    ArrayList <Block> targetList;
    Image targetImg;
    int targetWidth = 80;
    int targetHeight = 80;
    int targetX;
    int targetY;

    boolean gameOver = false;
    int score = 0;

    Timer gameLoop;
    int gameTimeLimit = 30_000;
    int timeRemaining;

    boolean isClicked;

    public GameWindow() throws AWTException {
        setPreferredSize(new Dimension(1280, 720));
        setFocusable(true);

        targetList = new ArrayList<>();
        targetImg = new ImageIcon("target.png").getImage();

        gameLoop = new Timer(1000/240, this);
        addMouseListener(this);
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                GameWindow.this.mouseMoved(e);
            }
        });
        //  setCursor(getToolkit().createCustomCursor(
        //      new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank"));
    }

    void placeTarget() {
        Block target = new Block(targetX, targetY, targetWidth, targetHeight, targetImg);
        targetList.add(target);
        if (targetList.size() > 1) {
            targetList.remove(0);
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (targetList.isEmpty() && getWidth() > 0 && getHeight() > 0) {
            targetX = rnd.nextInt(getWidth()-targetWidth);
            targetY = rnd.nextInt(getHeight()-targetHeight);
            placeTarget();
        }
        draw(g);
    }

    void draw(Graphics g) {

        for (Block target : targetList) {
            g.drawImage(target.getImage(), target.getX(), target.getY(), null);
        }
        g.setColor(Color.black);
        g.setFont(new Font("Courier", Font.PLAIN, 40));
        g.drawString("Score: " + score, centerX, 50);
        g.drawString("Time Remaining: " + timeRemaining, 400, 50);
        // crosshair
        int thickness = 3;
        int size = 10;
        int gap = 8;

        g.setColor(Color.cyan);
        g.fillRect(centerX - thickness/2, centerY - gap/2 - size, thickness, size);
        g.fillRect(centerX - thickness/2, centerY + gap/2, thickness, size);
        g.fillRect(centerX - gap/2 - size, centerY - thickness/2, size, thickness);
        g.fillRect(centerX + gap/2, centerY - thickness/2, size, thickness);
    }

    void mouseMoved(MouseEvent e) {
        mouseDeltaX = e.getX() - centerX;
        mouseDeltaY = e.getY() - centerY;
        robot.mouseMove(centerX, centerY);
    }

    boolean collision(Block a) {
        return a.getBounds().contains(centerPoint);
    }

    long start = System.currentTimeMillis();
    void update() {
        if (!gameOver) {
            centerX = getWidth() / 2;
            centerY = getHeight() / 2;
            centerPoint = new Point(centerX, centerY);
            long elapsed = System.currentTimeMillis() - start;
            timeRemaining = Math.toIntExact((gameTimeLimit - elapsed));
            if (elapsed >= gameTimeLimit) {
                gameOver = true;
                return;
            }
            worldOffsetX -= mouseDeltaX * sensivity;
            worldOffsetY -= mouseDeltaY * sensivity;
            if (!targetList.isEmpty() && collision(targetList.getFirst()) && isClicked) {
                score++;
            }
            if (isClicked) {
                isClicked = false;
            }
        }
    }

    public void addNotify() {
        super.addNotify();
        gameLoop.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
        if (gameOver) {
            gameLoop.stop();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1) {
            isClicked = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
