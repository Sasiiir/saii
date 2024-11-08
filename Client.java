import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;



public class Client {
    static final String SERVER_ADDRESS = "localhost";
    static final int SERVER_PORT = 49230;
    static final String CLIENT_DIR = "C:\\Users\\User\\Desktop\\client";

    public static void main(String[] args) {
        Socket socket = null;
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            // Receive the list of files from the server
            int fileCount = dis.readInt();
            String[] fileNames = new String[fileCount];
            for (int i = 0; i < fileCount; i++) {
                fileNames[i] = dis.readUTF();
            }

            System.out.println("Files available on the server:");
            for (String fileName : fileNames) {
                System.out.println(fileName);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter the name of the file you want to download: ");
            String requestedFileName = br.readLine();
            System.out.print("Choose transfer method: 1 for Regular, 2 for Zero Copy: ");
            int transferMethod = Integer.parseInt(br.readLine());

            // Send the requested file name and transfer method to the server
            dos.writeUTF(requestedFileName);
            dos.writeInt(transferMethod);

            // Receive the file from the server
            long fileSize = dis.readLong();
            if (fileSize > 0) {
                File clientDir = new File(CLIENT_DIR);
                if (!clientDir.exists()) {
                    clientDir.mkdirs();
                }
                File outputFile = new File(CLIENT_DIR + File.separator + requestedFileName);

                // Start timing the download
                long startTime = System.currentTimeMillis();

                if (transferMethod == 2) {
                    // Simulated Zero-Copy: using NIO Channels for improved performance
                    receiveFileWithChannel(socket, outputFile, fileSize);
                } else {
                    // Regular copy method
                    receiveFileRegular(dis, outputFile, fileSize);
                }

                // Stop timing and calculate elapsed time
                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime; // in milliseconds

                System.out.printf("File downloaded successfully in %d milliseconds.\n", elapsedTime);
            } else {
                System.out.println("File not found on the server.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void receiveFileRegular(DataInputStream dis, File outputFile, long fileSize) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192];
            int read;
            System.out.println("Downloading file...");
            while (fileSize > 0 && (read = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                fos.write(buffer, 0, read);
                fileSize -= read;
            }
            System.out.println("Download complete.");
        }
    }

    private static void receiveFileWithChannel(Socket socket, File outputFile, long fileSize) throws IOException {
        try (ReadableByteChannel readableChannel = Channels.newChannel(socket.getInputStream());
             FileChannel fileChannel = new FileOutputStream(outputFile).getChannel()) {

            System.out.println("Downloading file using simulated zero-copy...");
            long totalRead = 0;
            long position = 0;

            while (totalRead < fileSize) {
                long transferred = fileChannel.transferFrom(readableChannel, position, fileSize - totalRead);
                if (transferred <= 0) break;

                totalRead += transferred;
                position += transferred;
            }
            System.out.println("Download complete.");
        }
    }
}
