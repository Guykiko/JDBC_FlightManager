import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Updates an arrival's delay status in the FlightDB database.
 *
 * <p>The previous implementation built SQL by concatenating the entered status
 * into the query text. The database then parsed that combined text as SQL, so
 * input containing SQL syntax could alter the query instead of being treated
 * only as a status value.</p>
 *
 * <p>This version uses a {@link PreparedStatement}: the SQL structure is
 * prepared with {@code ?} placeholders, and values are bound separately with
 * {@code setString}. The database treats bound values as data, not SQL syntax,
 * preventing those values from changing the query's structure. Placeholders
 * apply to values, not SQL identifiers or other query structure.</p>
 */
public class SimpleFlightUpdater {
    private static final String URL =
            "jdbc:mysql://localhost:3306/FlightDB?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";
    private static final String USER = "root";
    private static final String FLIGHT_NUMBER = "LX354";

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Please enter the MySQL root password: ");
            String password = scanner.nextLine();

            try (Connection connection = DriverManager.getConnection(URL, USER, password)) {
                System.out.print("Please enter a new delay status for flight 'LX354': ");
                String delayStatus = scanner.nextLine();

                int rowsAffected = updateDelayStatus(connection, FLIGHT_NUMBER, delayStatus);
                System.out.println("Number of affected rows: " + rowsAffected);
            } catch (SQLException e) {
                System.err.println("An error occurred while updating the database.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Updates the delay status for the specified flight using parameterized SQL.
     *
     * <p>Previously, concatenating user input into the SQL string allowed that
     * input to be interpreted as part of the SQL command. An attacker could
     * provide text that changes the command's meaning. Here, the query is
     * prepared with placeholders before either value is supplied. Binding the
     * values separately ensures they are treated as data, so they cannot alter
     * the query structure.</p>
     *
     * @param connection active database connection
     * @param flightNumber flight number whose arrival status will be updated
     * @param delayStatus new delay status to store
     * @return the number of rows affected
     * @throws SQLException if preparing or executing the update fails
     */
    private static int updateDelayStatus(
            Connection connection, String flightNumber, String delayStatus) throws SQLException {
        String sql = "UPDATE Arrivals SET delay_status = ? WHERE flight_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, delayStatus);
            statement.setString(2, flightNumber);
            return statement.executeUpdate();
        }
    }
}