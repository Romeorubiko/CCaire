/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverinta;

import com.sun.org.apache.bcel.internal.util.ByteSequence;
import com.sun.org.apache.xml.internal.serialize.LineSeparator;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author raul
 */
public class Server {
    private int puerto;
    private DatagramSocket socket;
    private boolean running;
    private byte[] buffer; //buffer para recibir y enviar datos
    private String ip; //ip actual del servidor
    private boolean root; //es usuario con privilegios?
    
    private ArrayList<Watcher> watchers; //lista de watchers
    private Timer timerWatchers; //la comprobación de watchers periodica
    private int refrescoWatchers; //cada cuanto se comprueban los watchers en ms
    
    private final String WATCHERSGUARDADOS = "Wr.txt";
    
    public static final char APAGAR = '0';
    public static final char DAME_IP = '1';
    public static final char DAME_ENRUTAMIENTO = '2';
    public static final char CAMBIO_IP = '3';
    public static final char BORRA_IP = '4';
    public static final char ANADE_IP = '5';
    public static final char INFO_PROCESO = '6';
    public static final char MATAR_PROCESO = '7';
    public static final char RELANZAR = '8';
    public static final char ANADE_W = '9';
    public static final char BORRA_W = 'A';
    public static final char ES_W = 'B';
    public static final char ERES_ROOT = 'C';
    public static final char DAME_TIMEOUT = 'D';
    public static final char CAMBIA_TIMEOUT = 'E';
    public static final char DAME_W_Y_C = 'F';
    public static final char DAME_PATH = 'G';
    public static final char DAME_PID_ESTADO = 'H';
    
    
    public Server(int port, boolean root){
        this.puerto = port;
        this.root = root;
        watchers = new ArrayList();
        try{
            leerWathcersGuardados();
        }
        catch(Exception e){
            System.out.println("no se han leido watchers");
        }
        refrescoWatchers = 8000; //valor por defecto en ms
    }
    
    public int inicializar(String ip){
        timerWatchers = new Timer();
        timerWatchers.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
               ejecutarWatchers((ArrayList<Watcher>)watchers.clone());
            }
        }, 0, refrescoWatchers);
        try {
            socket = new DatagramSocket(puerto);
            //socket.setSoTimeout(1000);
            this.ip = ip;
            System.out.println("Servidor inicializado. IP: "+ ip); 
            System.out.println("Escuchando en puerto: "+puerto);
        }
        catch (IOException e) {
            System.err.println("No se pudo crear socket en puerto: "+this.puerto);
            return -1;
        }
        
        return 0;
    }
    
    public void run() throws InterruptedException, UnknownHostException, SocketException{
        running = true;
        while (running) {
            /*System.out.print("WatcherArray:[");
            for (Watcher watcher : watchers) System.out.print(watcher.getProceso()+",");
            System.out.println("]");*/
            
            buffer = new byte[512];
            String mensaje = "";
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            System.out.println("______________________________");
            System.out.print("["+new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime())+"]");
            System.out.println("Esperando peticion...");
            try {
                socket.receive(packet);
            } catch (IOException ex) {
                System.out.print("["+new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime())+"]");
                System.out.println("Sin mensajes.");
                continue;
            }
                
            InetAddress address = packet.getAddress();
            int port = packet.getPort();

            String peticion = new String(packet.getData(), 0, packet.getLength());
            System.out.print("["+new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime())+"]");
            System.out.println("Peticion recibida: ["+ peticion + "] desde: "+ address); 
            switch (peticion.charAt(0)) {
                case APAGAR:
                    running = false;
                    try {
                        enviarMensaje("hecho", address, port);
                        guardarWatchers();
                        System.exit(0);
                    } catch (IOException ex) {
                        System.out.print(ex);
                        break;
                    }
                    break;
                case DAME_IP:
                    try{
                        mensaje = leerInterfazRed();
                        if(mensaje == null) 
                            enviarError("01", address, port);
                        else 
                            enviarMensaje(mensaje, address, port);
                    }catch (IOException ex) {
                        System.out.print(ex);
                    }

                    break;
                case DAME_ENRUTAMIENTO:
                    try {  
                        mensaje = codificarMensageEnrutamiento(ejecutar("route -n"));
                        if(mensaje == null) 
                            enviarError("02",address, port);
                        else 
                            enviarMensaje(mensaje, address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case CAMBIO_IP:                        
                    ip = "";
                    String temp = "";
                    for (int i = 1, fase=0; i < peticion.length(); i++) {
                        switch (fase){
                            case 0:
                                if(peticion.charAt(i) != ':')ip = ip +peticion.charAt(i);
                                else fase++;
                                break;
                            case 1:
                                temp = temp + peticion.charAt(i);
                                break;
                        }
                    }
                    try {  
                        puerto = Integer.parseInt(temp);
                        socket.close();
                        if(inicializar(ip) == -1)
                            enviarError("03", address, port);
                        else
                            enviarMensaje("hecho", address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case BORRA_IP: 
                    try {
                        mensaje = ejecutar(peticion.substring(1));
                        if(mensaje == null) 
                            enviarError("04", address, port);
                        else
                            enviarMensaje(mensaje, address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case ANADE_IP: 
                    try {
                        mensaje = ejecutar(peticion.substring(1));
                        if(mensaje == null) 
                            enviarError("05", address, port);
                        else
                            enviarMensaje(mensaje, address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case INFO_PROCESO:
                    String pid = ejecutar("pgrep " + peticion.substring(1));
                    try{
                        if(pid == null){
                            enviarError("06", address, port);
                            break;
                        }
                        else if(pid.equals("")){
                            if(ejecutar("which "+peticion.substring(1)).equals("")){
                                enviarMensaje("null", address, port);
                                break;
                            }
                            else {
                                enviarMensaje("", address, port);
                                break;
                            }
                        }
                        else{
                            pid = pid.replaceAll(System.getProperty("line.separator"), ",").substring(0, pid.length()-1);
                            mensaje = ejecutar("ps p " + pid + " -f");

                            if(mensaje == null) 
                                enviarError("06", address, port);
                            else
                                enviarMensaje(mensaje, address, port);
                        }
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case MATAR_PROCESO:
                    try {  
                        mensaje = ejecutar("killall "+peticion.substring(1));
                        if(mensaje == null) 
                            enviarError("07", address, port);
                        else
                            enviarMensaje(mensaje, address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case RELANZAR:
                    try { 
                        mensaje = ejecutar(new String[]{peticion.substring(1)+" &"});
                        if(mensaje == null) 
                            enviarError("08", address, port);
                        else
                            enviarMensaje(mensaje, address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case ANADE_W:
                    boolean existe = false;
                    try {
                        for (int i = 0; i < watchers.size(); i++) {
                            if(watchers.get(i).getProceso().equals(peticion.substring(1))){
                                existe = true;
                                enviarMensaje("existe", address, port);
                                break;
                            }
                        }
                        if(existe)break;
                        String[] linea = peticion.split(System.getProperty("line.separator"));
                        if (linea.length == 1)watchers.add(new Watcher(linea[0].substring(1),null));
                        else watchers.add(new Watcher(linea[0].substring(1),linea[1]));
                        //System.out.println("Añadido:"+linea[0].substring(1));
                        if (watchers == null || watchers.isEmpty()) 
                            enviarError("09", address, port);
                        else
                            enviarMensaje("hecho", address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                   break;

                case BORRA_W:
                    boolean borrado = false;
                    try {
                        for (int i = 0; i < watchers.size(); i++) {
                            if(watchers.get(i).getProceso().equals(peticion.substring(1))){
                                watchers.remove(i);
                                borrado = true;
                                //System.out.println("Eliminado:"+peticion.substring(1));
                            }
                        }
                        if(watchers == null) 
                            enviarError("0A", address, port);
                        else if(!borrado) 
                            enviarError("1A", address, port);
                        else
                            enviarMensaje("hecho", address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;

                case ES_W:
                    try {
                        for (int i = 0; i < watchers.size(); i++) {
                            if(watchers.get(i).getProceso().equals(peticion.substring(1))){
                                enviarMensaje("true", address, port);
                            }
                        }

                        if (watchers == null) enviarError("0B", address, port);
                        else 
                            enviarMensaje("false", address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;

                case ERES_ROOT:
                    try { 
                        mensaje = ejecutar("id -u").trim();
                        if(mensaje == null)enviarError("0C", address, port);
                        else{
                            try {
                                if(Integer.parseInt(ejecutar("id -u").trim())==0)                                
                                    enviarMensaje("true", address, port);
                                else 
                                    enviarMensaje("false", address, port);
                            } catch (InterruptedException ex) {
                                enviarError("0C", address, port);
                            }
                        }
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;

                case DAME_TIMEOUT:
                    try {
                        mensaje = String.valueOf(socket.getSoTimeout());
                        if(mensaje == null) 
                            enviarError("0D", address, port);
                        else
                            enviarMensaje(mensaje, address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case CAMBIA_TIMEOUT:
                    try { 
                    socket.setSoTimeout(Integer.parseInt(peticion.substring(1)));
                        enviarMensaje("hecho", address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case DAME_W_Y_C:
                    try {
                        mensaje = "";
                        for (Watcher watcher : watchers) {
                            mensaje = mensaje.concat(watcher.getProceso()+"\n");
                            mensaje = mensaje.concat(watcher.getArgumentos()+"\n");
                        }
                        if(!mensaje.equals(""))
                            enviarMensaje(mensaje.substring(0, mensaje.length()-1), address, port);
                        else 
                            enviarMensaje("", address, port);
                        
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case DAME_PATH:
                    try {
                        mensaje = ejecutar("which " + peticion.substring(1).trim());
                        if(mensaje == null) 
                            enviarError("0G", address, port);
                        else
                            enviarMensaje(mensaje, address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                case DAME_PID_ESTADO:
                    try {
                        mensaje = getProcessPIDandState(peticion.substring(1));
                        enviarMensaje(mensaje, address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
                default:
                    try { 
                    System.out.println("Peticion rechazada.");
                        enviarError("99", address, port);
                    } catch (IOException ex) {
                        System.out.print(ex);
                    }
                    break;
            }

        }
        socket.close();
    }
    
    //añade un 00 (que significa: sin errores) y envia el mensaje. 
    private void enviarMensaje(String mensaje, InetAddress address, int port) throws IOException{  
        buffer = ("00" + mensaje).getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }

    //envia el mensjae de error que son dos nñumeros con el codigo de error corresponiente
    private void enviarError(String error, InetAddress address, int port) throws IOException{
        buffer = error.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }    
    
    private String ejecutar(String comando) throws InterruptedException{
        try {
            Process p = Runtime.getRuntime().exec(comando);
            p.waitFor(2000, TimeUnit.MILLISECONDS);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(
            p.getErrorStream()));
            String line = "";
            String output = "";
            String error= "";
            
            while ((line = stdInput.readLine()) != null) {
                output += line + "\n";
            }
            while ((line = stdError.readLine()) != null) {
                error += line + "\n";
            }
            System.out.println("Ejecutado: " + comando);
            //System.out.println("stdInput: \n" + output);
            //System.out.println("stdError: \n" + error);
            String s;
            if (!error.equals("")) {
                return null;
            }
            return output;
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error:"+ex);
        }
        return null;
    }
    
     private String ejecutar(String[] comando) throws InterruptedException{ //ejecutar con pipes
        try {
            String[] temp = {"/bin/sh","-c",comando[0]};
            Process p = Runtime.getRuntime().exec(temp);
            p.waitFor(2000, TimeUnit.MILLISECONDS);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(
            p.getErrorStream()));
            String line = "";
            String output = "";
            String error= "";
            
            while ((line = stdInput.readLine()) != null) {
                output += line + "\n";
            }
            while ((line = stdError.readLine()) != null) {
                error += line + "\n";
            }
            System.out.println("Ejecutado: " + comando[0]);
            //System.out.println("stdInput: \n" + output);
            //System.out.println("stdError: \n" + error);
            String s;
            if (!error.equals("")) {
                return null;
            }
            return output;
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error:"+ex);
        }
        return null;
    }
    
    private String leerInterfazRed() throws UnknownHostException, SocketException{
        String resultado = "";
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        if (en == null) {
            System.out.println("No hay interfaces de red");
            return null;
        } else while (en.hasMoreElements()) {
          NetworkInterface intf = en.nextElement();
          Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
          while (enumIpAddr.hasMoreElements()) {
                InetAddress ipAddr = enumIpAddr.nextElement();
                if(!intf.getName().equals("lo") && !ipAddr.getHostAddress().contains(":")){
                    resultado = resultado + intf.getName()+ "/" + ipAddr.getHostAddress()+"|";
                }
            } 
        }
        return resultado;
    }
    
    private String codificarMensageEnrutamiento(String mensage){
        String resultado = "";
        try (Scanner scanner = new Scanner(mensage)) {
            scanner.nextLine();
            scanner.nextLine();
            while (scanner.hasNextLine()){
                resultado = resultado + codificarLineaEnrut(scanner.nextLine())+"\n";
            }
        }
        catch(Exception ex) {
            return  null;
        }
        return resultado;
    }
   //separa cada información individual de la linea que corresponde a una linea de la tabla de enrutamiento
    private String codificarLineaEnrut(String linea){
        String resultado = "";
        int fase = 0;
        for (int i = 0; i < linea.length(); i++) {
            if(fase == 0){ 
                if(linea.charAt(i)!=' ')resultado = resultado + linea.charAt(i);
                        else {
                            fase = 1;
                            resultado = resultado + " ";
                        }
            }
            else if(fase == 1){
                if(linea.charAt(i)!=' '){
                    resultado = resultado + linea.charAt(i);
                    fase = 0;
                }

            }
            
        }
        return resultado;
    }
    
    public void setTimeout(int timeout){
        try {
            socket.setSoTimeout(timeout);
            System.out.println("Configurado timeut a: " + timeout +"ms");
        } catch (SocketException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void ejecutarWatchers(ArrayList<Watcher> watchers){
        if(watchers.isEmpty())return;
        /*System.out.print("WatcherClone:[");
        for (Watcher watcher : watchers) System.out.print(watcher.getProceso()+",");
        System.out.println("]");*/
        
        for (Watcher w : watchers) {
            System.out.println("______________________________");
            System.out.print("["+new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime())+"]");
            System.out.print("START[Watcher/"+w.getProceso()+"] ");
            String path = null;
            try {
                path = ejecutar("which " + w.getProceso());
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
            if(!path.equals(""))
                try {
                    String ejecutado = getProcessPIDandState(w.getProceso());
                    if(ejecutado.equals("")){
                        if(w.getArgumentos() != null)
                            ejecutar(new String[]{
                                path.replaceAll(System.getProperty("line.separator"), " ").trim() 
                                + " " + w.getArgumentos() + " &"});
                        else 
                            ejecutar(new String []{
                                path.replaceAll(System.getProperty("line.separator"), " ").trim()
                                + " &"});
                    }
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
            System.out.println("END[Watcher/"+w.getProceso()+"] ");
                
        } 

    }
    
    private String getProcessPIDandState(String process) throws InterruptedException{
        String[] s = {"ps aux | pgrep " + process + " | awk '{print $2 "+'"'+" "+'"'+" $8}'"};
        return ejecutar(s);
    }
  
    private void leerWathcersGuardados()throws FileNotFoundException, IOException{
        String cadena, args;
        FileReader f = new FileReader(WATCHERSGUARDADOS);
        BufferedReader b = new BufferedReader(f);
        while((cadena = b.readLine())!=null) {
            if((args = b.readLine()).equals(" "))args = null;
            watchers.add(new Watcher(cadena, args));
        }
        b.close();
    }
    
    private void guardarWatchers() throws IOException{
        FileWriter f = new FileWriter(WATCHERSGUARDADOS);
        for (Watcher w : watchers) {
                f.write(w.getProceso() + "\n");
                if(w.getArgumentos() == null || w.getArgumentos().equals(""))
                    f.write(" ");
                else
                    f.write(w.getArgumentos());
        }
        f.close();
    }
    
    private class Watcher{
        private String proceso;
        private String argumentos;
        
        public Watcher(String proceso, String argumentos){
            this.proceso = proceso;
            this.argumentos = argumentos;
        } 
        
        public String getProceso(){
            return this.proceso;
        }
        public String getArgumentos(){
            if(argumentos == null)return "";
            else return this.argumentos;
        }
        public void setProceso(String proceso){
            this.proceso = proceso;
        }
        public void setArgumentos(String argumentos){
            this.argumentos = argumentos;
        }
    }
}
