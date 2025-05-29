package com.example.sensordhtt22

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

class MainActivity : ComponentActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var ledRef: DatabaseReference
    private lateinit var tempRef: DatabaseReference
    private lateinit var humRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        try {
            database =
                FirebaseDatabase.getInstance("https://esp32sensorproject-e83c0-default-rtdb.firebaseio.com/")
//            database.setPersistenceEnabled(true)
            ledRef = database.getReference("Led/status")
            tempRef = database.getReference("Sensor/temperature")
            humRef = database.getReference("Sensor/humidity")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            SensorApp()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SensorApp() {
        var temperature by remember { mutableStateOf("--") }
        var humidity by remember { mutableStateOf("--") }
        var ledStatus by remember { mutableStateOf(false) }
        var connectionStatus by remember { mutableStateOf("Connecting...") }

        val gradientBackground = Brush.verticalGradient(
            colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        )

        LaunchedEffect(true) {
            tempRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue()
                    temperature = when (value) {
                        is Double -> "%.1f".format(value)
                        is Float -> "%.1f".format(value)
                        is Long -> value.toString()
                        is String -> value
                        else -> "--"
                    }
                    connectionStatus = "Connected"
                }

                override fun onCancelled(error: DatabaseError) {
                    temperature = "Error"
                    connectionStatus = "Error: ${error.message}"
                }
            })

            humRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue()
                    humidity = when (value) {
                        is Double -> "%.1f".format(value)
                        is Float -> "%.1f".format(value)
                        is Long -> value.toString()
                        is String -> value
                        else -> "--"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    humidity = "Error"
                }
            })

            ledRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ledStatus = snapshot.getValue(Boolean::class.java) ?: false
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚙️ IoT CONTROL PANEL",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00FFC6)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Status: $connectionStatus",
                    color = if (connectionStatus == "Connected") Color(0xFF34D399) else Color.Red,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SensorDisplayCard(
                        label = "🌡 Temp",
                        value = "$temperature°C",
                        background = Color(0xFFEF4444)
                    )
                    SensorDisplayCard(
                        label = "💧 Humidity",
                        value = "$humidity%",
                        background = Color(0xFF06B6D4)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                LedToggleCard(ledStatus) {
                    ledRef.setValue(!ledStatus)
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    fun SensorDisplayCard(label: String, value: String, background: Color) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(160.dp)
                .height(140.dp)
                .shadow(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, fontSize = 18.sp, color = Color.White)
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = background
                )
            }
        }
    }

    @Composable
    fun LedToggleCard(status: Boolean, onToggle: () -> Unit) {
        val ledColor by animateColorAsState(
            targetValue = if (status) Color.Yellow else Color.Gray,
            animationSpec = tween(300),
            label = "ledColor"
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .shadow(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.05f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "LED Status",
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (status) "ON" else "OFF",
                        color = ledColor,
                        fontSize = 16.sp
                    )
                }

                Switch(
                    checked = status,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Yellow,
                        checkedTrackColor = Color.Yellow.copy(alpha = 0.4f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}
