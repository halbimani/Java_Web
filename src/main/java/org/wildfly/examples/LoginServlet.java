package org.wildfly.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.mindrot.jbcrypt.BCrypt;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try (Connection connection = getConnection()) {
            if (connection != null) {
                if (authenticateUser(connection, username, password)) {
                    resp.sendRedirect("home.jsp");
                } else {
                    forwardWithError(req, resp, "Invalid username or password");
                }
            } else {
                forwardWithError(req, resp, "Failed to connect to the database");
            }
        } catch (Exception e) {
            e.printStackTrace();
            forwardWithError(req, resp, "An error occurred: " + e.getMessage());
        }
    }

    private Connection getConnection() throws Exception {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);
        }
        String url = props.getProperty("db.url");
        String dbUsername = props.getProperty("db.username");
        String dbPassword = props.getProperty("db.password");

        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(url, dbUsername, dbPassword);
    }

    private boolean authenticateUser(Connection connection, String username, String password) throws Exception {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String storedHashedPassword = resultSet.getString("password");
                    return BCrypt.checkpw(password, storedHashedPassword);
                }
            }
        }
        return false;
    }

    private void forwardWithError(HttpServletRequest req, HttpServletResponse resp, String errorMessage)
            throws ServletException, IOException {
        req.setAttribute("error", errorMessage);
        req.getRequestDispatcher("error.jsp").forward(req, resp);
    }
}