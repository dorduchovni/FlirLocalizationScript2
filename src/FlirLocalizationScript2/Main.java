package FlirLocalizationScript2;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.poi.hwpf.model.Sttb;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Main extends Application {
    private String filepath;
    private String selectedDirectoryPath;
    private Map<String,SheetToTranslate> sheetToTranslateMap = new LinkedHashMap<>();
    private String toExcelSource;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Localization");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        Manager manager = new Manager();
        FlowPane translateFlowPane =(FlowPane) (((AnchorPane)(((ScrollPane) ((TabPane)root.getChildrenUnmodifiable().get(0)).getTabs().get(0).getContent()).getContent())).getChildren().get(0));
        FlowPane exportToExcelFlowPane = (FlowPane) (((TabPane)root.getChildrenUnmodifiable().get(0)).getTabs().get(1).getContent());
        Button exportBrowseBtn = (Button) (exportToExcelFlowPane.lookup("#exportBrowseBtn"));
        TextField exportTextField = (TextField) (exportToExcelFlowPane.lookup("#exportFilePath"));
        exportBrowseBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                final String[] platform = new String[1];
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Resource File");
                File file = fileChooser.showOpenDialog(primaryStage);
                if (file != null) {
                    toExcelSource = file.getPath();
                    exportTextField.setText(toExcelSource);
                    GridPane pane = new GridPane();
                    Label platformLabel = new Label("Select Platform: ");
                    pane.add(platformLabel,0,0);
                    RadioButton platformIos = new RadioButton("iOS");
                    RadioButton platformAndroid = new RadioButton("Android");
                    final ToggleGroup group = new ToggleGroup();
                    platformIos.setToggleGroup(group);
                    platformAndroid.setToggleGroup(group);
                    platformIos.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            if (platformIos.isSelected()) {
                                platform[0] = "iOS";
                                platformAndroid.setSelected(false);
                            }
                        }
                    });
                    platformAndroid.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            if (platformAndroid.isSelected()){
                                platform[0] = "Android";
                                platformIos.setSelected(false);
                            }
                        }
                    });
                    pane.add(platformIos,0,1);
                    pane.add(platformAndroid,1,1);
                    Label savePath = new Label("Where to save it?");
                    pane.add(savePath,0,2);
                    TextField savePathTextField = new TextField();
                    pane.add(savePathTextField,0,3);
                    Button saveBtn = new Button("Browse");
                    Button exportBtn = new Button("Export");
                    exportBtn.setDisable(true);
                    saveBtn.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            DirectoryChooser chooser = new DirectoryChooser();
                            chooser.setTitle("Choose where to save");
                            File selectedDirectory = chooser.showDialog(primaryStage);
                            selectedDirectoryPath = selectedDirectory.getPath();
                            savePathTextField.setText(selectedDirectory.getPath());
                            exportBtn.setDisable(false);
                        }
                    });
                    exportBtn.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            manager.directory = selectedDirectoryPath;
                            try {
                                manager.sourceToExcel(toExcelSource,platform[0]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Export finished");
                            alert.setContentText("Export finished successfully");
                            alert.showAndWait();
                            try {
                                start(primaryStage);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    pane.add(saveBtn,1,3);
                    pane.add(exportBtn,0,4);
                    exportToExcelFlowPane.getChildren().add(pane);
                }
            }
        });
        Button browseBtn = (Button) (translateFlowPane).lookup("#browseBtn") ;
        Button startBtn = (Button) (translateFlowPane).lookup("#startBtn");
        Button resetBtn = (Button) (translateFlowPane).lookup("#resetBtn");
        javafx.scene.control.TextField browseTextfield = (TextField) (translateFlowPane).lookup("#filepathTextView");
        ScrollPane sp = new ScrollPane();
        sp.setContent(translateFlowPane);
        translateFlowPane.setVgap(5);
        translateFlowPane.setHgap(20);


        browseBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Resource File");
                File file = fileChooser.showOpenDialog(primaryStage);
                if (file != null) {
                    filepath = file.getAbsolutePath();
                    browseTextfield.setText(file.getAbsolutePath());
                }
            }
        });

        startBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {

                    LinkedHashMap<String, java.util.List<String>> sheetsAndLanguages = manager.getSheetsAndLanguages(filepath);
                    for (String sheet: sheetsAndLanguages.keySet()) {
                        sheetToTranslateMap.put(sheet,new SheetToTranslate());
                        sheetToTranslateMap.get(sheet).languages = new ArrayList<String>();
                    }
                    for (String sheet: sheetsAndLanguages.keySet()) {
                        sheetsAndLanguagesUI(sheet, sheetsAndLanguages.get(sheet),translateFlowPane, primaryStage);
                    }

                    startBtn.setDisable(true);
                    ScrollPane sp = new ScrollPane();
                    GridPane pane = new GridPane();
                    Label whereToSaveLabel= new Label("Choose where to save the files");
                    pane.add(whereToSaveLabel,0,0);
                    TextField whereToSavePath = new TextField();
                    pane.add(whereToSavePath,0,1);
                    Button selectDirectoryBtn = new Button("Browse");
                    Button translateBtn = new Button("Translate");
                    translateBtn.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {

                            if (allIsOk(sheetToTranslateMap)) {
                                Map<String,SheetToTranslate> sendToManager = new LinkedHashMap<String, SheetToTranslate>();
                                for (String sheet : sheetToTranslateMap.keySet()) {
                                    SheetToTranslate sheetToTranslate = sheetToTranslateMap.get(sheet);
                                    if (sheetToTranslate.languages.size() > 0) {
                                        sendToManager.put(sheet,sheetToTranslate);
                                    }
                                }
                                try {
                                    manager.translateSheetsMap(sendToManager,filepath,selectedDirectoryPath);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Translation finished");
                            alert.setContentText("Translation finished successfully");
                            alert.showAndWait();

                            try {
                                start(primaryStage);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    selectDirectoryBtn.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            DirectoryChooser chooser = new DirectoryChooser();
                            chooser.setTitle("Choose where to save");
                            File selectedDirectory = chooser.showDialog(primaryStage);
                            selectedDirectoryPath = selectedDirectory.getPath();
                            whereToSavePath.setText(selectedDirectory.getPath());
                            translateBtn.setDisable(false);
                        }
                    });
                    translateBtn.setDisable(true);
                    pane.add(selectDirectoryBtn,1,1);
                    pane.add(translateBtn,1,2);
                    sp.setContent(pane);
                    sp.setFitToHeight(true);
                    sp.setFitToWidth(true);
                    translateFlowPane.getChildren().add(sp);








                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        resetBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    start(primaryStage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


    }

    private boolean allIsOk(Map<String, SheetToTranslate> sheetToTranslateMap) {
        for (String sheet : sheetToTranslateMap.keySet()) {
            SheetToTranslate sheetToTranslate = sheetToTranslateMap.get(sheet);
            if (sheetToTranslate.languages.size()>0) {
                if (sheetToTranslate.sourceFileName == null || sheetToTranslate.platform == null){
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error - "+sheet);
                    alert.setContentText("Make sure that you selected a platform and source file for "+sheet);

                    alert.showAndWait();
                    return false;
                }
            }
        }
        return true;
    }


    private void sheetsAndLanguagesUI(String sheet, List<String> languages, FlowPane rootPane,Stage primaryStage) {
        GridPane pane = new GridPane();
        ScrollPane sp = new ScrollPane();

        if (languages.size()>1) {
            Label label = new Label("Sheet name: "+sheet + "         ");

            pane.add(label,0,0);
            Label label2 = new Label("English Strings Path: ");
            pane.add(label2,0,1);
            TextField textfield = new TextField();
            pane.add(textfield,1,1);
            Button browse = new Button("Browse");
            browse.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Open Resource File");
                    File file = fileChooser.showOpenDialog(primaryStage);
                    textfield.setText(file.getPath());
                    sheetToTranslateMap.get(sheet).sourceFileName = file.getPath();
                }
            });
            pane.add(browse,2,1);
            Label platformLabel = new Label("Platform:");
            pane.add(platformLabel,0,2);
            final ToggleGroup group = new ToggleGroup();
            RadioButton iosRadio = new RadioButton("iOS");
            iosRadio.setToggleGroup(group);
            RadioButton androidRadio = new RadioButton("Android");
            androidRadio.setToggleGroup(group);
            iosRadio.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (iosRadio.isSelected()) {
                        sheetToTranslateMap.get(sheet).platform = "iOS";
                        androidRadio.setSelected(false);
                    }
                }
            });
            androidRadio.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (androidRadio.isSelected()) {
                        iosRadio.setSelected(false);
                        sheetToTranslateMap.get(sheet).platform = "Android";
                    }

                }
            });
            pane.add(iosRadio,0,3);
            pane.add(androidRadio,0,4);
            Label chooseLangugae = new Label("Choose languages to translate");
            pane.add(chooseLangugae,0,5);
            int index = 0;
            for (String language: languages) {
                if (!language.equals("key")) {
                    CheckBox checkBox = new CheckBox(language);
                    checkBox.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            if (checkBox.isSelected()) {
                                sheetToTranslateMap.get(sheet).languages.add(language);
                            }
                            else {
                                if (sheetToTranslateMap.get(sheet).languages.indexOf(language)!=-1) {
                                    sheetToTranslateMap.get(sheet).languages.remove(language);
                                }
                            }
                        }
                    });

                    pane.add(checkBox,index%3,6+index/3);
                    index++;
                }
            }
            sp.setContent(pane);
            sp.setFitToHeight(true);
            sp.setFitToWidth(true);
            rootPane.getChildren().add(sp);
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
