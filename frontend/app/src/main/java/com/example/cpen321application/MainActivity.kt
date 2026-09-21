package com.example.cpen321application

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainNavigation(
                        apiBaseUrl = BuildConfig.API_BASE_URL,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainNavigation(apiBaseUrl: String, modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf("Menu") }

    BackHandler(enabled = currentScreen != "Menu") { currentScreen = "Menu" }

    when (currentScreen) {
        "Menu" -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { currentScreen = "Button1" }, modifier = Modifier.padding(8.dp).size(200.dp, 50.dp)) { Text("Login + Server") }
                Button(onClick = { currentScreen = "Button2" }, modifier = Modifier.padding(8.dp).size(200.dp, 50.dp)) { Text("Live Updates") }
                Button(onClick = { currentScreen = "Button3" }, modifier = Modifier.padding(8.dp).size(200.dp, 50.dp)) { Text("Timer") }
            }
        }
        "Button1" -> Button1Screen(apiBaseUrl, modifier)
        "Button2" -> Button2Screen(apiBaseUrl, modifier)
        "Button3" -> Button3Screen(modifier)
    }
}
// ----------------- BUTTON 3: TIMER & SURPRISE ----------------- //
@Composable
fun Button3Screen(modifier: Modifier = Modifier) {
    var minutesInput by remember { mutableStateOf("") }
    var secondsInput by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showSurprise by remember { mutableStateOf(false) }
    var surpriseJoke by remember { mutableStateOf("") }

    // The Background Timer and API Call
    androidx.compose.runtime.LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (timeRemaining > 0) {
                kotlinx.coroutines.delay(1000L)
                timeRemaining--
            }

            showSurprise = true
            surpriseJoke = "Fetching joke..."

            // Hit the public JokeAPI in the background
            val jokeResult = fetchJson("https://v2.jokeapi.dev/joke/Programming?type=single", "joke")

            // Display the live joke, with a quick fallback just in case the network drops
            surpriseJoke = if (jokeResult.startsWith("Error") || jokeResult == "Not found") {
                "Why do programmers prefer dark mode? Because light attracts bugs."
            } else {
                jokeResult
            }

            // Turn off the timer LAST, so the coroutine doesn't cancel itself early
            isTimerRunning = false
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showSurprise) {
            Text(
                text = surpriseJoke,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(onClick = { showSurprise = false }) {
                Text("Reset")
            }
        } else {
            Text("Set Timer", modifier = Modifier.padding(bottom = 16.dp))

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { minutesInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = secondsInput,
                    onValueChange = { secondsInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Sec") },
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }

            Text(
                text = "Time Left: ${timeRemaining / 60}m ${timeRemaining % 60}s",
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Button(
                onClick = {
                    val m = minutesInput.toIntOrNull() ?: 0
                    val s = secondsInput.toIntOrNull() ?: 0
                    timeRemaining = (m * 60) + s
                    if (timeRemaining > 0) {
                        isTimerRunning = true
                        showSurprise = false
                    }
                },
                enabled = !isTimerRunning
            ) {
                Text(if (isTimerRunning) "Running..." else "Start Timer")
            }
        }
    }
}
// ----------------- BUTTON 2: LIVE PIXEL ART ----------------- //
@Composable
fun Button2Screen(apiBaseUrl: String, modifier: Modifier = Modifier) {
    val gridSize = 16
    val gridColors = remember {
        mutableStateListOf<Color>().apply {
            for (i in 0 until gridSize * gridSize) add(Color.LightGray)
        }
    }
    var connectionStatus by remember { mutableStateOf("Connecting to Node.js backend...") }

    // 1. We need this scope to safely push network data to the UI thread
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val client = OkHttpClient()
        val wsUrl = apiBaseUrl.replace("http://", "ws://").replace("https://", "wss://")
        val request = Request.Builder().url(wsUrl).build()

        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                coroutineScope.launch {
                    connectionStatus = "Connected! Waiting for pixel data..."
                }
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                // 2. Wrap the UI update inside the coroutine scope
                coroutineScope.launch {
                    try {
                        val json = JSONObject(text)
                        val x = json.getInt("x")
                        val y = json.getInt("y")
                        val hexColor = json.getString("color")

                        val parsedColor = android.graphics.Color.parseColor(hexColor)
                        val index = y * gridSize + x

                        if (index in gridColors.indices) {
                            gridColors[index] = Color(parsedColor)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                coroutineScope.launch { connectionStatus = "Disconnected" }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                coroutineScope.launch { connectionStatus = "Connection Failed: ${t.message}" }
            }
        })

        onDispose {
            webSocket.close(1000, "User left screen")
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = connectionStatus, modifier = Modifier.padding(bottom = 16.dp))

        Column(modifier = Modifier.border(2.dp, Color.Black)) {
            for (y in 0 until gridSize) {
                Row {
                    for (x in 0 until gridSize) {
                        val index = y * gridSize + x
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(gridColors[index])
                        )
                    }
                }
            }
        }
    }
}
// ----------------- BUTTON 1: LOGIN + SERVER ----------------- //
@Composable
fun Button1Screen(apiBaseUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoggedIn by remember { mutableStateOf(false) }
    var authName by remember { mutableStateOf("Logging in...") }
    var serverIp by remember { mutableStateOf("Loading...") }
    var clientIp by remember { mutableStateOf("Loading...") }
    var serverTime by remember { mutableStateOf("Loading...") }
    var clientTime by remember { mutableStateOf("Loading...") }
    var backendName by remember { mutableStateOf("Loading...") }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestProfile().build()
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                authName = "${account?.givenName} ${account?.familyName}"
                isLoggedIn = true

                coroutineScope.launch {
                    clientTime = SimpleDateFormat("HH:mm:ss 'GMT'XXX", Locale.getDefault()).format(Date())
                    clientIp = fetchJson("https://api.ipify.org?format=json", "ip")
                    serverIp = fetchJson("$apiBaseUrl/api/ip", "serverIp")
                    serverTime = fetchJson("$apiBaseUrl/api/time", "serverLocalTime")
                    val fName = fetchJson("$apiBaseUrl/api/name", "firstName")
                    val lName = fetchJson("$apiBaseUrl/api/name", "lastName")
                    backendName = "$fName $lName"
                }
            } catch (e: ApiException) {
                authName = "Sign in failed"
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isLoggedIn) {
            Button(onClick = { launcher.launch(googleSignInClient.signInIntent) }) { Text("Log in with Google") }
        } else {
            Text("Server IP: $serverIp", modifier = Modifier.padding(bottom = 8.dp))
            Text("Client IP: $clientIp", modifier = Modifier.padding(bottom = 8.dp))
            Text("Server local time: $serverTime", modifier = Modifier.padding(bottom = 8.dp))
            Text("Client local time: $clientTime", modifier = Modifier.padding(bottom = 8.dp))
            Text("Backend Name: $backendName", modifier = Modifier.padding(bottom = 8.dp))
            Text("Logged in as: $authName", modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

suspend fun fetchJson(urlString: String, key: String): String = withContext(Dispatchers.IO) {
    try {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().readText()
            JSONObject(response).optString(key, "Not found")
        } else {
            "HTTP ${connection.responseCode}"
        }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}