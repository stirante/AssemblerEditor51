package com.stirante.asem;

import com.stirante.asem.ui.ByteCreator;
import com.stirante.asem.ui.CodeView;
import com.stirante.asem.ui.SegmentCreator;
import com.stirante.asem.ui.Settings;
import com.stirante.asem.utils.AsyncTask;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by stirante
 */
public class Main extends Application {

    public static final String VERSION = "1.1";
    private static Stage stage;
    //Elements from layout
    @FXML
    public TabPane tabs;
    @FXML
    public TextArea result;
    @FXML
    public MenuItem saveMenuItem;
    @FXML
    public Menu byteMenu;
    @FXML
    public MenuItem newMenuItem;
    @FXML
    public MenuItem openMenuItem;
    @FXML
    public MenuItem closeMenuItem;
    @FXML
    public MenuItem undoMenuItem;
    @FXML
    public MenuItem redoMenuItem;
    @FXML
    public MenuItem segmentCreatorItem;
    private ByteCreator byteCreator;

    public static Stage getStage() {
        return stage;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //load layout
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainWindow.fxml"));
        loader.setController(this);
        VBox root = loader.load();
        Scene scene = new Scene(root, 1280, 720);
        //set stylesheet
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("AssemblerView51");
        //catch close request and check for unsaved files
        primaryStage.setOnCloseRequest(event -> tabs.getTabs().forEach(tab -> ((CodeView) tab).onClose()));
        //disable save menu item if there is no active tab
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            saveMenuItem.setDisable(newValue == null);
            byteMenu.setDisable(newValue == null);
            segmentCreatorItem.setDisable(newValue == null);
        });
        saveMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        newMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        undoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
        closeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        openMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        primaryStage.show();
        stage = primaryStage;
        //Initialize ByteCreator
        byteCreator = new ByteCreator();
        //handle DnD
        root.setOnDragOver(event -> {
            if (event.getGestureSource() != root && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        root.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            System.out.println(db.getFiles());
            boolean success = false;
            if (db.hasFiles()) {
                db.getFiles().forEach(this::openFile);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F9) onCompileClicked();
            else if (event.getCode() == KeyCode.F10) onRunClicked();
        });
        //load file from startup parameters
        List<String> args = getParameters().getUnnamed();
        if (!args.isEmpty()) openFile(new File(args.get(0)));
    }

    //actually open tab for file, not file itself
    private void openFile(File f) {
        Tab tab = new CodeView(f);
        if (!tabs.getTabs().contains(tab)) {
            tabs.getTabs().add(tab);
            tabs.getSelectionModel().select(tab);
        } else {
            tabs.getSelectionModel().select(tab);
        }
    }

    public void onOpenClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open file");
        String path = Settings.getInstance().getLastPath();
        if (path != null && !path.isEmpty()) fileChooser.setInitialDirectory(new File(path));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ASM file", "*.asm"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );
        File f = fileChooser.showOpenDialog(stage);
        if (f != null) {
            Settings.getInstance().setLastPath(f.getParentFile().getAbsolutePath());
            openFile(f);
        }
    }

    public void onCloseClicked() {
        stage.close();
    }

    public void onAboutClicked() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        alert.setTitle("About");
        alert.setHeaderText("AssemblerEditor51 v." + VERSION);
        alert.setContentText("Author: Piotr Brzozowski");
        alert.showAndWait();
    }

    public void onCompileClicked() {
        final CodeView selectedItem = (CodeView) tabs.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            if (!selectedItem.save()) {
                result.setText("You need to save file first!");
                return;
            }

            new AsyncTask<Void, Void, String>() {
                @Override
                public String doInBackground(Void[] params) {
                    return selectedItem.compile();
                }

                @Override
                public void onPostExecute(String compileResult) {
                    result.setText(compileResult);
                }
            }.execute();
        }
    }

    public void onSaveClicked() {
        final CodeView selectedItem = (CodeView) tabs.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            selectedItem.save();
        }
    }

    public void undo() {
        final CodeView selectedItem = (CodeView) tabs.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            selectedItem.undo();
        }
    }

    public void redo() {
        final CodeView selectedItem = (CodeView) tabs.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            selectedItem.redo();
        }
    }

    public void byteCreatorTmod() {
        final CodeView selectedItem = (CodeView) tabs.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            ArrayList<String> descs = new ArrayList<>();
            descs.add(0, "Timer 0\n" +
                    "\n" +
                    "M0\n" +
                    "The last bits third and fourth are known as M1 and M0 respectively. These are used to select the timer mode.");
            descs.add(1, "Timer 0\n" +
                    "\n" +
                    "M1\n" +
                    "The last bits third and fourth are known as M1 and M0 respectively. These are used to select the timer mode.");
            descs.add(2, "Timer 0\n" +
                    "\n" +
                    "C/T\n" +
                    "C/T bit is used to decide whether a timer is used as a time delay generator or an event counter. If this bit is 0 then it is used as a timer and if it is 1 then it is used as a counter.");
            descs.add(3, "Timer 0\n" +
                    "\n" +
                    "GATE\n" +
                    "The hardware way of starting and stopping the timer by an external source is achieved by making GATE=1 in the TMOD register. And if we change to GATE=0 then we do no need external hardware to start and stop the timers.");
            descs.add(4, "Timer 1\n" +
                    "\n" +
                    "M0\n" +
                    "The last bits third and fourth are known as M1 and M0 respectively. These are used to select the timer mode.");
            descs.add(5, "Timer 1\n" +
                    "\n" +
                    "M1\n" +
                    "The last bits third and fourth are known as M1 and M0 respectively. These are used to select the timer mode.");
            descs.add(6, "Timer 1\n" +
                    "\n" +
                    "C/T\n" +
                    "C/T bit is used to decide whether a timer is used as a time delay generator or an event counter. If this bit is 0 then it is used as a timer and if it is 1 then it is used as a counter.");
            descs.add(7, "Timer 1\n" +
                    "\n" +
                    "GATE\n" +
                    "The hardware way of starting and stopping the timer by an external source is achieved by making GATE=1 in the TMOD register. And if we change to GATE=0 then we do no need external hardware to start and stop the timers.");
            String bits = byteCreator.create("TMOD", descs);
            selectedItem.insert(bits);
        }
    }

    public void byteCreatorTcon() {
        final CodeView selectedItem = (CodeView) tabs.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            ArrayList<String> descs = new ArrayList<>();
            descs.add(0, "IT0\n" +
                    "External interrupt 0 signal type control bit. Same as IT0.");
            descs.add(1, "IE0\n" +
                    "External interrupt 0 Edge flag. Not related to timer operations.");
            descs.add(2, "IT1\n" +
                    "External interrupt1 signal type control bit. Set to 1 by program to enable external interrupt 1 to be triggered by a falling edge signal. Set to 0 by program to enable a low level signal on external interrupt1 to generate an interrupt.");
            descs.add(3, "IE1\n" +
                    "External interrupt 1 Edge flag. Not related to timer operations.");
            descs.add(4, "TR0\n" +
                    "Timer 0 run control bit.  Same as TR1.");
            descs.add(5, "TF0\n" +
                    "Timer 0 over flow flag. Same as TF1.");
            descs.add(6, "TR1\n" +
                    "Timer 1 run control bit. Set to 1 by programmer to enable timer to count; Cleared to 0 by program to halt timer.");
            descs.add(7, "TF1\n" +
                    "Timer1 over flow flag. Set when timer rolls from all 1s to 0. Cleared when the processor vectors to execute interrupt service routine. Located at program address 001Bh.");
            String bits = byteCreator.create("TCON", descs);
            selectedItem.insert(bits);
        }
    }

    public void byteCreatorIe() {
        final CodeView selectedItem = (CodeView) tabs.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            ArrayList<String> descs = new ArrayList<>();
            descs.add(0, "EX0\n" +
                    "Enable External 0 Interrupt");
            descs.add(1, "ET0\n" +
                    "Enable Timer 0 Interrupt");
            descs.add(2, "EX1\n" +
                    "Enable External 1 Interrupt");
            descs.add(3, "ET1\n" +
                    "Enable Timer 1 Interrupt");
            descs.add(4, "ES\n" +
                    "Enable Serial Interrupt");
            descs.add(5, "Undefined");
            descs.add(6, "Undefined");
            descs.add(7, "EA\n" +
                    "Global Interrupt Enable/Disable");
            String bits = byteCreator.create("IE", descs);
            selectedItem.insert(bits);
        }
    }

    public void onNewClicked() {
        openFile(null);
    }

    public void onRunClicked() {
        final CodeView selectedItem = (CodeView) tabs.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            if (!selectedItem.save()) {
                result.setText("You need to save and compile file first!");
                return;
            }
            result.setText(selectedItem.run());
        }
    }

    public void segmentCreator() {
        final CodeView selectedItem = (CodeView) tabs.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            String bits = SegmentCreator.create();
            selectedItem.insert(bits);
        }
    }

    public void onSettingsClicked() {
        Settings.getInstance().show();
    }
}
