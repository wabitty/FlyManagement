import javax.swing.*;
import java.awt.Image;

public class User {
    private final int userId;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final byte[] profileImage;

    public User(int userId, String email, String firstName, String lastName, byte[] profileImage) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileImage = profileImage != null ? profileImage.clone() : null;
    }

    public ImageIcon getProfileImage(int width, int height) {
        try {
            Image baseImage;
            if (profileImage == null) {
                baseImage = new ImageIcon(getClass().getResource("/default_avatar.png")).getImage();
            } else {
                baseImage = new ImageIcon(profileImage).getImage();
            }
            return new ImageIcon(baseImage.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            return new ImageIcon();
        }
    }

    public int getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}