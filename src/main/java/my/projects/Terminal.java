package my.projects;

public interface Terminal {
    void enableRawMode();

    void disableRawMode();

    WindowSize getWindowSize();
}

record WindowSize(int rows, int columns) {

}
