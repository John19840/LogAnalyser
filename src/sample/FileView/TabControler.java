package sample.FileView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

//класс для контроля создания новых вкладок и отображения на них текста файла с выделенными совпадениями
public class TabControler {
    //счетчик строк при чтении файла
    static int count = 0;
//поиск обьекта описывающего вкладку в коллекции, по ссылке на вкладку
    public TabInfo tabFinder(List<TabInfo> tabs, Tab tab){
        TabInfo tabI = null;
        for (TabInfo tabInfo : tabs) {
            if(tabInfo.tab==tab)tabI = tabInfo;
        }
        return tabI;
    }
//чтение строк из файла и добавление их в listView
    public static ListView<String> getList(String path, String filter, TabInfo tabInfo) throws FileNotFoundException {
        //счетчик строк при чтении из файла
        count = 0;
        //элемент FX для отображения строк файла
        ListView<String> list = new ListView<String>();
        //задание собственной фабрики ячеек, которая будет подсвечивать совпадения
        list.setCellFactory(listView -> new FindCell(filter));
        //список для чтения строк из файла
        ObservableList<String> items = FXCollections.observableArrayList();
        //нить для реализации многопоточности
        Thread t = new Thread()
        {
            public void run() {
                //процесс чтения из файла
                FileReader fileReader = null;
                try {
                    fileReader = new FileReader(path);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {

                    String line;
                    line = bufferedReader.readLine();
                    StringBuilder sb = new StringBuilder(line);
                    if(!line.isEmpty()&&sb.charAt(0)==65279)
                        line = sb.deleteCharAt(0).toString();
                    if(line.contains(filter))
                        tabInfo.findIndexes.add(count);
                    items.add(line);
                    count++;
                    while((line = bufferedReader.readLine()) != null) {
                        items.add(line);
                        //добавляет индексы строк в которых найдено совпадение в TabInfo
                        if(line.contains(filter))
                            tabInfo.findIndexes.add(count);
                        count++;
                    }
                    //добавление списка строк в элемент listView
                    list.setItems(items);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //добавление общего количества строк файла в TabInfo
                tabInfo.stringCount=count;
            }

        };
        t.setDaemon(true);
        t.start();
        return list;
    }
//фабрика ячеет для listView которая подсвечивает найденные совпадения строк при отображении файла
    public static class FindCell extends ListCell<String> {
        //строка которую нужно найти
        String filter;


        public FindCell(String filter) {
            this.filter = filter;
        }

        //переопределение стандартного метод cellFactory
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if(empty || item == null) {

                setText(null);
                setGraphic(null);

            } else {
                //если ячейка видима на экране то ее содержимое определяется
                setGraphic(createTextFlow(item));
                //...
            }
        }
        //проверяет текст ячейки на совпадение, подсвечивает если оно найдено
        private Node createTextFlow(String msg) {
            //если совпадение найдено то ячейка конструируется чтобы подсветить совпадение
            if(msg.contains(filter)&&!filter.isEmpty()){

                HBox hb = new HBox();
                String[] parts = msg.split(filter);

                for (int i = 0; i < parts.length; i++) {
                    if(parts[i].equals(""))continue;
                    else {
                        Text text = new Text(parts[i]);
                        hb.getChildren().add(text);
                        if ((i != parts.length - 1)) {
                            Text ftext = new Text(filter);
                            ftext.setFill(javafx.scene.paint.Color.RED);
                            hb.getChildren().add(ftext);
                        }
                    }
                }
                if(msg.startsWith(filter)) {
                    Text ftext = new Text(filter);
                    ftext.setFill(javafx.scene.paint.Color.RED);
                    hb.getChildren().add(0, ftext);
                }
                if(msg.endsWith(filter)||msg.endsWith(filter+"\n")) {
                    Text ftext = new Text(filter);
                    ftext.setFill(Color.RED);
                    hb.getChildren().add(ftext);
                }
                return hb;
            }
            else{
                //если совпадение не найдено, то конструируется ячейка без подсветки
                HBox hb = new HBox();
                Text text = new Text(msg);
                hb.getChildren().add(text);
                return hb;
            }
        }
    }
}
