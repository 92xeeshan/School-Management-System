import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(import.meta.url));
const url = String(process.env.NG_APP_BACKEND_URL ?? '').trim();
const file = join(root, '../src/environments/backend-url.ts');

writeFileSync(file, `export const NG_APP_BACKEND_URL = ${JSON.stringify(url)};\n`);
