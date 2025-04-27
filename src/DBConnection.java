import org.mindrot.jbcrypt.BCrypt;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/bulgaria_air";
    private static final String USER = "root";
    private static final String PASSWORD = "5556444Ralic.";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static User login(String email, String password) throws SQLException {
        String query = "SELECT user_id, email, first_name, last_name, password, profile_image FROM users WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && BCrypt.checkpw(password, rs.getString("password"))) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("email"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getBytes("profile_image")
                    );
                }
            }
        }
        return null;
    }

    public static boolean register(String email, String firstName, String lastName,
                                   String password, byte[] image) throws SQLException {
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        String query = "INSERT INTO users (email, first_name, last_name, password, profile_image) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, email);
            pstmt.setString(2, firstName);
            pstmt.setString(3, lastName);
            pstmt.setString(4, hashed);
            pstmt.setBytes(5, image);

            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean bookTicket(int userId, int flightId, String seat) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT available_seats FROM flights WHERE flight_id = ?")) {
                pstmt.setInt(1, flightId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("available_seats") < 1) {
                    throw new SQLException("No available seats");
                }
            }

            // Create ticket
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO tickets (user_id, flight_id, seat_number) VALUES (?, ?, ?)")) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, flightId);
                pstmt.setString(3, seat);
                pstmt.executeUpdate();
            }

            // Update available seats
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE flights SET available_seats = available_seats - 1 WHERE flight_id = ?")) {
                pstmt.setInt(1, flightId);
                pstmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public static ResultSet getFlightDetails(int flightId) throws SQLException {
        String query = "SELECT * FROM flights WHERE flight_id = ?";
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, flightId);
        return pstmt.executeQuery();
    }

    public static byte[] resizeImage(BufferedImage originalImage) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage resizedImage = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, 150, 150, null);
            g.dispose();

            ImageIO.write(resizedImage, "jpg", baos);
            return baos.toByteArray();
        }
    }

    public static boolean cancelTicket(int ticketId) throws SQLException {
        String query = "DELETE FROM tickets WHERE ticket_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, ticketId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean updateBooking(int ticketId, int newFlightId, String newSeat) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // Get old flight ID
            int oldFlightId;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT flight_id FROM tickets WHERE ticket_id = ?")) {
                pstmt.setInt(1, ticketId);
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) throw new SQLException("Ticket not found");
                oldFlightId = rs.getInt("flight_id");
            }

            // Update ticket
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE tickets SET flight_id = ?, seat_number = ? WHERE ticket_id = ?")) {
                pstmt.setInt(1, newFlightId);
                pstmt.setString(2, newSeat);
                pstmt.setInt(3, ticketId);
                pstmt.executeUpdate();
            }

            // Update seat counts
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE flights SET available_seats = available_seats + 1 WHERE flight_id = ?")) {
                pstmt.setInt(1, oldFlightId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE flights SET available_seats = available_seats - 1 WHERE flight_id = ?")) {
                pstmt.setInt(1, newFlightId);
                pstmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public static ArrayList<Integer> getAvailableFlights() throws SQLException {
        ArrayList<Integer> flights = new ArrayList<>();
        String query = "SELECT flight_id FROM flights WHERE available_seats > 0";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                flights.add(rs.getInt("flight_id"));
            }
        }
        return flights;
    }
}