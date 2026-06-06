package com.example.miaplicacion.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.example.miaplicacion.R
// --- NUEVAS IMPORTACIONES PARA LA CONEXIÓN ---
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*

class Prueba : ComponentActivity(),
    SensorEventListener,
    CoroutineScope by MainScope() { // 1. AGREGADO: Habilita segundo plano para buscar el celular

    private var mediaPlayer: MediaPlayer? = null

    // Variables del sensor
    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private var sensorType = Sensor.TYPE_HEART_RATE

    // Variable para el componente de texto en pantalla
    private lateinit var textoRitmoCardiaco: TextView

    // 2. AGREGADO: Variables para identificar el celular conectado por Bluetooth
    private var phoneNodeID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(sensorType)

        setContentView(R.layout.prueba)

        // Vincular la variable con el ID del XML
        textoRitmoCardiaco = findViewById(R.id.texto_ritmo_cardiaco)

        // 3. AGREGADO: Al abrir la ventana, el reloj busca el ID del celular
        obtenerNodoTelefono()

        startSensor()

        // Control del reproductor de audio (Tu código original)
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.piano)
            mediaPlayer?.start()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar audio", Toast.LENGTH_SHORT).show()
        }

        val botonPlayPause: Button = findViewById(R.id.boton_play_pause)
        val botonRegresar: Button = findViewById(R.id.boton_regresar)

        botonPlayPause.setOnClickListener {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    botonPlayPause.text = "Continuar"
                } else {
                    player.start()
                    botonPlayPause.text = "Pausar"
                }
            }
        }

        botonRegresar.setOnClickListener {
            finish()
        }
    }

    private fun startSensor() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 1001)
            return
        }
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // =========================================================
    // MODIFICADO: AHORA PINTA EN RELOJ Y ENVÍA AL CELULAR
    // =========================================================
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == sensorType) {
            val lectura = event.values[0]

            // Muestra en la consola del sistema
            Log.d("onSensorChanged", "Lectura: ${lectura}")

            // Pintamos el valor en el reloj
            textoRitmoCardiaco.text = "${lectura.toInt()} BPM"

            // 4. AGREGADO: Mandamos las pulsaciones directo al celular en tiempo real
            enviarDatosAlCelular(lectura.toInt().toString())
        }
    }

    // =========================================================
    // NUEVA FUNCIÓN: ENVÍA MENSAJES DE RED AL MÓDULO MÓVIL
    // =========================================================
    private fun enviarDatosAlCelular(bpm: String) {
        phoneNodeID?.let { nodoId ->
            launch(Dispatchers.IO) {
                try {
                    // Enviamos los BPM a través de la ruta "/ritmo_cardiaco"
                    Wearable.getMessageClient(this@Prueba)
                        .sendMessage(nodoId, "/ritmo_cardiaco", bpm.toByteArray())
                    Log.d("Wearable", "¡Enviado con éxito al celular: $bpm!")
                } catch (e: Exception) {
                    Log.e("Wearable", "Error de envío", e)
                }
            }
        }
    }

    // =========================================================
    // NUEVA FUNCIÓN: RASTREA SI EL TELÉFONO ESTÁ CONECTADO (Diapositiva 2)
    // =========================================================
    private fun obtenerNodoTelefono() {
        launch(Dispatchers.Default) {
            val nodeList = Wearable.getNodeClient(this@Prueba).connectedNodes
            try {
                val nodes = Tasks.await(nodeList)
                for (node in nodes) {
                    phoneNodeID = node.id // Almacena el ID del celular
                    Log.d("NODO", "Celular enlazado detectado con ID: $phoneNodeID")
                }
            } catch (exception: Exception) {
                Log.d("Error en el nodo", exception.toString())
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Requerido por la interfaz
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        sensor?.also { heartRate ->
            sensorManager.registerListener(this, heartRate, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        cancel() // 5. AGREGADO: Cierra los procesos de comunicación al salir
    }
}