import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

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
}
