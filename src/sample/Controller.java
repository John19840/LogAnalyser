package sample;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.DirectoryChooser;
import sample.FileTree.FilePath;
import sample.FileView.TabControler;
import sample.FileView.TabInfo;


import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

public class Controller {

    public String ROOT_FOLDER = "";

    @FXML
    TreeView<FilePath> treeView;

    TreeItem<FilePath> rootTreeItem;
    TreeItem<FilePath> filteredRoot;

    List<TabInfo> tabs = new ArrayList<>();

    @FXML
    Button btnDown;
    @FXML
    Button btnFile;
    @FXML
    Button btnUp;
    @FXML
    TabPane tabPane;
    @FXML
    TextField findField;
    @FXML
    TextField extField;
    @FXML
    Button findButton;
        //Обьект, содержащий методы котороля закладок открытых файлов
    TabControler tabControler = new TabControler();
    //кнопка загрузки корневой папки
    @FXML
    void loadRoot(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);

        if(selectedDirectory == null){
            //No Directory selected
        }else{
            ROOT_FOLDER = selectedDirectory.getAbsolutePath();
        }
    }
    //кнопки прокрутки вниз и вверх до найденного совпадения
    @FXML
    void Down(){
        if(!tabPane.getTabs().isEmpty()) {
            //определяет выделенную вкладку и дает ссылку на нее
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            //дает ссылку на listView вкладки
            ListView<String> list = (ListView<String>) tab.getContent();
            //находит обьект описывающий вкладку в списке
            TabInfo tabInfo = tabControler.tabFinder(tabs, tab);
            //если не достиг нижней границы то пролистываем до следующего совпадения
            if (!(tabInfo.indPosition == tabInfo.stringCount)) {
                tabInfo.indPosition++;
                list.scrollTo(tabInfo.findIndexes.get(tabInfo.indPosition));
            }
        }
    }
    @FXML
    void Up(){
        if(!tabPane.getTabs().isEmpty()) {

            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            ListView<String> list = (ListView<String>) tab.getContent();
            TabInfo tabInfo = tabControler.tabFinder(tabs, tab);
            if (!(tabInfo.indPosition == 0)) {
                tabInfo.indPosition--;


                list.scrollTo(tabInfo.findIndexes.get(tabInfo.indPosition));
            }
        }
    }
    //обработка двойного щелчка на нужном файле для открытия
    @FXML
    void fileClick() {
        //добавляем обработчик событий при двойном клике на файл
        treeView.setOnMouseClicked(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {

                try {
                    //берет путь файла для открытия
                    FilePath filePath = treeView.getSelectionModel().getSelectedItem().getValue();
                    //добавляет новую вкладку содержащую файл
                    addTable(filePath,findField.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //кнопка запускающая поиск строки в дереве папок
    @FXML
    void Search() throws IOException {
        if(!ROOT_FOLDER.equals("")) {
            //создает дерево и фильтрует его
            createTree();
            treeView.setRoot(rootTreeItem);
            filterChanged(extField.getText());
            if (!findField.getText().isEmpty())
                fileFilterChanged(findField.getText());
        }
    }
    //кнопка добавления новой вкладки
    @FXML
    void addTable(FilePath path, String filter) throws IOException {
        Tab tab = new Tab();
        TabInfo tabInfo = new TabInfo(tab);
        tabs.add(tabInfo);
        //добавляет название вкладки
        tab.setText(path.text);
        //добавляет на вкладку обьект listView в котором можно пролистывать файл
        tab.setContent(TabControler.getList(path.path.toString(),filter,tabInfo));
        tabPane.getTabs().add(tab);
        //делает только что открытую вкладку активной
        tabPane.getSelectionModel().select(tab);
    }

    //рекурсивный выбор папок с файлами с нужным расширением
    public void filter(TreeItem<FilePath> root, String filter, TreeItem<FilePath> filteredRoot) {

        for (TreeItem<FilePath> child : root.getChildren()) {

            TreeItem<FilePath> filteredChild = new TreeItem<>( child.getValue());
            filteredChild.setExpanded(true);

            filter(child, filter, filteredChild);

            if (!filteredChild.getChildren().isEmpty() || isExt(filteredChild.getValue(), filter)) {
                filteredRoot.getChildren().add(filteredChild);
            }

        }
    }

    //рекурсивный выбор файлов содержащих нужную строку
    public void fileFilter(TreeItem<FilePath> root, String filter, TreeItem<FilePath> filteredRoot) {

        for (TreeItem<FilePath> child : root.getChildren()) {

            TreeItem<FilePath> filteredChild = new TreeItem<>( child.getValue());
            filteredChild.setExpanded(true);

            fileFilter(child, filter, filteredChild);

            if (!filteredChild.getChildren().isEmpty()){
                filteredRoot.getChildren().add(filteredChild);
            }
            else if (isExt(filteredChild.getValue(), extField.getText()) && fileMatcher(filteredChild.getValue().getPath().toString(), filter)) {
                filteredRoot.getChildren().add(filteredChild);
            }

        }
    }

    //проверка расширения файла
    public boolean isExt(FilePath value, String extension) {
        return value.toString().endsWith(extension); // TODO: optimize or change (check file extension, etc)
    }
    //создает отображение корневого узла для дальнейшего добавления в него отображения папок и файлов
    public TreeItem<FilePath> createTreeRoot() {
        TreeItem<FilePath> root = new TreeItem<FilePath>( new FilePath( Paths.get(ROOT_FOLDER)));
        root.setExpanded(true);
        return root;
    }

    //рекурсивное создание древовидной структуры файлов и папок
    public void createTree() throws IOException {

        // create root
        rootTreeItem = createTreeRoot();

        // create tree structure recursively
        createTree(rootTreeItem);

        // sort tree structure by name
        rootTreeItem.getChildren().sort( Comparator.comparing(new Function<TreeItem<FilePath>, String>() {
            @Override
            public String apply(TreeItem<FilePath> t) {
                return t.getValue().toString().toLowerCase();
            }
        }));
    }
    public static void createTree(TreeItem<FilePath> rootItem) throws IOException {

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(rootItem.getValue().getPath())) {

            for (Path path : directoryStream) {

                TreeItem<FilePath> newItem = new TreeItem<FilePath>( new FilePath( path));
                newItem.setExpanded(true);

                rootItem.getChildren().add(newItem);

                if (Files.isDirectory(path)) {
                    createTree(newItem);
                }
            }
        }
        catch( Exception ex) {
            ex.printStackTrace();
        }
    }
    //проверка файла на наличие строки
    public boolean fileMatcher(String path, String pattern){
        FileInputStream inputStream = null;
        Scanner sc = null;
        String filetext = "";
        try {
            inputStream = new FileInputStream(path);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                filetext = sc.nextLine();
                if(filetext.contains(pattern)) return true;
            }
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (sc != null) {
                sc.close();
            }
        }
        return false;
    }
    //смена текущего отображения структуры папок на фильтрованное по расширению файла
    private void filterChanged(String filter) {
        if (filter.isEmpty()) {
            treeView.setRoot(rootTreeItem);
        }
        else {
            //создает узел и наполняет его папками и файлами, подходящими под фильтр
            filteredRoot = createTreeRoot();
            filter(rootTreeItem, filter, filteredRoot);
            treeView.setRoot(filteredRoot);
        }
    }
    //то же самое - фильтр по содержимому файла
    private void fileFilterChanged(String filter) {
        if (filter.isEmpty()) {
            treeView.setRoot(filteredRoot);
        }
        else {
            //берет дерево фильтрованное по расширению и фильтрует его по совпадению в файлах
            TreeItem<FilePath> fileFilteredRoot = createTreeRoot();
            fileFilter(filteredRoot, filter, fileFilteredRoot);
            treeView.setRoot(fileFilteredRoot);
        }
    }
}
