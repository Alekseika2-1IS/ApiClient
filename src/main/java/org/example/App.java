package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App extends Application {

    private static final String URL = "http://192.168.1.200:4444/TransferSimulator/";

    private final ComboBox<String> typeBox = new ComboBox<>(
            FXCollections.observableArrayList("fullName", "snils", "inn", "email", "identityCard"));
    private final Label dataLabel = new Label("Данные: —");
    private final Label resultLabel = new Label("");
    private final Button checkButton = new Button("Проверить данные");

    private String type = "";
    private String value = "";

    @Override
    public void start(Stage stage) {
        typeBox.getSelectionModel().selectFirst();
        checkButton.setVisible(false);
        checkButton.setOnAction(e -> resultLabel.setText(check(type, value)));

        Button getButton = new Button("Получить данные");
        getButton.setOnAction(e -> load());

        VBox root = new VBox(10, new Label("Тип данных:"), typeBox, getButton, dataLabel, checkButton, resultLabel);
        root.setPadding(new Insets(20));

        stage.setTitle("Валидация данных клиента");
        stage.setMinWidth(420);
        stage.setMinHeight(320);
        stage.setScene(new Scene(root, 420, 320));
        stage.show();
    }

    private void load() {
        type = typeBox.getValue();
        resultLabel.setText("");
        checkButton.setVisible(false);
        dataLabel.setText("Запрос к серверу...");
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(URI.create(URL + type)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    Platform.runLater(() -> dataLabel.setText("Ошибка сервера: код " + response.statusCode()
                            + (response.statusCode() == 500 ? " — обратитесь к главному эксперту" : "")));
                    return;
                }
                Matcher m = Pattern.compile("\"value\"\\s*:\\s*\"(.*)\"").matcher(response.body());
                value = m.find() ? m.group(1) : "";
                Platform.runLater(() -> {
                    dataLabel.setText("Данные: " + value);
                    checkButton.setVisible(true);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> dataLabel.setText("Ошибка: нет связи с сервером. Проверьте сеть и повторите запрос."));
            }
        }).start();
    }

    private String check(String type, String v) {
        String error = switch (type) {
            case "fullName" -> v.matches("[A-Za-zА-Яа-яЁё\\- ]+") ? null : "запрещённые символы в ФИО";
            case "snils" -> v.matches("\\d{3}-\\d{3}-\\d{3} \\d{2}") ? null : "неверный формат СНИЛС";
            case "inn" -> v.matches("\\d{10}(\\d{2})?") ? null : "ИНН должен быть 10 или 12 цифр";
            case "email" -> !v.contains(" ") && v.indexOf('@') == v.lastIndexOf('@') && v.contains(".") ? null : "некорректный e-mail";
            case "identityCard" -> v.matches("\\d{2} \\d{2} \\d{6}") ? null : "неверный формат карты";
            default -> "неизвестный тип";
        };
        return error == null ? "Данные КОРРЕКТНЫ" : "Данные НЕ корректны: " + error;
    }

    public static void main(String[] args) {
        launch(args);
    }
}