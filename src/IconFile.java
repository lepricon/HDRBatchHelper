import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;

class IconFile implements Icon {
    private File file;
    private int width;
    private int height;
    Image image;
    private Icon icon;
    private int iconIndex;
    private ImageObserver imageObserver = null;
    
    public IconFile(String s, int i, int previewSize, MediaTracker mTracker) {
        iconIndex = i;
        file = new File(s);
        image = Toolkit.getDefaultToolkit().getImage(s);
        width = height = previewSize;

        synchronized (mTracker) {
            mTracker.addImage(image, iconIndex, width, height);
            try {
                mTracker.waitForID(iconIndex);
            } catch (InterruptedException e) {
                System.out.println("INTERRUPTED while loading Image");
            }
            mTracker.removeImage(image);
        }
        
        // ensure proper scaling
        if (image.getWidth(imageObserver) > image.getHeight(imageObserver)) {
            width = previewSize;
            height = -1;
        } else {
            height = previewSize;
            width = -1;
        }

        image = image.getScaledInstance(width, height, Image.SCALE_FAST);

        // very strange thing: w/o following line, the icons are empty
        icon = new ImageIcon(image);
    }
    
    public Icon getIcon() {
        return icon;
    }
    
    public void setFile(File f) {
        file = f;
    }
    
    public File getFile() {
        return file;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (imageObserver == null) {
            g.drawImage(image, x, y, c);
        } else {
            g.drawImage(image, x, y, imageObserver);
        }
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}
