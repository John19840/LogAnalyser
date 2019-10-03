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
import java.util.*;
import java.util.function.Function;

public class Controller {

    static boolean delete = true;

    public String ROOT_FOLDER = "";

    @FXML
    TreeView<FilePath> treeView;

    TreeItem<FilePath> rootTreeItem;
    TreeItem<FilePath> rootTreeItemDoubles;
    TreeItem<FilePath> filteredRoot;

    static Map<String,List<String>> map = new HashMap<>();

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
    @FXML
    Button searchUnusedBtn;
    @FXML
    Button deleteBtn;
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
    @FXML
    void searchDoubles() throws IOException {
        if(!ROOT_FOLDER.equals("")) {
            //создает дерево и фильтрует его
            createTree();
            treeView.setRoot(rootTreeItem);
            filterChanged(".properties");
                fileFilterChangedDoubles();
        }
    }
    @FXML
    void removeDoubles(){
        for (Map.Entry<String, List<String>> m : map.entrySet()) {
            for (String line : m.getValue()){
                removeLineFromFile(line,m.getKey());
            }
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
        tab.setContent(TabControler.getList(path.path.toString(), filter, tabInfo, map));
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
    public void fileFilterDoubles(TreeItem<FilePath> root, TreeItem<FilePath> filteredRoot) throws IOException {

        for (TreeItem<FilePath> child : root.getChildren()) {

            TreeItem<FilePath> filteredChild = new TreeItem<>( child.getValue());
            filteredChild.setExpanded(true);

            fileFilterDoubles(child, filteredChild);

            if (!filteredChild.getChildren().isEmpty()){
                filteredRoot.getChildren().add(filteredChild);
            }
            else if (isExt(filteredChild.getValue(), ".properties") && fileMatcherBuff(filteredChild.getValue().getPath().toString(),filteredRoot,root)) {
                filteredRoot.getChildren().add(filteredChild);
            }

        }
    }

    public boolean fileFilterKnowProperty(TreeItem<FilePath> root, String filter, TreeItem<FilePath> filteredRoot) {

        for (TreeItem<FilePath> child : root.getChildren()) {

            TreeItem<FilePath> filteredChild = new TreeItem<>( child.getValue());
            filteredChild.setExpanded(true);

            boolean b = fileFilterKnowProperty(child, filter, filteredChild);

            if (!filteredChild.getChildren().isEmpty()){
                if(b) return true;
            }
            else if (b || (isExt(filteredChild.getValue(), ".java") && fileMatcher(filteredChild.getValue().getPath().toString(), filter))) {
                return true;
            }
        }
        return false;
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
    public void createTreeDoubled() throws IOException {

        // create root
        rootTreeItemDoubles = createTreeRoot();

        // create tree structure recursively
        createTreeDoubled(rootTreeItemDoubles);

        // sort tree structure by name
        rootTreeItemDoubles.getChildren().sort( Comparator.comparing(new Function<TreeItem<FilePath>, String>() {
            @Override
            public String apply(TreeItem<FilePath> t) {
                return t.getValue().toString().toLowerCase();
            }
        }));
    }
    public static void createTreeDoubled(TreeItem<FilePath> rootItem) throws IOException {

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
//    public boolean fileMatcherBuff(String path, TreeItem<FilePath> filteredRoot, TreeItem<FilePath> root){
//        FileInputStream inputStream = null;
//        BufferedReader reader = null;
//        String filetext = "";
//        try {
//            inputStream = new FileInputStream(path);
//            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
//            String s;
//            while ((s = reader.readLine()) != null) {
//                String s2 = s.substring(0,s.indexOf("="));
//                s2 = s2.trim();
//                createTreeDoubled();
//                if(!fileFilterKnowProperty(rootTreeItemDoubles,s2,filteredRoot)){
//                    return true;
//                }
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
    public boolean fileMatcherBuff(String path, TreeItem<FilePath> filteredRoot, TreeItem<FilePath> root) throws IOException {
            map.put(path, new ArrayList<>());
            boolean b = false;
            FileInputStream inputStream = null;
            Scanner sc = null;
            inputStream = new FileInputStream(path);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNext()) {
                String line = sc.nextLine();
                if(line.isEmpty()) continue;
                String filetext = line.substring(0, line.indexOf("="));
                filetext = filetext.trim();
                createTreeDoubled();
                if (!fileFilterKnowProperty(rootTreeItemDoubles, filetext, filteredRoot)) {
                    b = true;
                    map.get(path).add(line);

                }
                if (sc.ioException() != null) {
                    throw sc.ioException();
                }
            }
            if (!b)
                map.remove(path);
            return b;
    }
    public static void removeLineFromFile(String lineToRemove, String file) {

        try {

            File inFile = new File(file);

            if (!inFile.isFile()) {
                System.out.println("Parameter is not an existing file");
                return;
            }

            // Construct the new file that will later be renamed to the original
            // filename.
            File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

            BufferedReader br = new BufferedReader(new FileReader(file));
            PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

            String line = null;

            // Read from the original file and write to the new
            // unless content matches data to be removed.
            while ((line = br.readLine()) != null) {

                if (!line.trim().equals(lineToRemove)) {

                    pw.println(line);
                    pw.flush();
                }
            }
            pw.close();
            br.close();

            // Delete the original file
            if (!inFile.delete()) {
                System.out.println("Could not delete file");
                return;
            }

            // Rename the new file to the filename the original file had.
            if (!tempFile.renameTo(inFile))
                System.out.println("Could not rename file");

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static boolean hasNextLine(Scanner scanner) {
        return scanner.hasNext() || scanner.hasNextLine();
    }

    private static String nextLine(Scanner scanner) {
        if (scanner.hasNext()) {
            return scanner.next();
        }
        return scanner.nextLine();
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
    private void fileFilterChangedDoubles() throws IOException {
            //берет дерево фильтрованное по расширению и фильтрует его по совпадению в файлах
            TreeItem<FilePath> fileFilteredRoot = createTreeRoot();
            fileFilterDoubles(filteredRoot, fileFilteredRoot);
            treeView.setRoot(fileFilteredRoot);
    }
}
