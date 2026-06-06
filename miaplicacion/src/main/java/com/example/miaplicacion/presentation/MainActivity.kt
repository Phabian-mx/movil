package com.example.miaplicacion.presentation

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.miaplicacion.R
import android.content.Intent
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

// 🛠️ CORREGIDO: Se agregó ', MessageClient.OnMessageReceivedListener' al final de la cabecera
class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val boton: Button = findViewById(R.id.boton)
        val context = this

        boton.setOnClickListener {
            // Mandamos los datos usando la ruta "/chat_ruta" para que el celular lo reciba directo en su chat
            enviarMensajeAlCelular("75")

            Toast.makeText(context, "Enviando pulsaciones...", Toast.LENGTH_SHORT).show()

            // Salto a tu pantalla con música y sensor real
            val intent = Intent(this@MainActivity, Prueba::class.java)
            startActivity(intent)
        }
    }

    // =========================================================
    // 📤 ENVIAR MENSAJE AL CELULAR
    // =========================================================
    private fun enviarMensajeAlCelular(mensaje: String) {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Wearable.getMessageClient(this)
                    // Usamos la ruta universal del chat
                    .sendMessage(node.id, "/chat_ruta", mensaje.toByteArray())
            }
        }
    }

    // =========================================================
    // 📩 ESCUCHADOR DE MENSAJES (AHORA SÍ FUNCIONA)
    // =========================================================
    override fun onMessageReceived(messageEvent: MessageEvent) {
        // Escucha todo lo que el celular le mande por el chat
        if (messageEvent.path == "/chat_ruta" || messageEvent.path == "/ritmo_cardiaco") {
            val mensajeDelCelular = String(messageEvent.data)

            runOnUiThread {
                try {
                    // Cambia el texto "Esperando celular..." por el mensaje real
                    val txtConsolaReloj = findViewById<TextView>(R.id.text_reloj)
                    txtConsolaReloj?.text = mensajeDelCelular
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Tu alerta Toast en pantalla
                Toast.makeText(this, "Celular dice: $mensajeDelCelular", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Activa la escucha Bluetooth
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        // Pausa la escucha al cerrar la ventana para no agotar la batería
        Wearable.getMessageClient(this).removeListener(this)
    }
}