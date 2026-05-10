import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class GameWindow extends JPanel implements ActionListener, MouseListener {
    Random rnd = new Random();

    int centerX;
    int centerY;
    Point centerPoint;
    int mouseDeltaX = 0;
    int mouseDeltaY = 0;

    int cameraX = 0;
    int cameraY = 0;

    double sensivity = 0.3;

    Robot robot;

    ArrayList <Block> targetList;
    Image targetImg;
    Image backgroundImg;
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
        backgroundImg = new ImageIcon("background.png").getImage();

        gameLoop = new Timer(1000/240, this);

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blank = toolkit.createCustomCursor(img, new Point(0, 0), "blank");
        setCursor(blank);

        addMouseListener(this);
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                centerX = getWidth() / 2;
                centerY = getHeight() / 2;
                calculateDeltaMousemovement(e.getX(), e.getY());
                Point p = getLocationOnScreen();
                robot.mouseMove(p.x + centerX, p.y +  centerY);
            }
        });
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
        g.drawImage(backgroundImg,0, 0, getWidth(), getHeight(),
                cameraX, cameraY, cameraX + getWidth(), cameraY + getHeight(),
                null);

        for (Block target : targetList) {
            g.drawImage(target.getImage(), target.getX(), target.getY(), null);
        }

        int fontSize = Math.max(6, getWidth() / 50);
        g.setFont(new Font("Arial", Font.BOLD, fontSize));
        FontMetrics fm = g.getFontMetrics();
        String scoreText = "Score: " + score;
        int scoreX = (getWidth() - fm.stringWidth(scoreText)) / 2;
        int scoreY = fm.getHeight();
        g.drawString(scoreText, scoreX, scoreY);

        String timeText = "Time Remaining: " + timeRemaining;
        int timeX = getWidth() - fm.stringWidth(timeText) - 10 ;
        int timeY = fm.getHeight();
        g.drawString(timeText, timeX, timeY);

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


    boolean collision(Block a) {
        return a.getBounds().contains(centerPoint);
    }

    long start = System.currentTimeMillis();
    void update() {
        if (!gameOver) {
            centerPoint = new Point(centerX, centerY);
            long elapsed = System.currentTimeMillis() - start;
            timeRemaining = Math.toIntExact((gameTimeLimit - elapsed));
            if (elapsed >= gameTimeLimit) {
                gameOver = true;
                return;
            }
            cameraX += (int) (mouseDeltaX * sensivity);
            cameraY += (int) (mouseDeltaY * sensivity);
            cameraX = Math.max(0, Math.min(cameraX, backgroundImg.getWidth(null) - getWidth()));
            cameraY = Math.max(0, Math.min(cameraY, backgroundImg.getHeight(null) - getHeight()));

            if (!targetList.isEmpty() && collision(targetList.getFirst()) && isClicked) {
                score++;
            }
            if (isClicked) {
                isClicked = false;
            }
        }
    }

    public void calculateDeltaMousemovement(int mouseX, int mouseY) {
        mouseDeltaX = mouseX - centerX;
        mouseDeltaY = mouseY - centerY;
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
