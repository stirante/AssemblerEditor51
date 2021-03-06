package com.stirante.asem.utils;

import com.stirante.asem.Constants;
import com.stirante.asem.Main;
import com.stirante.asem.ui.Settings;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

/**
 * Created by stirante
 */
public class UpdateUtil {

    private static final String JAR_VERSION = "http://files.stirante.com/version.txt";

    public static void check(Main app) {
        if (!Settings.getInstance().isCheckingUpdate()) return;
        double[] remote = new double[1];
        new AsyncTask<Void, Void, Void>() {

            @Override
            public Void doInBackground(Void[] params) {
                try {
                    URLConnection connection = new URL(JAR_VERSION).openConnection();
                    InputStream is = connection.getInputStream();
                    BufferedReader r = new BufferedReader(new InputStreamReader(is));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (total.length() != 0)
                            total.append("\n");//Actually it will never occur but why not?
                        total.append(line);
                    }
                    remote[0] = Double.parseDouble(total.toString());
                    r.close();
                } catch (Exception ignored) {
                    //This error doesn't really bother anyone. Probably just server overloaded or no internet connection
                }
                return null;
            }

            @Override
            public void onPostExecute(Void r) {
                if (Constants.VERSION < remote[0]) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("There is new version!");
                    alert.setHeaderText(null);
                    alert.setContentText("Do you want to update?");
                    alert.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

                    ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                    ButtonType no = new ButtonType("No", ButtonBar.ButtonData.NO);
                    alert.getButtonTypes().setAll(yes, no);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == yes) {
                        try {
                            Runtime.getRuntime().exec("java -jar SimpleUpdater.jar check .");
                            System.exit(0);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.execute();
    }

}
