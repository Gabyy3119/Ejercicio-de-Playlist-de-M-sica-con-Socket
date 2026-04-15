import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


/**
 * Servidor - Playlist de Música
 * @author Gabriel Hidalgo
 */
public class Servidor {

    public static void main(String[] args) {

        final int PUERTO = 5000;
        DataInputStream in;
        DataOutputStream out;

        System.out.println("=== SERVIDOR - Playlist de Música ===");
        System.out.println("[LOG] Esperando conexión en el puerto " + PUERTO + "...");

        try {
            ServerSocket ss = new ServerSocket(PUERTO);

            while (true) {
                Socket sc = ss.accept();
                System.out.println("[LOG] Cliente conectado: " + sc.getInetAddress());

                in  = new DataInputStream(sc.getInputStream());
                out = new DataOutputStream(sc.getOutputStream());

                // Lista de canciones y índice de reproducción actual
                ArrayList<String> playlist = new ArrayList<>();
                int indiceActual = 0;

                System.out.println("[LOG] ---- Inicio de sesión ----");

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
                    System.out.println("[LOG] Recibido: " + mensajeCliente);

                    String upper = mensajeCliente.trim().toUpperCase();
                    String respuesta;

                    if (upper.equals("EXIT")) {
                        respuesta = "¡Hasta luego! Tenías " + playlist.size() + " canción(es) en tu playlist.";
                        out.writeUTF(respuesta);
                        System.out.println("[LOG] Respuesta enviada: " + respuesta);
                        sesionActiva = false;

                    } else if (upper.startsWith("AGREGAR")) {
                        String contenido = mensajeCliente.trim().substring(7).trim();
                        if (contenido.isEmpty()) {
                            respuesta = "Formato incorrecto. Usá: AGREGAR <cancion> - <artista>";
                        } else {
                            playlist.add(contenido);
                            respuesta = "♪ '" + contenido + "' agregada a la playlist. (Total: " + playlist.size() + ")";
                        }
                        out.writeUTF(respuesta);
                        System.out.println("[LOG] Respuesta enviada: " + respuesta);

                    } else if (upper.equals("VER")) {
                        if (playlist.isEmpty()) {
                            respuesta = "Tu playlist está vacía. Usá AGREGAR para añadir canciones.";
                            out.writeUTF(respuesta);
                            System.out.println("[LOG] Respuesta enviada: " + respuesta);
                        } else {
                            out.writeUTF("--- Tu Playlist (" + playlist.size() + " canciones) ---");
                            for (int i = 0; i < playlist.size(); i++) {
                                String marca = (i == indiceActual) ? " ♪ (actual)" : "";
                                out.writeUTF((i + 1) + ". " + playlist.get(i) + marca);
                            }
                            out.writeUTF("-------------------------------");
                            System.out.println("[LOG] Playlist enviada al cliente.");
                        }

                    } else if (upper.equals("REPRODUCIR")) {
                        if (playlist.isEmpty()) {
                            respuesta = "Tu playlist está vacía. Agregá canciones primero.";
                        } else {
                            indiceActual = 0;
                            respuesta = "♪ Reproduciendo: " + playlist.get(indiceActual) + " [" + (indiceActual + 1) + "/" + playlist.size() + "]";
                        }
                        out.writeUTF(respuesta);
                        System.out.println("[LOG] Respuesta enviada: " + respuesta);

                    } else if (upper.equals("SIGUIENTE")) {
                        if (playlist.isEmpty()) {
                            respuesta = "Tu playlist está vacía.";
                        } else if (indiceActual < playlist.size() - 1) {
                            indiceActual++;
                            respuesta = "♪ Reproduciendo: " + playlist.get(indiceActual) + " [" + (indiceActual + 1) + "/" + playlist.size() + "]";
                        } else {
                            respuesta = "Ya estás en la última canción. Usá REPRODUCIR para volver al inicio.";
                        }
                        out.writeUTF(respuesta);
                        System.out.println("[LOG] Respuesta enviada: " + respuesta);

                    } else if (upper.equals("ANTERIOR")) {
                        if (playlist.isEmpty()) {
                            respuesta = "Tu playlist está vacía.";
                        } else if (indiceActual > 0) {
                            indiceActual--;
                            respuesta = "♪ Reproduciendo: " + playlist.get(indiceActual) + " [" + (indiceActual + 1) + "/" + playlist.size() + "]";
                        } else {
                            respuesta = "Ya estás en la primera canción.";
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
                                if (num < 1 || num > playlist.size()) {
                                    respuesta = "Número inválido. Tenés " + playlist.size() + " canción(es).";
                                } else {
                                    String eliminada = playlist.remove(num - 1);
                                    if (indiceActual >= playlist.size() && indiceActual > 0) {
                                        indiceActual--;
                                    }
                                    respuesta = "🗑 '" + eliminada + "' eliminada de la playlist.";
                                }
                            } catch (NumberFormatException e) {
                                respuesta = "'" + partes[1] + "' no es un número válido.";
                            }
                        }
                        out.writeUTF(respuesta);
                        System.out.println("[LOG] Respuesta enviada: " + respuesta);

                    } else if (upper.equals("TOTAL")) {
                        respuesta = "Tu playlist tiene " + playlist.size() + " canción(es).";
                        out.writeUTF(respuesta);
                        System.out.println("[LOG] Respuesta enviada: " + respuesta);

                    } else {
                        respuesta = "Comando no reconocido. Usá VER para ver los comandos disponibles.";
                        out.writeUTF(respuesta);
                        System.out.println("[LOG] Respuesta enviada: " + respuesta);
                    }
                }

                System.out.println("[LOG] ---- Fin de sesión ----");
                sc.close();
                System.out.println("[LOG] Cliente desconectado. Esperando nueva conexión...\n");
            }

        } catch (IOException ex) {
    System.out.println("Error: " + ex.getMessage());
        }
    }
}