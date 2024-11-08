import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;



public class Server {
    static int PORT = 49230; // พอร์ตที่เซิร์ฟเวอร์จะทำงาน
    static String FILES_DIR = "C:\\Users\\User\\Desktop\\Videotest"; // ไดเร็กทอรีที่เก็บไฟล์ที่เซิร์ฟเวอร์สามารถส่งให้กับลูกค้าได้

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                // ส่งรายชื่อไฟล์ไปยังลูกค้า
                File directory = new File(FILES_DIR);
                File[] files = directory.listFiles();
                List<String> fileNames = new ArrayList<>();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            fileNames.add(file.getName());
                        }
                    }
                }
                dos.writeInt(fileNames.size());
                for (String fileName : fileNames) {
                    dos.writeUTF(fileName);
                }

                // รับชื่อไฟล์ที่ลูกค้าต้องการและวิธีการส่ง
                String requestedFileName = dis.readUTF();
                int transferMethod = dis.readInt(); // รับวิธีการส่ง
                System.out.println("Client requested file: " + requestedFileName);

                File fileToSend = new File(FILES_DIR + File.separator + requestedFileName);
                if (fileToSend.exists()) {
                    dos.writeLong(fileToSend.length());

                    if (transferMethod == 2) { // Zero-copy
                        try (FileInputStream fis = new FileInputStream(fileToSend);
                             FileChannel fileChannel = fis.getChannel();
                             WritableByteChannel writableChannel = Channels.newChannel(dos)) {
                            
                            long position = 0;
                            long size = fileChannel.size();
                            while (position < size) {
                                position += fileChannel.transferTo(position, size - position, writableChannel);
                            }
                        }
                    } else { // Regular copy
                        try (FileInputStream fis = new FileInputStream(fileToSend)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = fis.read(buffer)) != -1) {
                                dos.write(buffer, 0, read);
                            }
                        }
                    }
                    System.out.println("Sent file: " + requestedFileName + " to client.");
                } else {
                    dos.writeLong(0); // แจ้งให้ลูกค้าทราบว่าไม่พบไฟล์
                    System.out.println("Requested file not found: " + requestedFileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
