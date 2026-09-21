import { createApp } from './app';
import { env } from './config/env';
import { WebSocketServer, WebSocket } from 'ws';

const app = createApp();

const server = app.listen(env.port, () => {
  console.log(`Server listening on port ${env.port}`);
});

// Initialize WebSocket server on the existing HTTP server
const wss = new WebSocketServer({ server });

wss.on('connection', (androidClientWs) => {
    console.log('Android app connected for Button 2.');

    // Connect to the course-provided pixel art server
    const courseWs = new WebSocket('wss://8.229.22.124');

    // Relay messages exactly as they arrive from the course server to the Android app
    courseWs.on('message', (data) => {
        if (androidClientWs.readyState === WebSocket.OPEN) {
            androidClientWs.send(data.toString());
        }
    });

    // Clean up connections if the Android app closes the screen
    androidClientWs.on('close', () => {
        console.log('Android app disconnected.');
        courseWs.close();
    });
});

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    server.close(() => {
      process.exit(0);
    });
  });
}
