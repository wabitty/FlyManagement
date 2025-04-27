import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WelcomeFrame extends JFrame {
    private JTable flightsTable;
    private JButton bookBtn;
    private JButton logoutBtn;
    private JButton clearFiltersBtn;
    private JComboBox<String> departureFilter;
    private JComboBox<String> arrivalFilter;
    private JCheckBox exclusiveFilter;
    private JComboBox<Date> dateFilter;
    private JPanel mainPanel;
    private JLabel profileImageLabel;
    private JLabel welcomeLabel;
    private JButton manageBookingsBtn;
    private JLabel titleLabel;
    private JButton requestFlightBtn;

    private final User currentUser;

    public WelcomeFrame(User user) {
        currentUser = user;
        setTitle("Bulgaria Air - Welcome " + user.getFirstName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);

        configureDateRenderer();
        updateUserDisplay();
        loadFilterData();
        loadFlights(new HashMap<>());
        addEventListeners();
        addRequestFlightButton();
    }

    private void addRequestFlightButton() {
        requestFlightBtn.addActionListener(e -> {
            new FlightRequestDialog((JFrame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
        });
    }

    private void configureDateRenderer() {
        dateFilter.setRenderer(new DefaultListCellRenderer() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d yyyy");
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                value = (value instanceof Date) ? sdf.format(value) : "Any Date";
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }

    private void updateUserDisplay() {
        profileImageLabel.setIcon(currentUser.getProfileImage(40, 40));
        welcomeLabel.setText("Welcome, " + currentUser.getFirstName());
    }

    private void loadFilterData() {
        loadComboData("SELECT DISTINCT departure_city FROM flights", departureFilter);
        loadComboData("SELECT DISTINCT destination_city FROM flights", arrivalFilter);
        loadDateOptions();
    }

    private void loadComboData(String query, JComboBox<String> combo) {
        combo.removeAllItems();
        combo.addItem(null);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                combo.addItem(rs.getString(1));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading filter data: " + e.getMessage());
        }
    }

    private void loadDateOptions() {
        dateFilter.removeAllItems();
        dateFilter.addItem(null);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT DISTINCT DATE(departure_time) FROM flights");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                dateFilter.addItem(new Date(rs.getDate(1).getTime()));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading dates: " + e.getMessage());
        }
    }

    private void addEventListeners() {
        ActionListener filterListener = e -> applyActiveFilter();
        departureFilter.addActionListener(filterListener);
        arrivalFilter.addActionListener(filterListener);
        dateFilter.addActionListener(filterListener);
        exclusiveFilter.addActionListener(filterListener);

        clearFiltersBtn.addActionListener(e -> clearFilters());
        bookBtn.addActionListener(e -> bookFlight());
        logoutBtn.addActionListener(e -> {
            new LoginForm().setVisible(true);
            dispose();
        });
        manageBookingsBtn.addActionListener(e -> {
            new ManageBookingsFrame(currentUser).setVisible(true);
        });
    }

    private void applyActiveFilter() {
        Map<String, Object> filters = new HashMap<>();
        if (departureFilter.getSelectedItem() != null) {
            filters.put("departure", departureFilter.getSelectedItem());
        }
        if (arrivalFilter.getSelectedItem() != null) {
            filters.put("arrival", arrivalFilter.getSelectedItem());
        }
        if (dateFilter.getSelectedItem() != null) {
            filters.put("date", new java.sql.Date(((Date) dateFilter.getSelectedItem()).getTime()));
        }
        filters.put("exclusive", exclusiveFilter.isSelected());
        loadFlights(filters);
    }

    private void clearFilters() {
        departureFilter.setSelectedItem(null);
        arrivalFilter.setSelectedItem(null);
        dateFilter.setSelectedItem(null);
        exclusiveFilter.setSelected(false);
        loadFlights(new HashMap<>());
    }

    private void loadFlights(Map<String, Object> filters) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Flight No", "From", "To", "Departure", "Arrival", "Price", "Seats", "Exclusive"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String query = buildFlightQuery(filters);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            int paramIndex = 1;
            if (filters.containsKey("departure")) {
                pstmt.setString(paramIndex++, (String) filters.get("departure"));
            }
            if (filters.containsKey("arrival")) {
                pstmt.setString(paramIndex++, (String) filters.get("arrival"));
            }
            if (filters.containsKey("date")) {
                pstmt.setDate(paramIndex++, (java.sql.Date) filters.get("date"));
            }
            if (filters.containsKey("exclusive")) {
                pstmt.setBoolean(paramIndex++, (Boolean) filters.get("exclusive"));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d HH:mm");
                while (rs.next()) {
                    model.addRow(new Object[]{
                            "BA" + rs.getInt("flight_id"),
                            rs.getString("departure_city"),
                            rs.getString("destination_city"),
                            sdf.format(rs.getTimestamp("departure_time")),
                            sdf.format(rs.getTimestamp("arrival_time")),
                            String.format("€%.2f", rs.getDouble("price")),
                            rs.getInt("available_seats"),
                            rs.getBoolean("is_exclusive") ? "⭐" : ""
                    });
                }
            }

            flightsTable.setModel(model);
            flightsTable.getColumnModel().getColumn(7).setMaxWidth(60);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading flights: " + e.getMessage());
        }
    }

    private String buildFlightQuery(Map<String, Object> filters) {
        StringBuilder query = new StringBuilder(
                "SELECT flight_id, departure_city, destination_city, " +
                        "departure_time, arrival_time, price, available_seats, is_exclusive " +
                        "FROM flights WHERE available_seats > 0 ");

        if (filters.containsKey("departure")) {
            query.append("AND departure_city = ? ");
        }
        if (filters.containsKey("arrival")) {
            query.append("AND destination_city = ? ");
        }
        if (filters.containsKey("date")) {
            query.append("AND DATE(departure_time) = ? ");
        }
        if (filters.containsKey("exclusive")) {
            query.append("AND is_exclusive = ? ");
        }
        return query.toString();
    }

    private void bookFlight() {
        int selectedRow = flightsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a flight first!");
            return;
        }

        try {
            String flightNumber = (String) flightsTable.getValueAt(selectedRow, 0);
            int flightId = Integer.parseInt(flightNumber.replace("BA", ""));

            String seat = (String) JOptionPane.showInputDialog(
                    this,
                    "Enter seat number:",
                    "Seat Selection",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (seat != null) {
                seat = seat.trim().toUpperCase();

                if (seat.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a seat number");
                    return;
                }

                if (!seat.matches("^[A-Z][0-9]+$")) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid seat format!\nMust start with a letter followed by numbers\nExample: A12");
                    return;
                }

                try {
                    String seatNumberPart = seat.replaceAll("[^0-9]", "");
                    int seatNumber = Integer.parseInt(seatNumberPart);
                    if (seatNumber < 1) {
                        JOptionPane.showMessageDialog(this, "Seat number must be at least 1");
                        return;
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid seat number format");
                    return;
                }

                if (DBConnection.bookTicket(currentUser.getUserId(), flightId, seat)) {
                    new TicketDetailsFrame(currentUser, flightId, seat).setVisible(true);
                    loadFlights(new HashMap<>());
                } else {
                    JOptionPane.showMessageDialog(this, "Booking failed! Please try again.");
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid flight number format!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + e.getMessage());
        }
    }
}