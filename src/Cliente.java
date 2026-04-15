import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;


/**
 * Cliente - Playlist de Música
 * @author Gabriel Hidalgo (adaptado)
 */
public class Cliente {

    public static void main(String[] args) {

        final String HOST   = "127.0.0.1";
        final int    PUERTO = 5000;
        DataInputStream  in;
        DataOutputStream out;

        System.out.println("=== CLIENTE - Playlist de Música ===");
        System.out.println("Conectando a " + HOST + ":" + PUERTO + "...");

        try {
            Socket sc = new Socket(HOST, PUERTO);
            System.out.println("¡Conexión establecida!\n");

            in  = new DataInputStream(sc.getInputStream());
            out = new DataOutputStream(sc.getOutputStream());

            Scanner teclado = new Scanner(System.in);

            // Leer los 10 mensajes de bienvenida del servidor
            for (int i = 0; i < 10; i++) {
                System.out.println("[Servidor]: " + in.readUTF());
            }

            boolean activo = true;

            while (activo) {
                System.out.print("\nTu comando: ");
                String input = teclado.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                out.writeUTF(input);

                // El comando VER devuelve múltiples líneas, el resto una sola
                if (input.toUpperCase().equals("VER")) {
                    String linea;
                    while (!(linea = in.readUTF()).equals("-------------------------------")) {
                        System.out.println("[Servidor]: " + linea);
                    }
                    System.out.println("[Servidor]: -------------------------------");
                } else {
                    String respuesta = in.readUTF();
                    System.out.println("[Servidor]: " + respuesta);

                    if (input.equalsIgnoreCase("EXIT")) {
                        activo = false;
                    }
                }
            }

            System.out.println("\nCerrando cliente. ¡Hasta luego!");
            sc.close();

        } catch (IOException ex) {
    System.out.println("Error: " + ex.getMessage());
        }
    }
}