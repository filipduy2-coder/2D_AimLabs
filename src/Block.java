import java.awt.*;

/**
 * Represents the block entity with position, size and image.
 */
public class Block {
    private int x, y, width, height;
    private Image image;

    public Block(int x, int y, int width, int height, Image image) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.image = image;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Image getImage() { return image; }
}