import express, { type Express } from 'express';

export function createApp(): Express {
  const app = express();

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.get('/api/ip', (req: Request, res: Response) => {
    res.json({
        serverIp: req.socket.localAddress
    });
  });

  app.get('/api/time', (req: Request, res: Response) => {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');

    const offset = -now.getTimezoneOffset();
    const sign = offset >= 0 ? '+' : '-';
    const offsetHours = String(Math.floor(Math.abs(offset) / 60)).padStart(2, '0');
    const offsetMinutes = String(Math.abs(offset) % 60).padStart(2, '0');

    const formattedTime = `${hours}:${minutes}:${seconds} GMT${sign}${offsetHours}:${offsetMinutes}`;

    res.json({
        serverLocalTime: formattedTime
    });
  });


  app.get('/api/name', (req: Request, res: Response) => {
    res.json({
        firstName: "Brennan",
        lastName: "Ow Yong"
    });
  });
  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
