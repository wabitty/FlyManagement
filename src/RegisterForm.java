import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class RegisterForm extends JFrame {
    private JTextField emailField;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JPasswordField passwordField;
    private JButton registerBtn;
    private JButton backBtn;
    private JButton selectImageBtn;
    private JLabel imageLabel;
    private JLabel Email;
    private JPanel mainPanel;

    private byte[] imageBytes;

    public RegisterForm() {
        setTitle("Bulgaria Air - Register");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);

        selectImageBtn.addActionListener(e -> selectImage());
        registerBtn.addActionListener(e -> performRegistration());
        backBtn.addActionListener(e -> {
            new LoginForm().setVisible(true);
            dispose();
        });
    }

    private void selectImage() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                BufferedImage originalImage = ImageIO.read(file);
                imageBytes = DBConnection.resizeImage(originalImage);
                imageLabel.setIcon(new ImageIcon(originalImage.getScaledInstance(150, 150, Image.SCALE_SMOOTH)));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage());
            }
        }
    }

    private void performRegistration() {
        if (imageBytes == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile image");
            return;
        }
        if (emailField.getText().isEmpty() || firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required");
            return;
        }
        if (!emailField.getText().matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            JOptionPane.showMessageDialog(this, "Invalid email format");
            return;
        }
        if (new String(passwordField.getPassword()).length() < 8) {
            JOptionPane.showMessageDialog(this, "Password must be at least 8 characters");
            return;
        }
        try {
            if (DBConnection.register(
                    emailField.getText(),
                    firstNameField.getText(),
                    lastNameField.getText(),
                    new String(passwordField.getPassword()),
                    imageBytes
            )) {
                JOptionPane.showMessageDialog(this, "Registration successful!");
                new LoginForm().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}