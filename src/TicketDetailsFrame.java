import javax.swing.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class TicketDetailsFrame extends JFrame {
    private JLabel flightNumberLabel;
    private JLabel routeLabel;
    private JLabel departureLabel;
    private JLabel arrivalLabel;
    private JLabel seatLabel;
    private JLabel priceLabel;
    private JLabel classLabel;
    private JLabel profileImageLabel;
    private JButton closeButton;
    private JPanel mainPanel;

    public TicketDetailsFrame(User user, int flightId, String seat) {
        setContentPane(mainPanel);
        setTitle("Ticket Confirmation");
        setSize(500, 400);
        setLocationRelativeTo(null);

        profileImageLabel.setIcon(user.getProfileImage(50, 50));

        try {
            ResultSet rs = DBConnection.getFlightDetails(flightId);
            if (rs != null && rs.next()) {
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d yyyy HH:mm");

                flightNumberLabel.setText("BA" + flightId);
                routeLabel.setText(rs.getString("departure_city") + " → " + rs.getString("destination_city"));
                departureLabel.setText(sdf.format(rs.getTimestamp("departure_time")));
                arrivalLabel.setText(sdf.format(rs.getTimestamp("arrival_time")));
                seatLabel.setText(seat);
                priceLabel.setText(String.format("€%.2f", rs.getDouble("price")));
                classLabel.setText(rs.getBoolean("is_exclusive") ? "Business ⭐" : "Economy");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading ticket details");
        }

        closeButton.addActionListener(e -> dispose());
    }
}