
package com.library;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LibraryManagementSystem extends JFrame {

    public LibraryManagementSystem() {
        setTitle("Library Management System");
        setSize(700, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel heading = new JLabel(
                "LIBRARY MANAGEMENT SYSTEM",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 26));

        JButton addBookButton = new JButton("Add Book");
        JButton viewBooksButton = new JButton("View Books");
        JButton issueBookButton = new JButton("Issue Book");
        JButton returnBookButton = new JButton("Return Book");

        // Add Book
        addBookButton.addActionListener(e -> {
            JTextField idField = new JTextField();
            JTextField titleField = new JTextField();
            JTextField authorField = new JTextField();

            Object[] fields = {
                "Book ID:", idField,
                "Book Title:", titleField,
                "Author:", authorField
            };

            int result = JOptionPane.showConfirmDialog(
                    this, fields, "Add New Book",
                    JOptionPane.OK_CANCEL_OPTION
            );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String idText = idField.getText().trim();
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();

            if (idText.isEmpty() || title.isEmpty() || author.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this, "Please fill in all fields."
                );
                return;
            }

            try {
                int bookId = Integer.parseInt(idText);

                String sql = "INSERT INTO books "
                        + "(book_id, title, author, available) "
                        + "VALUES (?, ?, ?, TRUE)";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    stmt.setInt(1, bookId);
                    stmt.setString(2, title);
                    stmt.setString(3, author);

                    stmt.executeUpdate();

                    JOptionPane.showMessageDialog(
                            this, "Book saved to MySQL successfully!"
                    );
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this, "Book ID must be a whole number."
                );
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Could not save book:\n" + ex.getMessage()
                );
            }
        });

        // View Books
        viewBooksButton.addActionListener(e -> {
            String sql = "SELECT book_id, title, author, available "
                    + "FROM books ORDER BY book_id";

            StringBuilder bookList = new StringBuilder();

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    bookList.append("ID: ")
                            .append(rs.getInt("book_id"))
                            .append(" | Title: ")
                            .append(rs.getString("title"))
                            .append(" | Author: ")
                            .append(rs.getString("author"))
                            .append(" | Available: ")
                            .append(rs.getBoolean("available") ? "Yes" : "No")
                            .append("\n\n");
                }

                if (bookList.length() == 0) {
                    bookList.append("No books found in the database.");
                }

                JTextArea area = new JTextArea(bookList.toString(), 12, 40);
                area.setEditable(false);

                JOptionPane.showMessageDialog(
                        this, new JScrollPane(area),
                        "All Books", JOptionPane.INFORMATION_MESSAGE
                );

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Could not load books:\n" + ex.getMessage()
                );
            }
        });

        // Issue Book (display-only for now)
        
        // Issue Book
        issueBookButton.addActionListener(e -> {
            JTextField bookIdField = new JTextField();
            JTextField borrowerField = new JTextField();

            Object[] fields = {
                "Book ID:", bookIdField,
                "Borrower's Name:", borrowerField
            };

            int result = JOptionPane.showConfirmDialog(
                    this, fields, "Issue Book",
                    JOptionPane.OK_CANCEL_OPTION
            );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String idText = bookIdField.getText().trim();
            String borrower = borrowerField.getText().trim();

            if (idText.isEmpty() || borrower.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this, "Please fill in all fields."
                );
                return;
            }

            try {
                int bookId = Integer.parseInt(idText);

                try (Connection conn = DBConnection.getConnection()) {
                    conn.setAutoCommit(false);

                    try {
                        // Check whether the book exists and is available
                        String checkSql =
                                "SELECT available FROM books "
                                + "WHERE book_id = ? FOR UPDATE";

                        boolean found;
                        boolean available = false;

                        try (PreparedStatement checkStmt =
                                     conn.prepareStatement(checkSql)) {
                            checkStmt.setInt(1, bookId);

                            try (ResultSet rs = checkStmt.executeQuery()) {
                                found = rs.next();

                                if (found) {
                                    available = rs.getBoolean("available");
                                }
                            }
                        }

                        if (!found) {
                            conn.rollback();
                            JOptionPane.showMessageDialog(
                                    this, "Book ID not found."
                            );
                            return;
                        }

                        if (!available) {
                            conn.rollback();
                            JOptionPane.showMessageDialog(
                                    this, "This book is already issued."
                            );
                            return;
                        }

                        // Record the issue
                        String issueSql =
                                "INSERT INTO issued_books "
                                + "(book_id, borrower_name) VALUES (?, ?)";

                        try (PreparedStatement issueStmt =
                                     conn.prepareStatement(issueSql)) {
                            issueStmt.setInt(1, bookId);
                            issueStmt.setString(2, borrower);
                            issueStmt.executeUpdate();
                        }

                        // Mark the book unavailable
                        String updateSql =
                                "UPDATE books SET available = FALSE "
                                + "WHERE book_id = ?";

                        try (PreparedStatement updateStmt =
                                     conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, bookId);
                            updateStmt.executeUpdate();
                        }

                        conn.commit();

                        JOptionPane.showMessageDialog(
                                this, "Book issued successfully!"
                        );

                    } catch (SQLException ex) {
                        conn.rollback();
                        throw ex;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this, "Book ID must be a whole number."
                );
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Could not issue book:\n" + ex.getMessage()
                );
            }
        });

        // Return Book (display-only for now)
        
        // Return Book
        returnBookButton.addActionListener(e -> {
            JTextField bookIdField = new JTextField();

            int result = JOptionPane.showConfirmDialog(
                    this,
                    new Object[]{"Book ID to return:", bookIdField},
                    "Return Book",
                    JOptionPane.OK_CANCEL_OPTION
            );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String idText = bookIdField.getText().trim();

            if (idText.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this, "Please enter the Book ID."
                );
                return;
            }

            try {
                int bookId = Integer.parseInt(idText);

                try (Connection conn = DBConnection.getConnection()) {
                    conn.setAutoCommit(false);

                    try {
                        // Find an active issue record for this book
                        String checkSql =
                                "SELECT issue_id FROM issued_books "
                                + "WHERE book_id = ? AND return_date IS NULL "
                                + "ORDER BY issue_id DESC LIMIT 1 FOR UPDATE";

                        int issueId = -1;

                        try (PreparedStatement checkStmt =
                                     conn.prepareStatement(checkSql)) {
                            checkStmt.setInt(1, bookId);

                            try (ResultSet rs = checkStmt.executeQuery()) {
                                if (rs.next()) {
                                    issueId = rs.getInt("issue_id");
                                }
                            }
                        }

                        if (issueId == -1) {
                            conn.rollback();
                            JOptionPane.showMessageDialog(
                                    this,
                                    "No active issue record found for this book."
                            );
                            return;
                        }

                        // Record the return date
                        String returnSql =
                                "UPDATE issued_books "
                                + "SET return_date = CURRENT_TIMESTAMP "
                                + "WHERE issue_id = ?";

                        try (PreparedStatement returnStmt =
                                     conn.prepareStatement(returnSql)) {
                            returnStmt.setInt(1, issueId);
                            returnStmt.executeUpdate();
                        }

                        // Mark the book available again
                        String updateSql =
                                "UPDATE books SET available = TRUE "
                                + "WHERE book_id = ?";

                        try (PreparedStatement updateStmt =
                                     conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, bookId);
                            updateStmt.executeUpdate();
                        }

                        conn.commit();

                        JOptionPane.showMessageDialog(
                                this, "Book returned successfully!"
                        );

                    } catch (SQLException ex) {
                        conn.rollback();
                        throw ex;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this, "Book ID must be a whole number."
                );
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Could not return book:\n" + ex.getMessage()
                );
            }
        });

        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(
                25, 150, 25, 150
        ));

        buttonPanel.add(addBookButton);
        buttonPanel.add(viewBooksButton);
        buttonPanel.add(issueBookButton);
        buttonPanel.add(returnBookButton);

        setLayout(new BorderLayout(10, 20));
        add(heading, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LibraryManagementSystem().setVisible(true);
        });
    }
}
