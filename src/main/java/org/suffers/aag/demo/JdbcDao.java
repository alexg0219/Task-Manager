package org.suffers.aag.demo;

import org.suffers.aag.demo.entities.Task;
import org.suffers.aag.demo.entities.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import static java.sql.DriverManager.getConnection;

public class JdbcDao {

    private static final String DATABASE_URL = Secret.getUrl() ;
    private static final String DATABASE_USERNAME = Secret.getUsername() ;
    private static final String DATABASE_PASSWORD = Secret.getPassword();

    public User authenticate(String emailId, String password) {

        String SELECT_QUERY = "SELECT * FROM users WHERE username = ? and password = ?";
        try (Connection connection = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);

             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_QUERY)) {
            preparedStatement.setString(1, emailId);
            preparedStatement.setString(2, password);

            System.out.println(preparedStatement);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new User(resultSet.getInt("id"), resultSet.getString("username"),
                        resultSet.getString("password"), resultSet.getString("role"),
                        resultSet.getInt("superior_id"));
            }


        } catch (SQLException e) {
            printSQLException(e);
        }
        return null;
    }

    public static void printSQLException(SQLException ex) {
        for (Throwable e : ex) {
            if (e instanceof SQLException) {
                e.printStackTrace(System.err);
                System.err.println("SQLState: " + ((SQLException) e).getSQLState());
                System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
                System.err.println("Message: " + e.getMessage());
                Throwable t = ex.getCause();
                while (t != null) {
                    System.out.println("Cause: " + t);
                    t = t.getCause();
                }
            }
        }
    }

    public List<Task> getAllTasksByMonth() {
        List<Task> tasks = new ArrayList<>();

        String sql = "SELECT t.close_at,t.deadline,t.created_at,t.description,t.id, t.title, t.status, c.password as creator_password, c.role as creator_role," +
                "c.superior_id as creator_superior, a.password as assignee_password, a.role as assignee_role," +
                "                a.superior_id as assignee_superior, c.id as creator_id, a.id as assignee_id," +
                "                c.id as creator_id, c.username as creator_name," +
                "                a.id as assignee_id, a.username as assignee_name " +
                "                FROM tasks t " +
                "                LEFT JOIN users c ON t.creator_id = c.id " +
                "                LEFT JOIN users a ON t.assignee_id = a.id" +
                "                WHERE status = 'OPEN' or  (close_at > DATE_SUB(NOW(), INTERVAL 1 MONTH) and status = 'CLOSED')";

        try (Connection conn = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                User creator = new User(
                        rs.getInt("creator_id"),
                        rs.getString("creator_name"),
                        rs.getString("creator_password"), rs.getString("creator_role"),
                        rs.getInt("creator_superior")
                );

                User assignee = rs.getObject("assignee_id") != null
                        ? new User(rs.getInt("assignee_id"),
                        rs.getString("assignee_name"),
                        rs.getString("assignee_password"), rs.getString("assignee_role"),
                        rs.getInt("assignee_superior"))
                        : null;

                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        creator,
                        assignee,
                        false,
                        rs.getTimestamp("deadline").toLocalDateTime(),
                        (rs.getTimestamp("close_at") != null) ? rs.getTimestamp("close_at").toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tasks;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();

        try (Connection connection = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users");
             ResultSet resultSet = stmt.executeQuery()) {

            while (resultSet.next()) {
                User user = new User(resultSet.getInt("id"), resultSet.getString("username"),
                        resultSet.getString("password"), resultSet.getString("role"),
                        resultSet.getInt("superior_id"));

                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return users;
    }

    public void updateTask(Task task) {
        String sql = "UPDATE tasks SET title = ?, description = ?, status = ?, created_at = ?, creator_id = ?, assignee_id = ?, deadline = ?, close_at = ? WHERE id = ?";

        try (Connection connection = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setString(3, task.getStatus());
            pstmt.setTimestamp(4, Timestamp.valueOf(task.getCreatedAt()));
            pstmt.setInt(5, task.getCreatorId());
            pstmt.setInt(6, task.getAssigneeId());
            pstmt.setTimestamp(7, Timestamp.valueOf(task.getDeadline()));
            if (task.getCloseAt() == null) {
                pstmt.setNull(8, Types.TIMESTAMP);
            } else {
                pstmt.setTimestamp(8, Timestamp.valueOf(task.getCloseAt()));
            }
            pstmt.setInt(9, task.getId());
            System.out.println(pstmt);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User createUser(User user) {
        String sql = "INSERT INTO users (id, username, password, role, superior_id)" +
                "VALUES (?, ?, ?, ?, ?);";

        try (Connection connection = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRole());
            pstmt.setInt(5, user.getSuperiorId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Sql error");
                return null;
            }
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                    return user;

                }
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public void updateUser(User user) {
        String sql = "UPDATE users SET superior_id = ?, role = ? WHERE id = ?";

        try (Connection connection = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(3, user.getId());
            pstmt.setInt(1, user.getSuperiorId());
            pstmt.setString(2, user.getRole());
            System.out.println(sql);
            System.out.print(user.getId());
            System.out.println(" " + user.getSuperiorId());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTask(Task task) {
        String sql = "INSERT INTO tasks (title, description, status, created_at, creator_id, assignee_id, deadline, close_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setString(3, task.getStatus());
            pstmt.setString(4, task.getCreatedAt().toString());
            pstmt.setInt(5, task.getCreator().getId());
            pstmt.setInt(6, task.getAssignee().getId());
            pstmt.setString(7, task.getDeadline().toString());
            if (task.getCloseAt() == null) {
                pstmt.setNull(8, Types.TIMESTAMP);
            } else {
                pstmt.setTimestamp(8, Timestamp.valueOf(task.getCloseAt()));
            }
            System.out.println(pstmt);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        task.setId(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating task", e);
        }
    }

    public List<Task> getArchivedTasks() {
        List<Task> tasks = new ArrayList<>();

        String sql = "SELECT t.close_at,t.deadline,t.created_at,t.description,t.id, t.title, t.status, c.password as creator_password, c.role as creator_role," +
                "c.superior_id as creator_superior, a.password as assignee_password, a.role as assignee_role," +
                "                a.superior_id as assignee_superior, c.id as creator_id, a.id as assignee_id," +
                "                c.id as creator_id, c.username as creator_name," +
                "                a.id as assignee_id, a.username as assignee_name " +
                "                FROM tasks t " +
                "                LEFT JOIN users c ON t.creator_id = c.id " +
                "                LEFT JOIN users a ON t.assignee_id = a.id" +
                "                WHERE status = 'CLOSED' and close_at < DATE_SUB(NOW(), INTERVAL 1 MONTH)";

        try (Connection conn = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                User creator = new User(
                        rs.getInt("creator_id"),
                        rs.getString("creator_name"),
                        rs.getString("creator_password"), rs.getString("creator_role"),
                        rs.getInt("creator_superior")
                );

                User assignee = rs.getObject("assignee_id") != null
                        ? new User(rs.getInt("assignee_id"),
                        rs.getString("assignee_name"),
                        rs.getString("assignee_password"), rs.getString("assignee_role"),
                        rs.getInt("assignee_superior"))
                        : null;

                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        creator,
                        assignee,
                        false,
                        rs.getTimestamp("deadline").toLocalDateTime(),
                        (rs.getTimestamp("close_at") != null) ? rs.getTimestamp("close_at").toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tasks;
    }

    public List<User> getProgrammersBySuperiorId(int id) {
        List<User> programmers = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'PROGRAMMER' AND superior_id = ?";

        try (Connection connection = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, id);
            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {

                User user = new User(resultSet.getInt("id"), resultSet.getString("username"),
                        resultSet.getString("password"), resultSet.getString("role"),
                        resultSet.getInt("superior_id"));

                programmers.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return programmers;
    }

    public List<Task> getAllTasksByMonthAndAdmin(int id) {
        List<Task> tasks = new ArrayList<>();

        String sql = "SELECT t.close_at,t.deadline,t.created_at,t.description,t.id, t.title, t.status, c.password as creator_password, c.role as creator_role," +
                "c.superior_id as creator_superior, a.password as assignee_password, a.role as assignee_role," +
                "                a.superior_id as assignee_superior, c.id as creator_id, a.id as assignee_id," +
                "                c.id as creator_id, c.username as creator_name," +
                "                a.id as assignee_id, a.username as assignee_name " +
                "                FROM tasks t " +
                "                LEFT JOIN users c ON t.creator_id = c.id " +
                "                LEFT JOIN users a ON t.assignee_id = a.id" +
                "                WHERE (status = 'OPEN' or  (close_at > DATE_SUB(NOW(), INTERVAL 1 MONTH) and status = 'CLOSED')) and (creator_id = ? or (assignee_id = ? and c.role = 'OWNER'))";

        try (Connection conn = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.setInt(2, id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User creator = new User(
                        rs.getInt("creator_id"),
                        rs.getString("creator_name"),
                        rs.getString("creator_password"), rs.getString("creator_role"),
                        rs.getInt("creator_superior")
                );

                User assignee = rs.getObject("assignee_id") != null
                        ? new User(rs.getInt("assignee_id"),
                        rs.getString("assignee_name"),
                        rs.getString("assignee_password"), rs.getString("assignee_role"),
                        rs.getInt("assignee_superior"))
                        : null;

                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        creator,
                        assignee,
                        false,
                        rs.getTimestamp("deadline").toLocalDateTime(),
                        (rs.getTimestamp("close_at") != null) ? rs.getTimestamp("close_at").toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public List<Task> getAllTasksByMonthAndProgrammer(int id) {
        List<Task> tasks = new ArrayList<>();

        String sql = "SELECT t.close_at,t.deadline,t.created_at,t.description,t.id, t.title, t.status, c.password as creator_password, c.role as creator_role," +
                "c.superior_id as creator_superior, a.password as assignee_password, a.role as assignee_role," +
                "                a.superior_id as assignee_superior, c.id as creator_id, a.id as assignee_id," +
                "                c.id as creator_id, c.username as creator_name," +
                "                a.id as assignee_id, a.username as assignee_name " +
                "                FROM tasks t " +
                "                LEFT JOIN users c ON t.creator_id = c.id " +
                "                LEFT JOIN users a ON t.assignee_id = a.id" +
                "                WHERE (status = 'OPEN' or  (close_at > DATE_SUB(NOW(), INTERVAL 1 MONTH) and status = 'CLOSED')) and assignee_id = ?";

        try (Connection conn = getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User creator = new User(
                        rs.getInt("creator_id"),
                        rs.getString("creator_name"),
                        rs.getString("creator_password"), rs.getString("creator_role"),
                        rs.getInt("creator_superior")
                );

                User assignee = rs.getObject("assignee_id") != null
                        ? new User(rs.getInt("assignee_id"),
                        rs.getString("assignee_name"),
                        rs.getString("assignee_password"), rs.getString("assignee_role"),
                        rs.getInt("assignee_superior"))
                        : null;

                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        creator,
                        assignee,
                        false,
                        rs.getTimestamp("deadline").toLocalDateTime(),
                        (rs.getTimestamp("close_at") != null) ? rs.getTimestamp("close_at").toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }
}