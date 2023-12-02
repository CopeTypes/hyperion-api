package hyper.ion.api.extensions;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import javax.swing.*;

public class DisplayWindow {
    private static final int DEFAULT_WIDTH = 500;
    private static final int DEFAULT_HEIGHT = 300;
    private static final String DEFAULT_TITLE = "Hyperion ExternalUI";

    private int width;
    private int height;
    private String title;
    private Color textColor;
    private Color backgroundColor;
    private final JFrame frame;
    private final JTextArea textArea;

    private boolean running;

    public DisplayWindow() {
        this.width = DEFAULT_WIDTH;
        this.height = DEFAULT_HEIGHT;
        this.title = DEFAULT_TITLE;
        this.textColor = Color.WHITE;
        this.backgroundColor = Color.BLACK;
        this.running = true;

        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setBackground(backgroundColor);

        textArea = new JTextArea();
        textArea.setForeground(textColor);
        textArea.setBackground(backgroundColor);
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        frame.setVisible(true);
    }

    public void updateText(String text) {
        textArea.setText(text);
        scaleTextArea();
        textArea.revalidate();
    }

    private void scaleTextArea() {
        int titleWidth = frame.getFontMetrics(frame.getFont()).stringWidth(title);
        int preferredWidth = textArea.getPreferredSize().width + titleWidth;
        int preferredHeight = textArea.getPreferredSize().height + 50;

        if (preferredWidth > width || preferredHeight > height) {
            width = Math.max(preferredWidth, width);
            height = Math.max(preferredHeight, height);
            frame.setSize(width, height);
        }
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        textArea.setForeground(textColor);
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        frame.setBackground(backgroundColor);
        textArea.setBackground(backgroundColor);
    }

    public void setTitle(String title) {
        this.title = title;
        frame.setTitle(title);
    }

    public static void start(String[] args) {
        int serverPort = 8888;
        if (args.length > 0) serverPort = Integer.parseInt(args[0]);
        DisplayWindow displayWindow = new DisplayWindow();
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("UI Server listening on port " + serverPort);

            while (displayWindow.running) { // command loop
                try {
                    Socket clientSocket = serverSocket.accept();
                    String currentClient = clientSocket.getInetAddress().getHostAddress();
                    System.out.println("Client connected: " + currentClient);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                    String command;
                    while ((command = reader.readLine()) != null) {
                        //System.out.println("Received command from client: " + command);
                        String response = processCommand(command, displayWindow);
                        writer.println(response);
                    }

                    clientSocket.close();
                    System.out.println("Client disconnected: " + currentClient);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String processCommand(String command, DisplayWindow window) {
        String[] parts = command.split(":");
        switch (parts[0]) {
            case "text" -> {
                String[] lines = command.replace("text:", "").split("\\|");
                window.updateText(String.join("\n", lines));
                return "Window text updated.";
            }
            case "textColor" -> {
                int red = Integer.parseInt(parts[1].trim());
                int green = Integer.parseInt(parts[2].trim());
                int blue = Integer.parseInt(parts[3].trim());
                window.setTextColor(new Color(red, green, blue));
                return "Window text color set.";
            }
            case "backgroundColor" -> {
                int red = Integer.parseInt(parts[1].trim());
                int green = Integer.parseInt(parts[2].trim());
                int blue = Integer.parseInt(parts[3].trim());
                window.setBackgroundColor(new Color(red, green, blue));
                return "Window background color set.";
            }
            case "title" -> {
                String title = parts[1].trim();
                window.setTitle(title);
                return "Window title set.";
            }
            case "quit" -> {
                System.out.println("Shutting down.");
                window.setRunning(false);
                return "Shutting down.";
            }
        }
        return "Bad command or syntax.";
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}

