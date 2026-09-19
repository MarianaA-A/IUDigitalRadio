package com.example.iudigitalradio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.iudigitalradio.ui.theme.IUDigitalRadioTheme

val emisorasMap = mapOf(
    "IU Radio FM" to "https://icecast.vrtcdn.be/mnm-high.mp3",
    "Radio Nacional" to "https://shoutcast.radionacional.co/radionacional",
    "La Mega Antioquia" to "https://19293.live.streamtheworld.com/LAMEGAMED_SC",
    "Tropicana" to "https://19993.live.streamtheworld.com/TROPICANA_MED_SC",
    "Bésame" to "https://21253.live.streamtheworld.com/BESAME_MED_SC",
    "Emisora Estudiantil" to "https://icecast.omroep.nl/3fm-bb-mp3"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IUDigitalRadioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RadioAppScreen()
                }
            }
        }
    }
}

@Composable
fun RadioAppScreen() {
    var selectedStation by rememberSaveable { mutableStateOf("IU Radio FM") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PerfilSection()
        Spacer(modifier = Modifier.height(24.dp))

        ReproductorSection(
            emisoraActual = selectedStation,
            urlAudio = emisorasMap[selectedStation] ?: ""
        )

        Spacer(modifier = Modifier.height(24.dp))
        ListaEmisorasSection(
            emisoraActual = selectedStation,
            onStationSelected = { nuevaEmisora -> selectedStation = nuevaEmisora }
        )
    }
}

@Composable
fun PerfilSection() {
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        imageBitmap = bitmap
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) cameraLauncher.launch(null)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "Perfil de Usuario", style = MaterialTheme.typography.titleLarge)
            Text(text = "Mariana Agudelo", style = MaterialTheme.typography.bodyMedium)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!.asImageBitmap(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sin foto", color = Color.DarkGray)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (hasCameraPermission) {
                    cameraLauncher.launch(null)
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text("Cámara")
            }
        }
    }
}

@Composable
fun ReproductorSection(emisoraActual: String, urlAudio: String) {
    val context = LocalContext.current

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isMuted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(urlAudio) {
        if (urlAudio.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(urlAudio)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (isPlaying) {
                exoPlayer.play()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    fun vibrarCorto() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Reproduciendo ahora", style = MaterialTheme.typography.labelLarge)
            Text(
                text = emisoraActual,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    vibrarCorto()
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                    isPlaying = !isPlaying
                }) {
                    Text(if (isPlaying) "Pause" else "Play")
                }
                Button(onClick = {
                    vibrarCorto()
                    isMuted = !isMuted
                }) {
                    Text(if (isMuted) "Unmute" else "Mute")
                }
            }
        }
    }
}

@Composable
fun ListaEmisorasSection(emisoraActual: String, onStationSelected: (String) -> Unit) {
    val emisoras = emisorasMap.keys.toList()

    Text(text = "Estaciones Disponibles", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(emisoras) { emisora ->
            val isSelected = emisora == emisoraActual
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onStationSelected(emisora) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = emisora,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}