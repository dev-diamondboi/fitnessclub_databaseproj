import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;

public class Main extends Application {
    // For in-college connection: oracle1.centennialcollege.ca
    // For remote connection use: 199.212.26.208
    private static final String DB_URL = "jdbc:oracle:thin:@199.212.26.208:1521:SQLD";
    private static final String DB_USER = "COMP214_F24_zo_87";
    private static final String DB_PASSWORD = "password";

    private VBox classContainer;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Fitness Club Functions");

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(10);
        gridPane.setHgap(10);

        classContainer = new VBox(10);

        Label enterLbl = new Label("Enter Day ID to view available classes (1 - Monday, 2 - Tuesday, etc): ");
        enterLbl.setStyle("-fx-font-weight: bold;");
        Label priceLbl = new Label("Enter Membership ID to view price: ");
        priceLbl.setStyle("-fx-font-weight: bold;");
        TextField dayIdTxtField = new TextField();


        Button loadButton = new Button("View Classes");
        loadButton.setOnAction(event -> {
            String dayIdText = dayIdTxtField.getText();
            try {
                int dayId = Integer.parseInt(dayIdText);
                if (dayId < 1 || dayId > 7) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Day ID", "Day ID must be between 1 and 7.");
                } else {
                    loadData(dayId);
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please enter a valid number between 1 and 7.");
            }
        });
        Button priceButton = new Button("Get Membership Price");
        TextField membershipIdTxtField = new TextField();
        Label priceLabel = new Label();
        priceButton.setOnAction(event -> {
            String membershipIdText = membershipIdTxtField.getText();
            try {
                int membershipId = Integer.parseInt(membershipIdText);
                if (membershipId < 1) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Membership ID", "Membership ID must be a positive number.");
                } else {
                    double price = getMembershipPrice(membershipId);
                    if (price == -1) {
                        priceLabel.setText("Membership not found.");
                    } else {
                        priceLabel.setText("Membership Price: $" + price);
                    }
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please enter a valid membership ID.");
            }
        });

        gridPane.add(enterLbl, 0, 0);
        gridPane.add(dayIdTxtField, 0, 1);
        gridPane.add(loadButton, 0, 2);
        gridPane.add(classContainer, 0, 3);
        gridPane.add(priceLbl, 0, 4);
        gridPane.add(membershipIdTxtField, 0, 5);
        gridPane.add(priceButton, 0, 6);
        gridPane.add(priceLabel, 0, 7);


        Scene scene = new Scene(gridPane, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    private void loadData(int dayId) {
        classContainer.getChildren().clear();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             CallableStatement callableStatement = connection.prepareCall("{ ? = call GET_CLASSES_BY_DAY_SF(?) }")) {

            callableStatement.registerOutParameter(1, Types.REF_CURSOR);
            callableStatement.setInt(2, dayId);

            callableStatement.execute();

            ResultSet resultSet = (ResultSet) callableStatement.getObject(1);

            while (resultSet.next()) {
                String className = resultSet.getString("class_name");
                String startTime = resultSet.getString("start_time");
                String endTime = resultSet.getString("end_time");

                Label classLabel = new Label("Class Name: " + className + ", Start Time: " + startTime + ", End Time: " + endTime);
                classContainer.getChildren().add(classLabel);
            }

            if (classContainer.getChildren().isEmpty()) {
                classContainer.getChildren().add(new Label("No classes available"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    private double getMembershipPrice(int membershipId) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             CallableStatement callableStatement = connection.prepareCall("{ ? = call GET_MEMBERSHIP_PRICE_SF(?) }")) {

            callableStatement.registerOutParameter(1, Types.NUMERIC);
            callableStatement.setInt(2, membershipId);

            callableStatement.execute();

            return callableStatement.getDouble(1);
        } catch (SQLException e) {
            System.out.println("An error occurred while retrieving the membership price: " + e.getMessage());
            return -1;
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
