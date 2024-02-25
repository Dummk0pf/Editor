package my.projects;

import java.io.IOException;

public class TextEditor {

    private WindowsTerminal wTerminal = WindowsTerminal.getInstance();

    public void openNewFile(String path) {
        wTerminal.enableRawMode();
        while (true) {
            try {
                int key = System.in.read();
                System.out.println((char) key);
                if ((char) key == 'q') {
                    wTerminal.disableRawMode();
                    return;
                }
            } catch (IOException e) {
                System.out.println("Could Not Access IO");
            }

        }
    }
}
