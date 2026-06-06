package com.example.movil

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var textoChat: TextView
    private lateinit var inputMensaje: EditText
    private lateinit var btnEnviar: Button

    // La ruta oficial para el texto del chat
    private val CHAT_PATH = "/chat_ruta"

    // La ruta que usa tu archivo Prueba.kt en el reloj para los latidos
    private val RITMO_PATH = "/ritmo_cardiaco"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Conectamos las variables con el diseño XML
        textoChat = findViewById(R.id.texto_chat)
        inputMensaje = findViewById(R.id.input_mensaje)
        btnEnviar = findViewById(R.id.btn_enviar)

        // Acción al presionar el botón Enviar
        btnEnviar.setOnClickListener {
            val texto = inputMensaje.text.toString()
            if (texto.isNotEmpty()) {
                enviarMensajeAlReloj(texto)
                // Mostramos nuestro propio mensaje en la pantalla del celular
                textoChat.append("Celular: $texto\n")
                inputMensaje.text.clear() // Limpiamos la caja de texto
            }
        }
    }

    // =========================================================
    // 📤 ENVIAR MENSAJE DE TEXTO AL RELOJ
    // =========================================================
    private fun enviarMensajeAlReloj(mensaje: String) {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                Toast.makeText(this, "No hay reloj conectado", Toast.LENGTH_SHORT).show()
            }
            for (node in nodes) {
                Wearable.getMessageClient(this)
                    .sendMessage(node.id, CHAT_PATH, mensaje.toByteArray())
            }
        }
    }

    // =========================================================
    // 📩 RECIBIR DATOS DEL RELOJ (MENSAJES Y RITMO CARDÍACO)
    // =========================================================
    override fun onMessageReceived(messageEvent: MessageEvent) {
        // Caso 1: El reloj nos manda un mensaje de texto desde su pantalla de chat
        if (messageEvent.path == CHAT_PATH) {
            val mensajeRecibido = String(messageEvent.data)
            runOnUiThread {
                textoChat.append("Reloj: $mensajeRecibido\n")
            }
        }
        // Caso 2: El reloj nos manda las pulsaciones del sensor desde Prueba.kt
        else if (messageEvent.path == RITMO_PATH) {
            val bpmRecibidos = String(messageEvent.data)
            runOnUiThread {
                // Se imprimen los latidos de forma automática en la misma consola de chat
                textoChat.append("💓 Sensor: $bpmRecibidos BPM\n")
            }
        }
    }

    // =========================================================
    // 🔄 CICLO DE VIDA: ACTIVAR Y DESACTIVAR EL ESCUCHADOR
    // =========================================================
    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }
}