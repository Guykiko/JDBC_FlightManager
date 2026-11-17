import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class FlightViewer {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/FlightDB?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";
        String user = "root";

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Please enter the MySQL root password: ");
            String password = scanner.nextLine();

            System.out.print("Enter a target terminal number (2, 3, 4, 5): ");
            String terminal = scanner.nextLine().trim();

            try (Connection connection = DriverManager.getConnection(url, user, password);
                 PreparedStatement pstmt = connection.prepareStatement("SELECT a.id, a.flight_number, r.from_city, a.scheduled, a.delay_status, a.terminal FROM Arrivals a INNER JOIN FlightRoutes r ON a.flight_number = r.flight_number WHERE a.terminal = ? ORDER BY a.scheduled ASC")) {

                pstmt.setString(1, terminal);

                try (ResultSet rs = pstmt.executeQuery()) {
                    int count = 0;

                    System.out.printf("%-5s %-10s %-20s %-15s %-16s %s%n", "ID", "Flight #", "From City", "Scheduled", "Status", "Terminal");
                    System.out.println("----------------------------------------------------------------------------------------------");

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String flightNumber = rs.getString("flight_number");
                        String fromCity = rs.getString("from_city");
                        String scheduled = rs.getString("scheduled");
                        String delayStatus = rs.getString("delay_status");
                        String terminalValue = rs.getString("terminal");

                        System.out.printf("%-5d %-10s %-20s %-15s %-16s %s%n", id, flightNumber, fromCity, scheduled, delayStatus, terminalValue);

                        count++;
                    }

                    if (count == 0) {
                        System.out.println("No flights found for the specified terminal.");
                    } else {
                        System.out.println("Total number of flights found: " + count);
                    }
                } catch (SQLException e) {
                    System.err.println("An error occurred while executing the query.");
                    System.err.println("Error code: " + e.getErrorCode());
                    System.err.println("Error message: " + e.getMessage());
                }
            } catch (SQLException e) {
                System.err.println("An error occurred while connecting to the database.");
                System.err.println("Error code: " + e.getErrorCode());
                System.err.println("Error message: " + e.getMessage());
            }
        }
    }
}
