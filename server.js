const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.DEFAULT_APP_PORT || 3000;
const ROOT_DIR = __dirname;

const server = http.createServer((req, res) => {
  const reqUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = decodeURIComponent(reqUrl.pathname);

  // Health check endpoint
  if (pathname === '/health' || pathname === '/_health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', time: new Date().toISOString() }));
    return;
  }

  // APK download endpoints
  if (pathname === '/KothaPad.apk' || pathname === '/app-debug.apk' || pathname === '/download') {
    const candidates = [
      path.join(ROOT_DIR, 'KothaPad.apk'),
      path.join(ROOT_DIR, 'app-debug.apk'),
      path.join(ROOT_DIR, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
    ];

    let apkPath = candidates.find(p => fs.existsSync(p));

    if (apkPath) {
      const stat = fs.statSync(apkPath);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="KothaPad.apk"',
        'Cache-Control': 'no-cache'
      });
      fs.createReadStream(apkPath).pipe(res);
      return;
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('APK not found. Please compile the applet first.');
      return;
    }
  }

  // Root landing page
  if (pathname === '/' || pathname === '/index.html') {
    const apkExists = fs.existsSync(path.join(ROOT_DIR, 'KothaPad.apk')) ||
                      fs.existsSync(path.join(ROOT_DIR, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk'));

    const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>KothaPad (km10) - Android App</title>
  <style>
    :root {
      --primary: #FF7043;
      --primary-hover: #F4511E;
      --bg: #121212;
      --card-bg: #1E1E1E;
      --card-border: #2A2A2A;
      --text: #F5F5F5;
      --text-muted: #9E9E9E;
      --accent: #FFB74D;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      background-color: var(--bg);
      color: var(--text);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }
    .container {
      max-width: 620px;
      width: 100%;
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 20px;
      padding: 36px 28px;
      box-shadow: 0 12px 40px rgba(0,0,0,0.5);
      text-align: center;
    }
    .badge {
      display: inline-block;
      padding: 6px 14px;
      background: rgba(255, 112, 67, 0.15);
      color: var(--primary);
      font-size: 13px;
      font-weight: 600;
      border-radius: 999px;
      margin-bottom: 16px;
      letter-spacing: 0.5px;
    }
    h1 {
      font-size: 28px;
      font-weight: 700;
      margin-bottom: 8px;
      color: #FFFFFF;
    }
    p.subtitle {
      font-size: 15px;
      color: var(--text-muted);
      line-height: 1.5;
      margin-bottom: 28px;
    }
    .status-card {
      background: #181818;
      border: 1px solid #262626;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      font-size: 14px;
      color: #A5D6A7;
    }
    .status-indicator {
      width: 10px;
      height: 10px;
      background: #4CAF50;
      border-radius: 50%;
      box-shadow: 0 0 10px #4CAF50;
    }
    .download-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      width: 100%;
      background: var(--primary);
      color: #FFFFFF;
      font-size: 17px;
      font-weight: 600;
      padding: 16px 24px;
      border-radius: 12px;
      text-decoration: none;
      transition: background 0.2s, transform 0.1s;
      cursor: pointer;
      box-shadow: 0 4px 20px rgba(255, 112, 67, 0.4);
    }
    .download-btn:hover {
      background: var(--primary-hover);
      transform: translateY(-1px);
    }
    .download-btn:active {
      transform: translateY(1px);
    }
    .features-list {
      margin-top: 32px;
      text-align: left;
      border-top: 1px solid var(--card-border);
      padding-top: 24px;
    }
    .features-list h3 {
      font-size: 15px;
      font-weight: 600;
      color: var(--accent);
      margin-bottom: 12px;
      text-transform: uppercase;
      letter-spacing: 0.8px;
    }
    .features-list ul {
      list-style: none;
    }
    .features-list li {
      font-size: 14px;
      color: #CCCCCC;
      padding: 8px 0;
      display: flex;
      align-items: flex-start;
      gap: 10px;
      line-height: 1.4;
    }
    .features-list li::before {
      content: "✔";
      color: var(--primary);
      font-weight: bold;
    }
    .instructions {
      margin-top: 20px;
      font-size: 12px;
      color: var(--text-muted);
      line-height: 1.6;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="badge">NATIVE ANDROID APP</div>
    <h1>KothaPad (km10)</h1>
    <p class="subtitle">Speech-enabled power notepad with continuous dictation, 3x10 in-app keyboard, text stamps, and local-first storage.</p>

    <div class="status-card">
      <div class="status-indicator"></div>
      <span>${apkExists ? 'Compiled APK Ready for Installation' : 'Compiling Android Build...'}</span>
    </div>

    <a href="/KothaPad.apk" class="download-btn" download="KothaPad.apk">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
        <polyline points="7 10 12 15 17 10"></polyline>
        <line x1="12" y1="15" x2="12" y2="3"></line>
      </svg>
      Download KothaPad.apk
    </a>

    <div class="features-list">
      <h3>Key App Capabilities</h3>
      <ul>
        <li><strong>Seamless Dual-Keyboard System:</strong> Smoothly toggle between the custom 3x10 grid keyboard and the device's system keyboard (Gboard, Samsung, etc.) without flickering.</li>
        <li><strong>Continuous Voice Dictation:</strong> Persistent speech recognition with noise filtering and audio level visualization.</li>
        <li><strong>One-Tap Smart Stamps:</strong> Automated templates for dates, times, and structured notes.</li>
        <li><strong>Private & Local-First:</strong> Room Database persistence with full offline capabilities.</li>
      </ul>
    </div>

    <div class="instructions">
      <p><strong>Device Installation:</strong> Download <code>KothaPad.apk</code> directly on your Android phone, tap to install, and grant microphone permissions when prompted.</p>
    </div>
  </div>
</body>
</html>`;

    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(html);
    return;
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Dev server listening on http://0.0.0.0:${PORT}`);
});
