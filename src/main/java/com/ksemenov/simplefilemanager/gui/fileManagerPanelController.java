package com.ksemenov.simplefilemanager.gui;

import com.ksemenov.simplefilemanager.engine.DiskInfo;
import com.ksemenov.simplefilemanager.engine.FileInfo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;



public class fileManagerPanelController implements Initializable {

    @FXML
    TableView<FileInfo> filesTable;

    @FXML
    ComboBox<DiskInfo> disksBox;

    @FXML
    TextField currentPath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        List<File> drives = new ArrayList<>();
        for (Path p:FileSystems.getDefault().getRootDirectories()) {
                drives.add(p.toFile());
            };

        disksBox.getItems().clear();

        for (File drive:drives) {
            try {
                disksBox.getItems().add(new DiskInfo(drive));
            } catch (IOException e) {
                throw new RuntimeException("Unable to collect drives");
            }
        }
        disksBox.setMaxWidth(70);

        disksBox.setButtonCell(new ListCell<DiskInfo>(){
            @Override
            protected void updateItem(DiskInfo item, boolean empty) {
                super.updateItem(item, empty);
                if(item!=null||!empty){
                    setText(item.getDiskName());
                }

            }
        });
        disksBox.setCellFactory(row -> new ComboBoxListCell<DiskInfo>(){
            @Override
            public void updateItem(DiskInfo item, boolean empty) {
                super.updateItem(item, empty);
                if(item!=null||!empty){
                    setText(item.getDiskAbsPath()+" ("+item.getDiskName()+")");
                }
            }

        });

        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileType().getName()));
        fileTypeColumn.setPrefWidth(25);



        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileNameColumn.setPrefWidth(250);


        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getFileSize()));
        fileSizeColumn.setPrefWidth(150);


        fileSizeColumn.setCellFactory(column-> new TableCell<FileInfo, Long>(){
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if(item == null || empty){
                    setText(null);
                    setStyle("");
                } else {
                    String str = item.toString();

                    if(item==-1L){
                        str = "[DIR]";
                    }
                    else
                    {
                        str = String.format("%,d bytes", item);
                    }
                    setText(str);
                }

            }
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> lastModifyDateColumn = new TableColumn<>("Last Modify Date");
        lastModifyDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModifiedDate().format(dtf)));
        lastModifyDateColumn.setPrefWidth(150);
        filesTable.getColumns().addAll(fileTypeColumn,fileNameColumn,fileSizeColumn, lastModifyDateColumn);

        updateFileList(Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize());


    }

    public void updateFileList(Path path){
        synchronizePathData(path);
        try {
            List<Path> pp = Files.list(path).collect(Collectors.toList());
            for (Path p: Files.list(path).collect(Collectors.toList())) {
                filesTable.getItems().add(new FileInfo(p));
            }
            filesTable.sort();

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING,"Can't refresh files list", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void synchronizePathData(Path path) {
        currentPath.clear();
        currentPath.setText(path.normalize().toAbsolutePath().toString());
        filesTable.getItems().clear();
        for (DiskInfo di: disksBox.getItems()) {
            if(di.getFile().toPath().getRoot().equals(path.getRoot())){
               disksBox.setValue(di);
            }
        }

    }


    @FXML
    public void onPathUpClick(ActionEvent actionEvent) {
        Path path = Paths.get(currentPath.getText()).getParent();
        if(path!= null)
            updateFileList(path);
    }


    @FXML
    public void onDiskBoxAction(ActionEvent actionEvent) {
        Path path;
        ComboBox<DiskInfo> cb = (ComboBox<DiskInfo>)actionEvent.getSource();
        path = Paths.get(cb.getSelectionModel().getSelectedItem().getDiskAbsPath());

        if(path!= null){
            updateFileList(path);
        }
    }

    @FXML
    public void onFilesTableClick(MouseEvent mouseEvent) {

        if(mouseEvent.getClickCount() == 2){
            FileInfo fi = filesTable.getSelectionModel().getSelectedItem();
            if(fi.getFileType() == FileInfo.FileType.DIRECTORY) {
                updateFileList(fi.getFilePath());
            }
        }
    }

    public void onGoPathClick(ActionEvent actionEvent) {
        Path p = Paths.get(currentPath.getText());
        updateFileList(p.normalize().toAbsolutePath());

    }
}
