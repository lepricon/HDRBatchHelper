import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.ImageObserver;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Locale;

/**
 * 
 * @author Volodymyr Volkov
 *
 */
public class HDRBatchHelper extends JPanel implements ActionListener {
    private static final long serialVersionUID = -8653871707049477151L;
    private static final int windowSize = 600;
    private static final int previewSize = 100;
    private static final int photoPadding = 15;
    final DefaultListModel model = new DefaultListModel();
    private String currentDirectiory = new String("/home/volodymyr/Photos/");
    
    JList photosList = new JList(model);
    JFrame frame = new JFrame("HDR Batch Helper");
    JLabel status = new JLabel(currentDirectiory);
    JScrollPane listScroller = new JScrollPane(photosList);
    JMenuBar menuBar = new JMenuBar();
    JMenuItem menuItemOpen = new JMenuItem("Open");
    JMenuItem menuItemProcess = new JMenuItem("Process");
    ImageCellRenderer renderer = new ImageCellRenderer();

    // TODO implement load balancing using the queue
    LinkedList< HDRProcessingThread > processingQueue = new LinkedList< HDRProcessingThread >();
    public static int sequenceId = 0;
    File temporaryDirectory;
    
    public HDRBatchHelper() {
        renderer.setPreferredSize(new Dimension(previewSize + photoPadding, previewSize + photoPadding));

        listScroller.setPreferredSize(new Dimension(windowSize, windowSize));
        
        photosList.setCellRenderer(renderer);
        photosList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        photosList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        photosList.setVisibleRowCount(-1);

        menuBar.add(menuItemOpen);
        menuBar.add(menuItemProcess);
        menuItemOpen.addActionListener(this);
        menuItemProcess.addActionListener(this);

        frame.setJMenuBar(menuBar);
        frame.setLayout(new BorderLayout());
        frame.add(BorderLayout.CENTER, listScroller);
        frame.add(BorderLayout.SOUTH, status);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack(); // sets appropriate size for frame
        frame.setVisible(true);
        
        loadPhotoFiles(currentDirectiory);
        //actionPerformed(new ActionEvent(this, 0, "Open"));
    }

    class JpegFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.toLowerCase(Locale.ENGLISH).endsWith(".jpeg") ||
                    name.toLowerCase(Locale.ENGLISH).endsWith(".jpg"));
        }
    }
    
    private void loadPhotoFiles(String directory) {
        File files = new File(directory);
        FilenameFilter filter = new JpegFilter();
        String[] fileNames = files.list(filter);
        if (fileNames != null) {
	        model.clear();
	        for (int i = 0; i < fileNames.length; i++) {
	            model.addElement( new IconFile( currentDirectiory + "/" + fileNames[i]) );
	        }
        }
    }
    
    public static void main(String[] args) {
        new HDRBatchHelper();
    }

    public void setStatus(String s) {
        status.setText(s);

    }
    
    class ImageCellRenderer extends JLabel implements ListCellRenderer {
        private static final long serialVersionUID = -8341045428412639868L;

        public ImageCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }

        public Component getListCellRendererComponent(
                JList list, 
                Object value, 
                int index, 
                boolean isSelected,
                boolean cellHasFocus) {
            
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            
            IconFile selectedImage = (IconFile)value;
            setIcon( selectedImage.getIcon() );
            return this;
        }
    };

    class IconFile implements Icon {
        private File file;
        private int width;
        private int height;
        private Image imageScaled;
        private Icon icon;
        private ImageObserver imageObserver = null;
        
        public IconFile(String s) {
            file = new File(s);
            imageScaled = loadImage(s);
            
            // very strange thing: w/o following line, the icons are empty
            icon = new ImageIcon(imageScaled);
        }

        private Image loadImage(String name) {
            Image image = Toolkit.getDefaultToolkit().createImage(name);
            Component comp = new Component() {
                    private static final long serialVersionUID = 5242197476830270365L;
                };
            int id = 0;
            MediaTracker mTracker = new MediaTracker(comp);
            synchronized (mTracker) {
                mTracker.addImage(image, id);
                try {
                    mTracker.waitForID(id);
                } catch (InterruptedException e) {
                    System.out.println("INTERRUPTED while loading Image");
                }
                mTracker.removeImage(image, id);
            }
            // ensure proper scaling
            int imageWidth = image.getWidth(imageObserver);
            int imageHeight = image.getHeight(imageObserver);
            if (imageWidth > imageHeight) {
                width = previewSize;
                height = width * imageHeight / imageWidth;
            } else {
                height = previewSize;
                width = height * imageWidth / imageHeight;
            }
            
            return image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
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
                g.drawImage(imageScaled, x, y, c);
            } else {
                g.drawImage(imageScaled, x, y, imageObserver);
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
    };

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "Open") {
            JFileChooser fc = new JFileChooser(currentDirectiory);
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File directory = fc.getSelectedFile();
                currentDirectiory = directory.getPath();
                setStatus(currentDirectiory);
                loadPhotoFiles(currentDirectiory);
            }
        } else if (e.getActionCommand() == "Process") {
        	createTemporaryDirectory();
        	Object[] selectedImages = photosList.getSelectedValues();
        	if (selectedImages.length > 0) {
        	    IconFile[] images = new IconFile[selectedImages.length];
        		for (int i = 0; i < selectedImages.length; ++i) {
        			images[i] = (IconFile)selectedImages[i];
        			model.removeElement(selectedImages[i]);
        		}

        		HDRProcessingThread hdrSequence = new HDRProcessingThread(images, sequenceId++);
        		
            	synchronized (processingQueue) {
            		processingQueue.addFirst(hdrSequence);
            	}
            	processHdrSequence();
            	
        	} else {
        		setStatus("You must select something!");
        	}
        }
    }
    
    class HDRProcessingThread extends Thread {
        public int sequenceId;
        private IconFile[] images;
        private Process process;
        
        HDRProcessingThread(IconFile[] i, int id) {
            images = i;
            sequenceId = id;
        }
        
        public void run() {
            try {
                String[] command = new String[]{"hdr.sh", images[0].getFile().getParent()};
                process = Runtime.getRuntime().exec(command);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                while (line != null) {
                    System.out.println(line);
                    line = reader.readLine();
                }

                process.waitFor();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
        }
    }
    
	private void processHdrSequence() {
	    HDRProcessingThread hdrTread;
	    synchronized (processingQueue) {
	        hdrTread = processingQueue.pollFirst();
        }
	    if (hdrTread != null) {
	        // copy images for processing
	        File processingDir = new File(temporaryDirectory + "/" + hdrTread.sequenceId + "/");
	        processingDir.mkdir();
	        for ( IconFile imageFile : hdrTread.images ) {
	            File movedImageFile = new File(processingDir + "/" + imageFile.getFile().getName());
	            if (imageFile.getFile().renameTo(movedImageFile)) {
	                imageFile.setFile(movedImageFile);
	            } else {
	                System.out.println("Error while renaming an image: " + imageFile.getFile().getPath());
	            }
	        }
	        
	        hdrTread.start();
	    } else {
	        System.out.println("Popping an element fro mempty processingQueue...");
	    }
	}
    
    private void createTemporaryDirectory() {
		temporaryDirectory = new File( currentDirectiory + "/" + "tmp" );
		temporaryDirectory.mkdir();
		System.out.println(temporaryDirectory.getPath() + " created");
    }
}
