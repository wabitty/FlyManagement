import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ManageBookingsFrame extends JFrame {
    private JTable bookingsTable;
    private JButton cancelButton;
    private JButton updateButton;
    private JTextField newSeatField;
    private JComboBox<String> flightComboBox;
    private JPanel mainPanel;
    private JButton backButton;
    private JPanel Header;
    private JPanel UpdateField;

    private final User currentUser;
    private DefaultTableModel tableModel;

    public ManageBookingsFrame(User user) {
        currentUser = user;
        setContentPane(mainPanel);
        setTitle("Manage Bookings");
        setSize(600, 500);
        setLocationRelativeTo(null);

        initializeTable();
        loadBookings();
        loadFlightOptions();
        setupListeners();
    }

    private void initializeTable() {
        tableModel = new DefaultTableModel(
                new Object[]{"Ticket ID", "Flight No", "From", "To", "Departure", "Seat", "Class"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookingsTable.setModel(tableModel);
    }

    private void loadBookings() {
        tableModel.setRowCount(0);
        String query = "SELECT t.ticket_id, f.flight_id, f.departure_city, f.destination_city, " +
                "f.departure_time, t.seat_number, f.is_exclusive " +
                "FROM tickets t JOIN flights f ON t.flight_id = f.flight_id " +
                "WHERE t.user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, currentUser.getUserId());
            ResultSet rs = pstmt.executeQuery();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("ticket_id"),
                        "BA" + rs.getInt("flight_id"),
                        rs.getString("departure_city"),
                        rs.getString("destination_city"),
                        sdf.format(rs.getTimestamp("departure_time")),
                        rs.getString("seat_number"),
                        rs.getBoolean("is_exclusive") ? "Business" : "Economy"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading bookings: " + e.getMessage());
        }
    }

    private void loadFlightOptions() {
        flightComboBox.removeAllItems();
        try {
            ArrayList<Integer> flights = DBConnection.getAvailableFlights();
            for (Integer flightId : flights) {
                flightComboBox.addItem("BA" + flightId);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading flights: " + e.getMessage());
        }
    }

    private void setupListeners() {
        cancelButton.addActionListener(e -> cancelBooking());
        updateButton.addActionListener(e -> updateBooking());
        backButton.addActionListener(e -> {
            new WelcomeFrame(currentUser).setVisible(true);
            dispose();
        });
    }

    private void cancelBooking() {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking to cancel");
            return;
        }

        int ticketId = (int) tableModel.getValueAt(selectedRow, 0);
        try {
            if (DBConnection.cancelTicket(ticketId)) {
                JOptionPane.showMessageDialog(this, "Booking canceled successfully");
                loadBookings();
                loadFlightOptions();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Cancel failed: " + ex.getMessage());
        }
    }

    private void updateBooking() {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking to update");
            return;
        }

        String newSeat = newSeatField.getText().trim().toUpperCase();
        String selectedFlight = (String) flightComboBox.getSelectedItem();

        if (selectedFlight == null) {
            JOptionPane.showMessageDialog(this, "Please select a flight");
            return;
        }

        // Seat validation
        if (newSeat.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a seat number");
            return;
        }

        if (!newSeat.matches("^[A-Z][0-9]+$")) {
            JOptionPane.showMessageDialog(this,
                    "Invalid seat format!\nMust start with a letter followed by numbers\nExample: A12");
            return;
        }

        try {
            String seatNumberPart = newSeat.replaceAll("[^0-9]", "");
            int seatNumber = Integer.parseInt(seatNumberPart);
            if (seatNumber < 1) {
                JOptionPane.showMessageDialog(this, "Seat number must be at least 1");
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid seat number format");
            return;
        }

        int ticketId = (int) tableModel.getValueAt(selectedRow, 0);
        int newFlightId = Integer.parseInt(selectedFlight.replace("BA", ""));

        try {
            if (DBConnection.updateBooking(ticketId, newFlightId, newSeat)) {
                JOptionPane.showMessageDialog(this, "Booking updated successfully");
                loadBookings();
                loadFlightOptions();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage());
        }
    }
}