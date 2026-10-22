
package com.library;

import java.sql.Connection;
import javax.swing.JOptionPane;

public class TestConnection {

    public static void main(String[] args) {
        try (Connection connection = DBConnection.getConnection()) {
            JOptionPane.showMessageDialog(
                null,
                "Connected to MySQL successfully!"
            );
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                null,
                "Connection failed:\n" + e.getMessage()
            );
        }
    }
}
