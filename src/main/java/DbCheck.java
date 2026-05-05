import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbCheck {
    public static void main(String[] args) {
        String[] passwords = {"", "1234"};
        String[] urls = {"jdbc:mysql://localhost:3306/callapp_db", "jdbc:mysql://localhost:3306/"};
        
        for (String url : urls) {
            for (String pass : passwords) {
                System.out.println("Trying connection to: " + url + " with pass: '" + pass + "'");
                try (Connection conn = DriverManager.getConnection(url + "?useSSL=false&serverTimezone=UTC", "root", pass)) {
                    System.out.println("SUCCESS: Connected to " + url + " with pass: '" + pass + "'");
                    if (url.endsWith("/")) {
                        System.out.println("Wait, connected to MySQL root but 'callapp_db' might be missing.");
                    }
                    return;
                } catch (SQLException e) {
                    System.out.println("FAILED: " + e.getMessage());
                }
            }
        }
    }
}
