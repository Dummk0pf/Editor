package my.projects;

import java.io.IOException;

public class TextEditor {

    private WindowsTerminal wTerminal;
    private int rows;
    private int columns;

    private final static int ARROW_UP = 1000;
    private final static int ARROW_DOWN = 1001;
    private final static int ARROW_LEFT = 1002;
    private final static int ARROW_RIGHT = 1003;
    private final static int HOME = 1004;
    private final static int END = 1005;
    private final static int PAGE_UP = 1006;
    private final static int PAGE_DOWN = 1007;
    private final static int DELETE = 1008;

    public TextEditor() {
        wTerminal = WindowsTerminal.getInstance();
        rows = wTerminal.getWindowSize().rows();
        columns = wTerminal.getWindowSize().columns();
    }

    public void openNewFile(String path) {
        boolean canRead = true;
        startListening();
        while (canRead) {
            try {
                // refreshScreen();
                int key = readKey();
                canRead = keyAction(key);

            } catch (IOException e) {
                System.out.println("Could Not Access IO");
            }

        }
    }

    private void refreshScreen() {

        StringBuilder screen = new StringBuilder();
        String title = "\033[7mEditor";
        int pad = columns - title.length();
        title = title + " ".repeat(pad) + "\033[0m\r\n";

        screen.append("\033[2J");
        screen.append(title);
        for (int i = 1; i < rows - 1; i++) {
            screen.append("$\r\n");
        }
        screen.append("\033[2H");

        System.out.print(screen.toString());
    }

    private void startListening() {
        wTerminal.enableRawMode();
    }

    private int readKey() throws IOException {
        int key = System.in.read();

        // esc -> \033

        // esc[A -> Up
        // esc[B -> Down
        // esc[C -> Right
        // esc[D -> Left
        // esc[H -> Home
        // esc[F -> End

        // esc[3~ -> Delete
        // esc[5~ -> Page up
        // esc[6~ -> Page down

        if (key != '\033') {
            return key;
        }

        key = System.in.read();

        if (key != '[') {
            return key;
        }

        key = System.in.read();

        return switch (key) {
            case 'A' -> ARROW_UP;
            case 'B' -> ARROW_DOWN;
            case 'C' -> ARROW_RIGHT;
            case 'D' -> ARROW_LEFT;
            case 'H' -> HOME;
            case 'F' -> END;
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                int nextKey = System.in.read();
                if (nextKey != '~') {
                    yield key;
                }
                yield switch (key) {
                    case '3' -> DELETE;
                    case '5' -> PAGE_UP;
                    case '6' -> PAGE_DOWN;
                    default -> key;
                };
            }

            default -> key;
        };
    }

    private boolean keyAction(int key) {
        System.out.println((char) key + " -> " + key);
        if ((char) key == 'q') {
            stopListening();
            return false;
        }
        return true;
    }

    private void stopListening() {
        clearScreen();
        wTerminal.disableRawMode();
    }

    private void clearScreen() {
        System.out.print("\033[2J");
        System.out.print("\033[H");
    }
}
