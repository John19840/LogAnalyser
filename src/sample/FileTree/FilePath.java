package sample.FileTree;

import java.nio.file.Path;

public class FilePath {

    public Path path;
    public String text;

    public FilePath(Path path) {

        this.path = path;

        // display text: the last path part
        // consider root, e. g. c:\
        if( path.getNameCount() == 0) {
            this.text = path.toString();
        }
        // consider folder structure
        else {
            this.text = path.getName( path.getNameCount() - 1).toString();
        }

    }

    public Path getPath() {
        return path;
    }

    public String toString() {

        // hint: if you'd like to see the entire path, use this:
        // return path.toString();

        // show only last path part
        return text;

    }
}
