/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverinta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author raul
 */
public class Inicio {

    static final int defaultPort = 4040;

    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {
       
        System.out.println("/////Bienvenido a CCAire/////");
        // TODO code application logic here
        if(args.length == 0){
            //elige en que ip quieres inicializarlo
           System.out.println("Estas son las IP:");
           listadoIP();
           System.out.print("\n"
                   + "Por favor utilice las siguientes flags para configurar el servidor:\n"
                   + "  -i <IP>\n"
                   + "  -p <puerto> (por defecto será 4040)\n"
                   + "  -t <timeout> (por defecto no habrá)\n");
           return;
        }
        /*if(args[0].equals("prueba")){
            String respuesta = Server.ejecutar("pgrep watchdog");
            respuesta = respuesta.replaceAll(System.getProperty("line.separator"), ",").substring(0, respuesta.length()-1);
            Server.ejecutar("ps p "+respuesta+" -f");
            return;
        }*/
        
        String ip = null;
        int puerto = -1;
        int timeout = -1;
        boolean sudo = false;
        if(args.length%2!=0){
            System.err.println("Número de argumentos inválidos");
            return;
        }     
        for (int j = 0; j < args.length; j+=2) {
            if(args[j].charAt(0) == '-'){
                switch (args[j].charAt(1)) {
                case 'i':
                    if(!args[j+1].contains(".") || comprobarIP(args[j+1])!=0){
                        System.out.println("IP no válida.");
                        return;
                    }
                    ip = args[j+1];
                    break;
                case 'p':
                    if(Integer.parseInt(args[j+1])<1025 || Integer.parseInt(args[j+1])>65535){
                        System.out.println("Puerto inválido. Debe estar entre [1024,65535]");
                        return;
                    } 
                        
                    puerto = Integer.parseInt(args[j+1]);
                    break;
                case 't':
                    if(Integer.parseInt(args[j+1])<60 || Integer.parseInt(args[j+1])>3600){ 
                        System.out.println("timeout inválido. Debe estar entre [60,3600]");
                        return;
                    }
                    timeout = Integer.parseInt(args[j+1]);
                    break;
                default:
                    System.err.println("Flag no recocnocido" + args[j]);
                    return;
                }
            }
            else System.err.println("Argumento no reconocido "+args[j]);
        }
        
        if(ip == null){
            System.err.println("No se ha introducido ip");
            return;
        }
        else{
            
            if(esRoot()){
                System.out.println("Corriendo como root");
                sudo = true;
            }
            else{
                System.out.println("No corriendo como root. Si quiere configurar su tabla de enrutamiento debera correr el programa con el comando 'sudo' al principio.");
            }
            
            Server server;
            if(puerto == -1) server = new Server(defaultPort, sudo);
            else server = new Server(puerto, sudo);
            server.inicializar(ip);
            
            if(timeout != -1) server.setTimeout(timeout);
            server.run();
        }
        
    }
    
    private static void listadoIP() throws UnknownHostException, SocketException{
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        if (en == null) {
            System.out.println("No hay interfaces de red");
        } else while (en.hasMoreElements()) {
            NetworkInterface intf = en.nextElement();
            Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
            while (enumIpAddr.hasMoreElements()) {
                InetAddress ipAddr = enumIpAddr.nextElement();
                if(!intf.getName().equals("lo") && !ipAddr.getHostAddress().contains(":")){
                    System.out.print(ipAddr.getHostAddress());
                    System.out.println(" / "+intf.getName());
                }
            } 
        }
    }
    
    private static int comprobarIP(String ip) throws UnknownHostException, SocketException{
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        if (en == null) {
            System.out.println("No hay interfaces de red");
        } else while (en.hasMoreElements()) {
            NetworkInterface intf = en.nextElement();
            Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
            while (enumIpAddr.hasMoreElements()) {
                InetAddress ipAddr = enumIpAddr.nextElement();
                if(ipAddr.getHostAddress().equals(ip)){
                    return 0;
                }
            } 
        }
        return 1;
    }
    
    private static boolean esRoot() throws InterruptedException{
        try {
            Process p = Runtime.getRuntime().exec("id -u");
            p.waitFor(1500, TimeUnit.MILLISECONDS);
            BufferedReader buf = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            String line = "";
            String output = "";
            
            while ((line = buf.readLine()) != null) {
                output += line + "\n";
            }
            if(Integer.parseInt(output.trim()) == 0)
                return true;
            else return false;
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error:"+ex);
        }
        return false;
    }
    
}
