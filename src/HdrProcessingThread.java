import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Observable;

class HDRProcessingThread extends Observable implements Runnable {
    int id = 0;
    IconFile[] images;
    private Process process;
    private String currentDirectiory;

    HDRProcessingThread(IconFile[] i, String directory) {
        images = i;
        id++;
        currentDirectiory = directory;
    }
    
    public void run() {
        try {
            String[] command = new String[]{"hdr.sh", images[0].getFile().getParent(), String.valueOf(id)};
            process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }

            process.waitFor();
            cleanupTmp();

        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e2) {
            e2.printStackTrace();
        }
        setChanged();
        notifyObservers();
    }
    
    private void cleanupTmp() {
        // search for free id for file name
        int extraId = 0;
        String hdrFilename = new String("hdr" + id + ".jpg");
        File movedHrdBack = new File(currentDirectiory + hdrFilename);
        while (movedHrdBack.exists()) {
            hdrFilename = new String("hdr" + id + "_" + extraId + ".jpg");
            movedHrdBack = new File(currentDirectiory + hdrFilename);
            extraId++;
        }
        
        // move hdrXX.jpg back
        File createdHdr = new File(currentDirectiory + "tmp/" + String.valueOf(id) + "/" + hdrFilename);
        createdHdr.renameTo(movedHrdBack);
        
        // deleting original photos
        File tmpDirectory = new File(currentDirectiory + "tmp/" + String.valueOf(id));
        FilenameFilter filterAllFiles = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return true;
            }
        };
        String[] fileNames = tmpDirectory.list(filterAllFiles);
        for (String filename : fileNames) {
            String tmpFile = new String(tmpDirectory.getPath() + "/" + filename);
            if ( !(new File(tmpFile)).delete() ) {
                System.out.println("Error deleting file: " + tmpFile);
            }
        }
        tmpDirectory.delete();
        
        // delete .../tmp/
        // actual deletion will be done when the last image will finished processing
        (new File(tmpDirectory.getParent())).delete();
    }
}
