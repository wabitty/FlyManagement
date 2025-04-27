import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new LoginForm().setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Startup error: " + ex.getMessage());
            }
        });
    }
}