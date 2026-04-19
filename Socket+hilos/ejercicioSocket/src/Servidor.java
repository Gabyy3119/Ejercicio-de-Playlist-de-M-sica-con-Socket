import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Servidor - Playlist Compartida con Hilos
 * @author Gabriel Hidalgo
 */
public class Servidor {

    // Playlist compartida por todos los clientes
    static ArrayList<String> playlist = new ArrayList<>(); //La playlist pasó a static para que todos los clientes compartan la misma
    static int indiceActual = 0;

    public static void main(String[] args) {

        final int PUERTO = 5000;

        System.out.println("=== SERVIDOR - Playlist de Música ===");
        System.out.println("[LOG] Esperando conexión en el puerto " + PUERTO + "...");

        try {
            ServerSocket ss = new ServerSocket(PUERTO);

            while (true) { //sin el cliente handler, el while acepta un cliente y se queda pegado atendiendo a ese cliente hasta que termina. El Cliente 2 tiene que esperar.
                Socket sc = ss.accept();
                System.out.println("[LOG] Cliente conectado: " + sc.getInetAddress());

                // Por cada cliente se crea un hilo nuevo
                Thread hilo = new Thread(new ClienteHandler(sc)); // En el main en vez de atender al cliente directamente, crea un hilo:
                hilo.start();
                
                // el servidor queda LIBRE para aceptar al siguiente cliente

                System.out.println("[LOG] Hilo iniciado para: " + sc.getInetAddress());
            }

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }
}

class ClienteHandler implements Runnable { // la lógica está en ClienteHandler porque cada hilo necesita su propia copia de la lógica para atender a su cliente 
                                                                            //sin interferir con los demás.

    private Socket sc;

    public ClienteHandler(Socket sc) {
        this.sc = sc;
    }

    @Override
    public void run() {
        DataInputStream in;
        DataOutputStream out;

        try {
            in  = new DataInputStream(sc.getInputStream());
            out = new DataOutputStream(sc.getOutputStream());

            System.out.println("[LOG] ---- Inicio de sesión: " + sc.getInetAddress() + " ----");

            // Mensajes de bienvenida
            out.writeUTF("=== Bienvenido a tu Playlist de Música ===");
            out.writeUTF("Comandos disponibles:");
            out.writeUTF("  AGREGAR <cancion> - <artista>  → agrega una canción");
            out.writeUTF("  VER                            → muestra toda la playlist");
            out.writeUTF("  REPRODUCIR                     → reproduce la primera canción");
            out.writeUTF("  SIGUIENTE                      → pasa a la siguiente canción");
            out.writeUTF("  ANTERIOR                       → vuelve a la canción anterior");
            out.writeUTF("  ELIMINAR <número>              → elimina canción por número");
            out.writeUTF("  TOTAL                          → cantidad de canciones");
            out.writeUTF("  EXIT                           → salir");
            out.writeUTF("------------------------------------------");

            boolean sesionActiva = true;

            while (sesionActiva) {
                String mensajeCliente = in.readUTF();
                System.out.println("[LOG] " + sc.getInetAddress() + " envió: " + mensajeCliente);

                String upper = mensajeCliente.trim().toUpperCase();
                String respuesta;

                if (upper.equals("EXIT")) {
                    respuesta = "¡Hasta luego! La playlist tiene " + Servidor.playlist.size() + " canción(es).";
                    out.writeUTF(respuesta);
                    System.out.println("[LOG] Respuesta enviada: " + respuesta);
                    sesionActiva = false;

                } else if (upper.startsWith("AGREGAR")) {
                    String contenido = mensajeCliente.trim().substring(7).trim();
                    if (contenido.isEmpty()) {
                        respuesta = "Formato incorrecto. Usá: AGREGAR <cancion> - <artista>";
                    } else {
                        synchronized (Servidor.playlist) { //Se agregó synchronized en cada operación sobre la lista para evitar conflictos
                            Servidor.playlist.add(contenido); // synchronized que evita que dos hilos toquen la lista al mismo tiempo.
                        }
                        respuesta = "♪ '" + contenido + "' agregada a la playlist. (Total: " + Servidor.playlist.size() + ")";
                    }
                    out.writeUTF(respuesta);
                    System.out.println("[LOG] Respuesta enviada: " + respuesta);

                } else if (upper.equals("VER")) {
                    synchronized (Servidor.playlist) {
                        if (Servidor.playlist.isEmpty()) {
                            respuesta = "La playlist está vacía. Usá AGREGAR para añadir canciones.";
                            out.writeUTF(respuesta);
                            System.out.println("[LOG] Respuesta enviada: " + respuesta);
                        } else {
                            out.writeUTF("--- Playlist compartida (" + Servidor.playlist.size() + " canciones) ---");
                            for (int i = 0; i < Servidor.playlist.size(); i++) {
                                String marca = (i == Servidor.indiceActual) ? " ♪ (actual)" : "";
                                out.writeUTF((i + 1) + ". " + Servidor.playlist.get(i) + marca);
                            }
                            out.writeUTF("-------------------------------");
                            System.out.println("[LOG] Playlist enviada a " + sc.getInetAddress());
                        }
                    }

                } else if (upper.equals("REPRODUCIR")) {
                    synchronized (Servidor.playlist) {
                        if (Servidor.playlist.isEmpty()) {
                            respuesta = "La playlist está vacía. Agregá canciones primero.";
                        } else {
                            Servidor.indiceActual = 0;
                            respuesta = "♪ Reproduciendo: " + Servidor.playlist.get(Servidor.indiceActual) + " [" + (Servidor.indiceActual + 1) + "/" + Servidor.playlist.size() + "]";
                        }
                    }
                    out.writeUTF(respuesta);
                    System.out.println("[LOG] Respuesta enviada: " + respuesta);

                } else if (upper.equals("SIGUIENTE")) {
                    synchronized (Servidor.playlist) {
                        if (Servidor.playlist.isEmpty()) {
                            respuesta = "La playlist está vacía.";
                        } else if (Servidor.indiceActual < Servidor.playlist.size() - 1) {
                            Servidor.indiceActual++;
                            respuesta = "♪ Reproduciendo: " + Servidor.playlist.get(Servidor.indiceActual) + " [" + (Servidor.indiceActual + 1) + "/" + Servidor.playlist.size() + "]";
                        } else {
                            respuesta = "Ya estás en la última canción. Usá REPRODUCIR para volver al inicio.";
                        }
                    }
                    out.writeUTF(respuesta);
                    System.out.println("[LOG] Respuesta enviada: " + respuesta);

                } else if (upper.equals("ANTERIOR")) {
                    synchronized (Servidor.playlist) {
                        if (Servidor.playlist.isEmpty()) {
                            respuesta = "La playlist está vacía.";
                        } else if (Servidor.indiceActual > 0) {
                            Servidor.indiceActual--;
                            respuesta = "♪ Reproduciendo: " + Servidor.playlist.get(Servidor.indiceActual) + " [" + (Servidor.indiceActual + 1) + "/" + Servidor.playlist.size() + "]";
                        } else {
                            respuesta = "Ya estás en la primera canción.";
                        }
                    }
                    out.writeUTF(respuesta);
                    System.out.println("[LOG] Respuesta enviada: " + respuesta);

                } else if (upper.startsWith("ELIMINAR")) {
                    String[] partes = mensajeCliente.trim().split("\\s+");
                    if (partes.length < 2) {
                        respuesta = "Formato incorrecto. Usá: ELIMINAR <número>  Ej: ELIMINAR 2";
                    } else {
                        try {
                            int num = Integer.parseInt(partes[1]);
                            synchronized (Servidor.playlist) {
                                if (num < 1 || num > Servidor.playlist.size()) {
                                    respuesta = "Número inválido. Tenés " + Servidor.playlist.size() + " canción(es).";
                                } else {
                                    String eliminada = Servidor.playlist.remove(num - 1);
                                    if (Servidor.indiceActual >= Servidor.playlist.size() && Servidor.indiceActual > 0) {
                                        Servidor.indiceActual--;
                                    }
                                    respuesta = "🗑 '" + eliminada + "' eliminada de la playlist.";
                                }
                            }
                        } catch (NumberFormatException e) {
                            respuesta = "'" + partes[1] + "' no es un número válido.";
                        }
                    }
                    out.writeUTF(respuesta);
                    System.out.println("[LOG] Respuesta enviada: " + respuesta);

                } else if (upper.equals("TOTAL")) {
                    respuesta = "La playlist tiene " + Servidor.playlist.size() + " canción(es).";
                    out.writeUTF(respuesta);
                    System.out.println("[LOG] Respuesta enviada: " + respuesta);

                } else {
                    respuesta = "Comando no reconocido. Usá VER para ver los comandos disponibles.";
                    out.writeUTF(respuesta);
                    System.out.println("[LOG] Respuesta enviada: " + respuesta);
                }
            }

            System.out.println("[LOG] ---- Fin de sesión: " + sc.getInetAddress() + " ----");
            sc.close();

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }
}