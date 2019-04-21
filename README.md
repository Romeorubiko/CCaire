# CCaire
Esto es un software desarrollado para la configuración del dron UAV del INTA. Ha sido desarrollado como un trabajo de la UC3M.

Las versiones se pueden encontrar en:<br> 
https://github.com/Romeorubiko/CCTierra/releases

Se compone de dos proyectos:<br>
El cliente  -> https://github.com/Romeorubiko/CCTierra<br>
El servidor -> https://github.com/Romeorubiko/CCAire
<details open>
  <summary>Instrucciones de instalación</summary>
  <br>
1.	Instalar Java 8 o superior en ambas máquinas. Se recomienda utilizar openJDK8 que se puede instalar fácilmente ejecutando el siguiente comando en la terminal mientras se tiene una conexión a internet:<br>
  <b>sudo apt-get install openjdk-8-jre</b><br>
2.	Crear una carpeta en el Router de tierra y copiar el archivo CC_tierra.jar dentro.<br>
3.	Crear una carpeta en la Máquina UAV y copiar el archivo CC_aire.jar dentro.<br>
<br>
</details>
<details open>
<summary>Instrucciones de ejecución</summary>
<br>
Para el cliente:<br>
<b>Java –jar "path de CC_tierra.jar"</b><br>
	Para el servidor:<br>
<b>sudo Java –jar "path de CC_aire.jar" -i "IP" -p "puerto" -t "timeout"</b><br>
	*los flags de puerto y timeout no son obligatorios.<br>
	*si se utiliza una sesión ssh para ejecutar el servidor hay que tener en cuenta que una vez cerrada la sesión ssh el servidor debe seguir corriendo. Por lo que se debe utilizar el comando screen (o similares) al final del comando de ejecución del servidor para mantenerlo corriendo una vez que la sesión ssh se cierre.<br>
</details>
