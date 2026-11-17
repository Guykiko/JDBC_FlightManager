import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement; // FIX #2: missing import - Statement.SUCCESS_NO_INFO did not compile without it
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Batched status dispatcher for the Airport Flight Arrivals Management System.
 * <p>
 * Updates the {@code delay_status} of several arrivals in a single network round-trip
 * using a pre-compiled {@link PreparedStatement} together with JDBC batch processing
 * ({@code addBatch()} / {@code executeBatch()}).
 * </p>
 * <p>
 * Generated with Continue (Qwen2.5-Coder-7B-Instruct) and manually reviewed.
 * Manual corrections applied to the AI output:
 * <ol>
 *   <li>FIX #1 - The constant parameter {@code newStatus} is now bound ONCE, outside the loop
 *       (the AI re-bound it on every iteration, ignoring the CRITICAL requirement).</li>
 *   <li>FIX #2 - Added the missing {@code import java.sql.Statement} (compilation error).</li>
 *   <li>FIX #3 - Only positive update counts are summed (negative codes such as
 *       {@code EXECUTE_FAILED} are excluded), and the summary message was made explicit.</li>
 * </ol>
 * </p>
 */
public class AdvancedFlightDispatcher {

    /** JDBC connection URL pointing to FlightDB. */
    private static final String URL =
            "jdbc:mysql://localhost:3306/FlightDB?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";

    /** MySQL administrative user. */
    private static final String USER = "root";

    /**
     * Application entry point. Prompts for the MySQL root password, opens a connection
     * with try-with-resources, and sets arrivals 1, 3 and 5 to {@code 'Landed - Final'}.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Please enter the MySQL root password: ");
            String password = scanner.nextLine();

            try (Connection connection = DriverManager.getConnection(URL, USER, password)) {
                List<Integer> arrivalIds = Arrays.asList(1, 3, 5);
                updateMultipleDelays(connection, arrivalIds, "Landed - Final");
            } catch (SQLException e) {
                System.err.println("An error occurred while updating the database.");
                System.err.println("Error Code: " + e.getErrorCode());
                System.err.println("Message: " + e.getMessage());
            }
        }
    }

    /**
     * Updates {@code delay_status} for all given arrival IDs using one {@link PreparedStatement}
     * and a single JDBC batch.
     * <p>
     * The constant parameter ({@code newStatus}, index 1) is bound once before the loop, because a
     * {@code PreparedStatement} keeps its parameter values between {@code addBatch()} calls.
     * Inside the loop only the varying parameter ({@code id}, index 2) is bound and the command
     * is queued with {@code addBatch()}. All queued commands are then sent to the server together
     * by {@code executeBatch()}, instead of one {@code executeUpdate()} round-trip per ID.
     * </p>
     *
     * @param conn       active JDBC {@link Connection} to {@code FlightDB}
     * @param arrivalIds list of {@code Arrivals.id} values to update
     * @param newStatus  new value for {@code delay_status}
     * @throws SQLException if preparing or executing the batch fails
     */
    public static void updateMultipleDelays(Connection conn, List<Integer> arrivalIds, String newStatus) throws SQLException {
        String sql = "UPDATE Arrivals SET delay_status = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // FIX #1: bind the constant parameter ONCE, outside the loop
            pstmt.setString(1, newStatus);

            for (Integer id : arrivalIds) {
                pstmt.setInt(2, id);   // only the varying parameter changes per iteration
                pstmt.addBatch();      // queue the command - nothing is sent to the server yet
            }

            // Send all queued commands to the server in one batch
            int[] results = pstmt.executeBatch();

            // FIX #3: sum only positive update counts
            int totalAffectedRows = 0;
            for (int result : results) {
                if (result > 0) {
                    totalAffectedRows += result;
                } else if (result == Statement.SUCCESS_NO_INFO) {
                    System.out.println("Note: driver returned SUCCESS_NO_INFO for one batch command.");
                }
            }

            System.out.println("Batch update completed: " + results.length + " commands sent, "
                    + totalAffectedRows + " rows updated to '" + newStatus + "' (IDs: " + arrivalIds + ").");
        }
    }
}