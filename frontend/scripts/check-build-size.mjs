import { readdir, stat } from 'node:fs/promises';
import { join } from 'node:path';

const assetsDir = join(process.cwd(), 'dist', 'assets');
const maxJavaScriptBytes = 250 * 1024;
const maxCssBytes = 16 * 1024;

const files = await readdir(assetsDir);
const jsFiles = files.filter((file) => file.endsWith('.js'));
const cssFiles = files.filter((file) => file.endsWith('.css'));

const jsBytes = await totalBytes(jsFiles);
const cssBytes = await totalBytes(cssFiles);

if (jsBytes > maxJavaScriptBytes) {
  throw new Error(`JavaScript bundle is ${jsBytes} bytes, above ${maxJavaScriptBytes} bytes.`);
}

if (cssBytes > maxCssBytes) {
  throw new Error(`CSS bundle is ${cssBytes} bytes, above ${maxCssBytes} bytes.`);
}

console.log(`Build size OK: ${jsBytes} bytes JS, ${cssBytes} bytes CSS.`);

async function totalBytes(filesToMeasure) {
  const sizes = await Promise.all(filesToMeasure.map(async (file) => (await stat(join(assetsDir, file))).size));
  return sizes.reduce((total, size) => total + size, 0);
}
