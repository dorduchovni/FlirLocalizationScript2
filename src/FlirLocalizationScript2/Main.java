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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Main extends Application {
    private String filepath;
    private Map<String,SheetToTranslate> sheetToTranslateMap = new LinkedHashMap<>();

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Localization");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        FlowPane flowPane =(FlowPane) ((AnchorPane)((ScrollPane)root.getChildrenUnmodifiable().get(0)).getContent()).getChildren().get(0);
        Button browseBtn = (Button) (flowPane).lookup("#browseBtn") ;
        Button startBtn = (Button) (flowPane).lookup("#startBtn");
        Button resetBtn = (Button) (flowPane).lookup("#resetBtn");
        javafx.scene.control.TextField browseTextfield = (TextField) (flowPane).lookup("#filepathTextView");
        ScrollPane sp = new ScrollPane();
        sp.setContent(flowPane);
        flowPane.setVgap(5);
        flowPane.setHgap(20);


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
                    LinkedHashMap<String, java.util.List<String>> sheetsAndLanguages = Manager.getSheetsAndLanguages(filepath);
                    for (String sheet: sheetsAndLanguages.keySet()) {
                        sheetToTranslateMap.put(sheet,new SheetToTranslate());
                        sheetToTranslateMap.get(sheet).languages = new ArrayList<String>();
                    }
                    for (String sheet: sheetsAndLanguages.keySet()) {
                        sheetsAndLanguagesUI(sheet, sheetsAndLanguages.get(sheet),flowPane, primaryStage);
                    }
            //        primaryStage.setMaximized(true);
              //      primaryStage.setResizable(true);


                    startBtn.setDisable(true);
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
                                    Manager.translateSheetsMap(sendToManager,filepath);
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
                    flowPane.getChildren().add(translateBtn);
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
