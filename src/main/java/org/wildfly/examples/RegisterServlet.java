package org.wildfly.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try (Connection connection = getConnection()) {
            if (connection != null) {
                if (isUserExists(connection, username)) {
                    forwardWithError(req, resp, "User already exists");
                } else {
                    registerUser(connection, username, password);
                    forwardWithMessage(req, resp, "home.jsp", "Welcome " + username);
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

    private boolean isUserExists(Connection connection, String username) throws Exception {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void registerUser(Connection connection, String username, String password) throws Exception {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, hashedPassword);
            preparedStatement.executeUpdate();
        }
    }

    private void forwardWithError(HttpServletRequest req, HttpServletResponse resp, String errorMessage)
            throws ServletException, IOException {
        req.setAttribute("error", errorMessage);
        RequestDispatcher dispatcher = req.getRequestDispatcher("error.jsp");
        dispatcher.forward(req, resp);
    }

    private void forwardWithMessage(HttpServletRequest req, HttpServletResponse resp, String page, String message)
            throws ServletException, IOException {
        req.setAttribute("message", message);
        RequestDispatcher dispatcher = req.getRequestDispatcher(page);
        dispatcher.forward(req, resp);
    }
}