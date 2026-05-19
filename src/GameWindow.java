import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class GameWindow extends JPanel implements ActionListener, MouseListener, KeyListener {
    Random rnd = new Random();

    JMenuBar menuBar;

    int centerX;
    int centerY;
    Point centerPoint;
    int mouseDeltaX = 0;
    int mouseDeltaY = 0;
    boolean recentering = false;

    int cameraX = 0;
    int cameraY = 0;

    JSlider sensitivitySlider;
    boolean draggingSlider = false;

    JTextField sensitivityTF;
    double sensitivity = 0.2;
    int scale = 100;
    double zoom = 0.8;

    Robot robot;

    ArrayList<Block> targetList;
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
    long pauseStartTime = 0;
    long totalPausedTime = 0;

    boolean isClicked;
    boolean isPaused = false;

    Rectangle resumeButton = new Rectangle();
    Rectangle restartButton = new Rectangle();
    Rectangle leaveButton = new Rectangle();

    public GameWindow() throws AWTException {
        menuBar = new JMenuBar();
        JMenu scoreBoardMenu = new JMenu("Scoreboard");
        JMenuItem scoreOpenItem = new JMenuItem("Open Scoreboard");
        scoreOpenItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isPaused = true;
                pauseStartTime = System.currentTimeMillis();
                ScoreWindow scoreWindow = new ScoreWindow();
            }
        });
        scoreBoardMenu.add(scoreOpenItem);
        menuBar.add(scoreBoardMenu);
        setFocusable(true);

        targetList = new ArrayList<>();
        targetImg = new ImageIcon("target.png").getImage();
        backgroundImg = new ImageIcon("background.jpeg").getImage();

        addKeyListener(this);
        addMouseListener(this);

        gameLoop = new Timer(1000 / 360, this);
        robot = new Robot();

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                handleMouseEvent(e);
            }
            public void mouseDragged(MouseEvent e) {
                handleMouseEvent(e);
            }
        });
        sensitivitySlider = new JSlider(JSlider.HORIZONTAL, 10, 300, (int) (sensitivity * scale));
        sensitivitySlider.setOpaque(false);

        sensitivitySlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int min = sensitivitySlider.getMinimum();
                int max = sensitivitySlider.getMaximum();
                int width = sensitivitySlider.getWidth();

                int value = min + (int)((x / (double) width) * (max - min));
                sensitivitySlider.setValue(value);
                draggingSlider = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingSlider = false;
            }
        });
        sensitivitySlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                sensitivity = (double) sensitivitySlider.getValue() / scale;
                sensitivitySlider.setToolTipText(String.format("%.2f", sensitivity));
                sensitivityTF.setText(String.format("%.2f", sensitivity));
            }
        });
        add(sensitivitySlider);

        sensitivityTF = new JTextField(String.format("%.2f", sensitivity));
        sensitivityTF.setColumns(5);
        sensitivityTF.setHorizontalAlignment(JTextField.CENTER);
        sensitivityTF.setFont(new Font("Arial", Font.PLAIN, 20));
        sensitivityTF.setOpaque(false);
        sensitivityTF.setForeground(Color.WHITE);
        sensitivityTF.setBorder(null);
        add(sensitivityTF);
        sensitivityTF.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    double value = Double.parseDouble(sensitivityTF.getText());
                    sensitivity = Math.max(0.01, Math.min(value, 3.0));
                    sensitivitySlider.setValue((int)(sensitivity * scale));
                } catch (NumberFormatException ex) {
                    sensitivityTF.setText(String.format("%.2f", sensitivity));
                }
            }
        });
        sensitivityTF.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                sensitivityTF.requestFocusInWindow();
            }
        });
    }

    private void handleMouseEvent(MouseEvent e) {
        if (gameOver) {
            mouseDeltaX = 0;
            mouseDeltaY = 0;
            return;
        }
        if (isPaused) {
            mouseDeltaX = 0;
            mouseDeltaY = 0;
            return;
        }

        if (recentering) {
            recentering = false;
            return;
        }
        calculateDeltaMousemovement(e.getX(), e.getY());
        Point p = getLocationOnScreen();
        recentering = true;
        robot.mouseMove(p.x + centerX, p.y + centerY);
    }

    void placeTarget() {
        Block target = new Block(targetX, targetY, targetWidth, targetHeight, targetImg);
        targetList.add(target);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int viewWidth = (int) (getWidth() / zoom);
        int viewHeight = (int) (getHeight() / zoom);

        // background
        g2d.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(),
                cameraX, cameraY, cameraX + viewWidth, cameraY + viewHeight,
                null);
        sensitivitySlider.setVisible(false);
        sensitivityTF.setVisible(false);
        if (gameOver) {
            // dark overlay
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // === GAME OVER TITLE ===
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            FontMetrics fm = g2d.getFontMetrics();

            String title = "GAME OVER";
            int titleX = (getWidth() - fm.stringWidth(title)) / 2;
            int titleY = getHeight() / 2 - 120;
            g2d.drawString(title, titleX, titleY);

            // === YOUR SCORE ===
            g2d.setFont(new Font("Arial", Font.PLAIN, 30));
            fm = g2d.getFontMetrics();

            String yourScore = "Your score: " + score;
            int yourScoreX = (getWidth() - fm.stringWidth(yourScore)) / 2;
            int yourScoreY = getHeight() / 2 - 60;
            g2d.drawString(yourScore, yourScoreX, yourScoreY);

            // === HIGHEST SCORE ===
            int best = getHighestScore();
            String bestScore = "Highest score: " + best;
            int bestX = (getWidth() - fm.stringWidth(bestScore)) / 2;
            int bestY = getHeight() / 2 - 20;
            g2d.drawString(bestScore, bestX, bestY);

            // === BUTTONS ===
            int bw = 200;
            int bh = 50;
            int bx = getWidth() / 2 - bw / 2;

            // Restart button
            int byRestart = getHeight() / 2 + 40;
            g2d.drawRect(bx, byRestart, bw, bh);
            g2d.drawString("Restart",
                    bx + (bw - fm.stringWidth("Restart")) / 2,
                    byRestart + 35);
            restartButton.setBounds(bx, byRestart, bw, bh);

            // Leave button
            int byLeave = getHeight() / 2 + 110;
            g2d.drawRect(bx, byLeave, bw, bh);
            g2d.drawString("Leave",
                    bx + (bw - fm.stringWidth("Leave")) / 2,
                    byLeave + 35);
            leaveButton.setBounds(bx, byLeave, bw, bh);

            return;
        }
        if (isPaused) {

            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.drawString("PAUSED", getWidth() / 2 - 80, getHeight() / 2 - 120);

            g2d.setFont(new Font("Arial", Font.PLAIN, 30));

            g2d.drawRect(getWidth() / 2 - 100, getHeight() / 2 - 70, 200, 50);
            g2d.drawString("Resume", getWidth() / 2 - 55, getHeight() / 2 - 35);

            g2d.drawRect(getWidth() / 2 - 100, getHeight() / 2, 200, 50);
            g2d.drawString("Restart", getWidth() / 2 - 50, getHeight() / 2 + 35);

            g2d.drawRect(getWidth() / 2 - 100, getHeight() / 2 + 70, 200, 50);
            g2d.drawString("Leave", getWidth() / 2 - 45, getHeight() / 2 + 105);

            resumeButton.setBounds(getWidth() / 2 - 100, getHeight() / 2 - 70, 200, 50);
            restartButton.setBounds(getWidth() / 2 - 100, getHeight() / 2, 200, 50);
            leaveButton.setBounds(getWidth() / 2 - 100, getHeight() / 2 + 70, 200, 50);

            int sliderWidth = 300;
            int sliderHeight = 30;
            int sx = getWidth() / 2 - sliderWidth / 2;
            int sy = getHeight() / 2 + 160;
            sensitivitySlider.setBounds(sx, sy, sliderWidth, sliderHeight);
            sensitivitySlider.setVisible(true);

            g2d.setFont(new Font("Arial", Font.BOLD, 25));
            g2d.drawString("Sensivity", sx - 110, sy + sliderHeight / 2);
            if (draggingSlider) {
                g2d.setFont(new Font("Arial", Font.BOLD, 15));

                int sliderX = sensitivitySlider.getX();
                int sliderY = sensitivitySlider.getY();
                int knobX = sliderX + sensitivitySlider.getValue() * sliderWidth / sensitivitySlider.getMaximum();

                g2d.drawString(String.format("%.2f", sensitivity), knobX - 10, sliderY - 10);
            }
            int fieldWidth = 50;
            int fieldHeight = 30;
            int tx = sx + sliderWidth + 10;
            sensitivityTF.setBounds(tx, sy, fieldWidth, fieldHeight);
            sensitivityTF.setVisible(true);
            return;
        }
        for (Block target : targetList) {
            g2d.drawImage(target.getImage(), target.getX() - cameraX, target.getY() - cameraY, null);
        }

        int fontSize = Math.max(6, getWidth() / 50);
        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
        FontMetrics fm = g2d.getFontMetrics();
        String scoreText = "Score: " + score;
        int scoreX = (getWidth() - fm.stringWidth(scoreText)) / 2;
        int scoreY = fm.getHeight();
        g2d.drawString(scoreText, scoreX, scoreY);

        String timeText = "Time Remaining: " + timeRemaining;
        int timeX = getWidth() - fm.stringWidth(timeText) - 10;
        int timeY = fm.getHeight();
        g2d.drawString(timeText, timeX, timeY);

        // crosshair
        int thickness = 3;
        int size = 10;
        int gap = 8;

        g2d.setColor(Color.cyan);
        g2d.fillRect(centerX - thickness / 2, centerY - gap / 2 - size, thickness, size);
        g2d.fillRect(centerX - thickness / 2, centerY + gap / 2, thickness, size);
        g2d.fillRect(centerX - gap / 2 - size, centerY - thickness / 2, size, thickness);
        g2d.fillRect(centerX + gap / 2, centerY - thickness / 2, size, thickness);


    }


    boolean collision(Block a) {
        return a.getBounds().contains(centerPoint);
    }

    long start = System.currentTimeMillis();

    void update() {
        long elapsed = System.currentTimeMillis() - start - totalPausedTime;
        timeRemaining = Math.toIntExact((gameTimeLimit - elapsed));
        if (elapsed >= gameTimeLimit) {
            gameOver = true;
            setCursor(Cursor.getDefaultCursor());
            return;
        }
        if (isPaused) {
            setCursor(Cursor.getDefaultCursor());
            return;
        }
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blank = toolkit.createCustomCursor(img, new Point(0, 0), "blank");
        setCursor(blank);

        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        cameraX += (int) (mouseDeltaX * sensitivity);
        cameraY += (int) (mouseDeltaY * sensitivity);
        centerPoint = new Point(centerX + cameraX, centerY + cameraY);

        int viewWidth = (int) (getWidth() / zoom);
        int viewHeight = (int) (getHeight() / zoom);
        int screenMaxX = backgroundImg.getWidth(null) - viewWidth;
        int screenMaxY = backgroundImg.getHeight(null) - viewHeight;
        cameraX = Math.max(0, Math.min(cameraX, screenMaxX));
        cameraY = Math.max(0, Math.min(cameraY, screenMaxY));

        int bgWidth = backgroundImg.getWidth(null);
        int bgHeight = backgroundImg.getHeight(null);

        int marginLeft = (int) (bgWidth / 2.25);
        int marginRight = (int) (bgWidth / 2.25);

        int marginTop = bgHeight / 3;
        int marginBottom = (int) (bgHeight / 2.1);

        int usableWidth = bgWidth - marginLeft - marginRight;
        int usableHeight = bgHeight - marginTop - marginBottom;

        if (targetList.isEmpty() && getWidth() > 0 && getHeight() > 0) {
            targetX = rnd.nextInt(usableWidth - targetWidth) + marginLeft;
            targetY = rnd.nextInt(usableHeight - targetHeight) + marginTop;
            placeTarget();
        }

        if (!targetList.isEmpty() && collision(targetList.getFirst()) && isClicked) {
            score++;
            targetList.clear();
        }

        if (isClicked) {
            isClicked = false;
        }

        mouseDeltaX = 0;
        mouseDeltaY = 0;
    }

    private void calculateDeltaMousemovement(int mouseX, int mouseY) {
        mouseDeltaX = mouseX - centerX;
        mouseDeltaY = mouseY - centerY;
    }

    ArrayList<String> loadScores() {
        ArrayList<String> list = new ArrayList<>();
        try {
            Scanner sc = new Scanner(new File("scores.txt"));
            while (sc.hasNextLine()) {
                list.add(sc.nextLine());
            }
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addNotify() {
        super.addNotify();
        gameLoop.start();
        int bgWidth = backgroundImg.getWidth(null);
        int bgHeight = backgroundImg.getHeight(null);

        int viewWidth = (int)(getWidth() / zoom);
        int viewHeight = (int)(getHeight() / zoom);

        cameraX = (bgWidth - viewWidth) / 2;
        cameraY = (bgHeight - viewHeight) / 2;
    }

    private void saveScore(int score) {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            FileWriter fw = new FileWriter("scores.txt", true);
            fw.write(score + ";" + now.format(fmt) + "\n");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getHighestScore() {
        ArrayList<String> scores = loadScores();
        int max = 0;

        for (String line : scores) {
            String[] parts = line.split(";");
            int s = Integer.parseInt(parts[0]);
            if (s > max) max = s;
        }

        return max;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
        if (gameOver) {
            gameLoop.stop();
            saveScore(score);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        int mX = e.getX();
        int mY = e.getY();
        if (isPaused) {
            if (resumeButton.contains(mX, mY)) {
                isPaused = false;
            } else if (restartButton.contains(mX, mY)) {
                restartGame();
            } else if (leaveButton.contains(mX, mY)) {
                System.exit(0);
            }
            return;
        }
        if (gameOver) {
            if (restartButton.contains(mX, mY)) {
                restartGame();
            } else if (leaveButton.contains(mX, mY)) {
                System.exit(0);
            }
        }
        if (e.getButton() == MouseEvent.BUTTON1) {
            isClicked = true;
        }
    }

    private void restartGame() {
        score = 0;
        targetList.clear();

        int bgWidth = backgroundImg.getWidth(null);
        int bgHeight = backgroundImg.getHeight(null);
        int viewWidth = (int)(getWidth() / zoom);
        int viewHeight = (int)(getHeight() / zoom);

        cameraX = (bgWidth - viewWidth) / 2;
        cameraY = (bgHeight - viewHeight) / 2;

        start = System.currentTimeMillis();
        gameOver = false;
        isPaused = false;
        gameLoop.start();
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

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            isPaused = !isPaused;

            if (isPaused) {
                pauseStartTime = System.currentTimeMillis();
            } else {
                totalPausedTime += System.currentTimeMillis() - pauseStartTime;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
