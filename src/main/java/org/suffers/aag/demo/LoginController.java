package org.suffers.aag.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.suffers.aag.demo.entities.User;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField emailIdField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button submitButton;

    @FXML
    public void login(ActionEvent event) throws SQLException, IOException {

        Window owner = submitButton.getScene().getWindow();
        Stage currentStage = (Stage) submitButton.getScene().getWindow();
        System.out.println(emailIdField.getText());
        System.out.println(passwordField.getText());

        if (emailIdField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, owner, "Error!",
                    "Введите имя");
            return;
        }
        if (passwordField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, owner, "Form Error!",
                    "Введите пароль");
            return;
        }

        String emailId = emailIdField.getText();
        String password = passwordField.getText();

        JdbcDao jdbcDao = new JdbcDao();
        User user = jdbcDao.authenticate(emailId, PasswordManager.hashPassword(password));

        if (user == null) {
            infoBox("Неверный пароль или имя пользователя", null, "Failed");
        } else {
            currentStage.close();

            FXMLLoader loader = new FXMLLoader();
            loader.setControllerFactory(controllerClass -> {
                try {
                    if (controllerClass == OwnerController.class) {
                        return new OwnerController(user);
                    } else if (controllerClass == AdminController.class) {
                        return new AdminController(user);
                    } else if (controllerClass == ProgrammerController.class) {
                        return new ProgrammerController(user);
                    } else {
                        return controllerClass.newInstance();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            if (user.getRole().equals("OWNER")) {
                loader.setLocation(getClass().getResource("table_owner.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Управление задачами");
                stage.show();
            } else if (user.getRole().equals("ADMIN")) {
                loader.setLocation(getClass().getResource("table_admin.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Управление задачами");
                stage.show();
            } else {
                loader.setLocation(getClass().getResource("table_programmer.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Управление задачами");
                stage.show();
            }
        }
    }

    public static void infoBox(String infoMessage, String headerText, String title) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setContentText(infoMessage);
        alert.setTitle(title);
        alert.setHeaderText(headerText);

        alert.initStyle(StageStyle.UTILITY);
        alert.getDialogPane().getStylesheets().add(
                LoginController.class.getResource("css/style.css").toExternalForm()
        );
        alert.getDialogPane().getStyleClass().add("custom-alert");

        alert.getDialogPane().getButtonTypes().forEach(buttonType -> {
            Button button = (Button) alert.getDialogPane().lookupButton(buttonType);
            if (button != null) {
                button.getStyleClass().add("action-button");
            }
        });

        alert.showAndWait();
    }

    private static void showAlert(Alert.AlertType alertType, Window owner, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);

        alert.initStyle(StageStyle.UTILITY);
        alert.getDialogPane().getStylesheets().add(
                LoginController.class.getResource("css/style.css").toExternalForm()
        );
        alert.getDialogPane().getStyleClass().add("custom-alert");

        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (okButton != null) {
            okButton.getStyleClass().add("action-button");
        }
        if (cancelButton != null) {
            cancelButton.getStyleClass().add("dialog-button");
        }
        alert.show();
    }
}