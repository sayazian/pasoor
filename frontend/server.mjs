import { createReadStream } from 'node:fs';
import { stat } from 'node:fs/promises';
import { createServer } from 'node:http';
import { extname, join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';

const port = Number(process.env.PORT ?? 4173);
const backendUrl = process.env.BACKEND_URL ?? 'http://localhost:8080';
const distDir = join(fileURLToPath(new URL('.', import.meta.url)), 'dist');
const proxyPrefixes = ['/api', '/oauth2', '/login', '/logout'];

createServer(async (request, response) => {
  try {
    const url = new URL(request.url ?? '/', `http://${request.headers.host ?? 'localhost'}`);

    if (proxyPrefixes.some((prefix) => url.pathname === prefix || url.pathname.startsWith(`${prefix}/`))) {
      await proxyRequest(request, response, url);
      return;
    }

    await serveStatic(response, url.pathname);
  } catch (error) {
    console.error(error);
    response.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
    response.end('Internal server error');
  }
}).listen(port, '0.0.0.0', () => {
  console.log(`Pasoor frontend listening on ${port}`);
});

async function proxyRequest(request, response, url) {
  const targetUrl = new URL(`${url.pathname}${url.search}`, backendUrl);
  const headers = new Headers();

  for (const [key, value] of Object.entries(request.headers)) {
    if (value === undefined || key.toLowerCase() === 'host') {
      continue;
    }
    if (Array.isArray(value)) {
      headers.set(key, value.join(', '));
    } else {
      headers.set(key, value);
    }
  }

  headers.set('x-forwarded-host', request.headers.host ?? '');
  headers.set('x-forwarded-proto', 'https');

  const proxiedResponse = await fetch(targetUrl, {
    method: request.method,
    headers,
    body: request.method === 'GET' || request.method === 'HEAD' ? undefined : request,
    duplex: 'half',
    redirect: 'manual'
  });

  response.writeHead(proxiedResponse.status, Object.fromEntries(proxiedResponse.headers.entries()));
  if (!proxiedResponse.body) {
    response.end();
    return;
  }

  for await (const chunk of proxiedResponse.body) {
    response.write(chunk);
  }
  response.end();
}

async function serveStatic(response, pathname) {
  const safePath = normalize(decodeURIComponent(pathname)).replace(/^(\.\.[/\\])+/, '');
  const requestedPath = safePath === '/' ? '/index.html' : safePath;
  let filePath = join(distDir, requestedPath);

  try {
    const fileStat = await stat(filePath);
    if (!fileStat.isFile()) {
      throw new Error('Not a file');
    }
  } catch {
    filePath = join(distDir, 'index.html');
  }

  response.writeHead(200, {
    'Content-Type': contentType(filePath),
    'Cache-Control': filePath.endsWith('index.html') ? 'no-cache' : 'public, max-age=31536000, immutable'
  });
  createReadStream(filePath).pipe(response);
}

function contentType(filePath) {
  switch (extname(filePath)) {
    case '.css':
      return 'text/css; charset=utf-8';
    case '.html':
      return 'text/html; charset=utf-8';
    case '.js':
      return 'text/javascript; charset=utf-8';
    case '.json':
      return 'application/json; charset=utf-8';
    case '.svg':
      return 'image/svg+xml';
    default:
      return 'application/octet-stream';
  }
}
