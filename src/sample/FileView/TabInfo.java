package sample.FileView;

import javafx.scene.control.Tab;

import java.util.ArrayList;
import java.util.List;

//класс хранения информации о вкладке, служит для отображения позиции в файле, при перемещении вверх/вниз к найденным совпадениям
public class TabInfo {
    //ссылка на вкладку
    public Tab tab;
    //хранение номеров строк, в которых найдены совпадения
    public List<Integer> findIndexes = new ArrayList<>();
    //позиция текущего совпадения
    public int indPosition = 0;
    //общее количество строк в файле
    public int stringCount = 0;

    public TabInfo(Tab tab) {
        this.tab = tab;
    }
}
