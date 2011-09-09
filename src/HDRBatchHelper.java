import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

/**
 * @author Volodymyr Volkov
 */
public class HDRBatchHelper extends JPanel implements ActionListener {
    static final long serialVersionUID = -8653871707049477151L;
    static final int windowHeight = 600;
    static final int windowWidth = 1000;
    static final int previewSize = 100;
    static final int photoPadding = 15;
    static final int maxProcessingThreads = 3;
    final DefaultListModel model = new DefaultListModel();
    String currentDirectiory = new String("/home/volodymyr/Photos/");
    
    JList photosList = new JList(model);
    JFrame frame = new JFrame("HDR Batch Helper");
    JLabel status = new JLabel(currentDirectiory);
    JLabel queueStatus = new JLabel("Queue status: 0/0");
    JProgressBar progress = new JProgressBar();
    JPanel statusPanel = new JPanel();
    JScrollPane listScroller = new JScrollPane(photosList);
    JMenuBar menuBar = new JMenuBar();
    JMenuItem menuItemOpen = new JMenuItem("Open");
    JMenuItem menuItemProcess = new JMenuItem("Process");
    ImageCellRenderer renderer = new ImageCellRenderer();

    MediaTracker mTracker = new MediaTracker(photosList);
    
    LinkedList< HDRProcessingThread > processingQueue = new LinkedList< HDRProcessingThread >();
    ProcessorsObserver processingActivator = new ProcessorsObserver(); 
    
    public HDRBatchHelper() {
        renderer.setPreferredSize(new Dimension(previewSize + photoPadding, previewSize + photoPadding));

        listScroller.setPreferredSize(new Dimension(windowWidth, windowHeight));
        
        photosList.setCellRenderer(renderer);
        photosList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        photosList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        photosList.setVisibleRowCount(-1);

        menuBar.add(menuItemOpen);
        menuBar.add(menuItemProcess);
        menuItemOpen.addActionListener(this);
        menuItemProcess.addActionListener(this);

        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.LINE_AXIS));
        statusPanel.add(status);
        statusPanel.add(Box.createHorizontalGlue());
        statusPanel.add(queueStatus);
        statusPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        statusPanel.add(progress);
        
        frame.setJMenuBar(menuBar);
        frame.setLayout(new BorderLayout());
        frame.add(BorderLayout.CENTER, listScroller);
        frame.add(BorderLayout.SOUTH, statusPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack(); // sets appropriate size for frame
        frame.setVisible(true);
        
        loadPhotoFiles(currentDirectiory);
        //actionPerformed(new ActionEvent(this, 0, "Open"));
    }

    class ProcessorsObserver implements Observer {
        private int runningThreads = 0;

        public void activateProcessing() {
            synchronized (this) {
                int i = 0;
                while ((i < maxProcessingThreads - runningThreads) && nextHdrStarted()) {
                    runningThreads++;
                    i++;
                }
            }
            progress.setValue(runningThreads);
            progress.setMaximum(runningThreads + processingQueue.size());
            queueStatus.setText("Queue status: " + Integer.toString(runningThreads) + "/"
                                + Integer.toString(runningThreads + processingQueue.size()));
        }

        @Override
        public void update(Observable o, Object arg) {
            runningThreads--;
            activateProcessing();
        }
    }
    
    class JpegFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.toLowerCase(Locale.ENGLISH).endsWith(".jpeg") ||
                    name.toLowerCase(Locale.ENGLISH).endsWith(".jpg"));
        }
    }
    
    class ImageLoader extends Thread {
        String[] fileNames;
        ImageLoader(String[] _fileNames) {
            fileNames = _fileNames;
        }

        @Override
        public void run() {
            model.clear();
            for (int i = 0; i < fileNames.length; i++) {
                model.addElement( new IconFile(currentDirectiory + "/" + fileNames[i], i, previewSize, mTracker) );
                setStatus("Items to load: " + String.valueOf(i) + "/" + String.valueOf(fileNames.length) + 
                          " [" + currentDirectiory + "*]");
            }
            setStatus("Items loaded: " + String.valueOf(fileNames.length) + " [" + currentDirectiory + "*]");            
        }
    }

    private int loadPhotoFiles(String directory) {
        File files = new File(directory);
        FilenameFilter filter = new JpegFilter();
        String[] fileNames = files.list(filter);
        Arrays.sort(fileNames);
        if (fileNames != null) {
	        ImageLoader loader = new ImageLoader(fileNames);
	        loader.start();
        }
        return fileNames.length;
    }
    
    public static void main(String[] args) {
        new HDRBatchHelper();
    }

    public void setStatus(String s) {
        status.setText(s);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "Open") {
            JFileChooser fc = new JFileChooser(currentDirectiory);
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File directory = fc.getSelectedFile();
                currentDirectiory = directory.getPath() + "/";
                CharSequence chSeq = new String(" ");
                if (! currentDirectiory.contains(chSeq)) {
                    setStatus("loading images...");
                    loadPhotoFiles(currentDirectiory);
                } else {
                    setStatus("Sorry, but the program does not support spases in dirnames for now...");
                }
            }
        } else if (e.getActionCommand() == "Process") {
            (new File( currentDirectiory + "tmp/" )).mkdir();
        	Object[] selectedImages = photosList.getSelectedValues();
        	if (selectedImages.length > 1) {
        	    IconFile[] images = new IconFile[selectedImages.length];
        		for (int i = 0; i < selectedImages.length; ++i) {
        			images[i] = (IconFile)selectedImages[i];
        			model.removeElement(selectedImages[i]);
        			mTracker.removeImage(((IconFile)selectedImages[i]).image);
        		}

        		HDRProcessingThread hdrSequence = new HDRProcessingThread(images, currentDirectiory);
        		hdrSequence.addObserver(processingActivator);
        		
            	synchronized (processingQueue) {
            		processingQueue.addFirst(hdrSequence);
            	}
          	    processingActivator.activateProcessing();

        	} else {
        		setStatus("You must select more than one image!");
        	}
        }
    }
    
	private boolean nextHdrStarted() {
	    HDRProcessingThread hdrTread;
	    synchronized (processingQueue) {
	        hdrTread = processingQueue.pollFirst();
        }
	    if (hdrTread != null) {
	        // copy images for processing into #next directory
	        File processingDir = new File(currentDirectiory + "tmp/" + hdrTread.id + "/");
	        processingDir.mkdir();
	        for ( IconFile imageFile : hdrTread.images ) {
	            File movedImageFile = new File(processingDir + "/" + imageFile.getFile().getName());
	            if (imageFile.getFile().renameTo(movedImageFile)) {
	                imageFile.setFile(movedImageFile);
	            } else {
	                System.out.println("Error while renaming an image: " + imageFile.getFile().getPath());
	            }
	        }

	        new Thread(hdrTread).start();
	        return true;
	    } else {
	        System.out.println("Popping an element from empty processingQueue...");
	    }
	    return false;
	}
}
