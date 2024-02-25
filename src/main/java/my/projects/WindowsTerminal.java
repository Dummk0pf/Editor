package my.projects;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class WindowsTerminal implements Terminal {
    private static WindowsTerminal wTerminal;
    private IntByReference inMode;
    private IntByReference outMode;

    private WindowsTerminal() {

    }

    public static WindowsTerminal getInstance() {
        if (wTerminal == null) {
            wTerminal = new WindowsTerminal();
        }
        return wTerminal;
    }

    @Override
    public void enableRawMode() {
        Pointer inHandle = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_INPUT_HANDLE);

        inMode = new IntByReference();
        Kernel32.INSTANCE.GetConsoleMode(inHandle, inMode);

        int inMode;
        inMode = this.inMode.getValue() & ~(Kernel32.ENABLE_ECHO_INPUT
                | Kernel32.ENABLE_LINE_INPUT
                | Kernel32.ENABLE_MOUSE_INPUT
                | Kernel32.ENABLE_WINDOW_INPUT
                | Kernel32.ENABLE_PROCESSED_INPUT);

        inMode |= Kernel32.ENABLE_VIRTUAL_TERMINAL_INPUT;

        Kernel32.INSTANCE.SetConsoleMode(inHandle, inMode);

        Pointer outHandle = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
        outMode = new IntByReference();
        Kernel32.INSTANCE.GetConsoleMode(outHandle, outMode);

        int outMode = this.outMode.getValue();
        outMode |= Kernel32.ENABLE_VIRTUAL_TERMINAL_PROCESSING;
        outMode |= Kernel32.ENABLE_PROCESSED_OUTPUT;
        Kernel32.INSTANCE.SetConsoleMode(outHandle, outMode);

    }

    @Override
    public void disableRawMode() {
        Pointer inHandle = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
        Kernel32.INSTANCE.SetConsoleMode(inHandle, inMode.getValue());

        Pointer outHandle = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
        Kernel32.INSTANCE.SetConsoleMode(outHandle, outMode.getValue());
    }

    @Override
    public WindowSize getWindowSize() {
        final Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
        final Kernel32 instance = Kernel32.INSTANCE;
        final Pointer handle = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
        instance.GetConsoleScreenBufferInfo(handle, info);
        return new WindowSize(info.windowHeight(), info.windowWidth());
    }
}
