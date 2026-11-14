const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

const W = 16, H = 16;
const img = new Uint8Array(W * H * 4);

function set(x, y, r, g, b, a = 255) {
  if (x < 0 || x >= W || y < 0 || y >= H) return;
  const i = (y * W + x) * 4;
  img[i] = r; img[i + 1] = g; img[i + 2] = b; img[i + 3] = a;
}

// Handle (gold/bronze)
for (let y = 9; y < H; y++) {
  for (let x = 6; x <= 9; x++) set(x, y, 150, 110, 48);
}
for (let y = 13; y < H; y++) for (let x = 6; x <= 9; x++) set(x, y, 120, 88, 36);
for (let y = 9; y < 13; y++) set(8, y, 196, 158, 82);

// Guard ring
for (let y = 7; y <= 9; y++) {
  for (let x = 5; x <= 10; x++) {
    const dx = x - 7.5, dy = y - 8;
    if (Math.sqrt(dx * dx + dy * dy) > 3.1) set(x, y, 214, 176, 88);
  }
}

// Red spiked head
const cx = 7.5, cy = 5.5;
const R = 4.6;
for (let y = 0; y < H; y++) {
  for (let x = 0; x < W; x++) {
    const dx = x - cx, dy = y - cy;
    const d = Math.sqrt(dx * dx + dy * dy);
    if (d < R) {
      const t = d / R;
      const r = Math.round(226 - t * 90 + (1 - t) * 30);
      const g = Math.round(34 - t * 14);
      const b = Math.round(34 - t * 14);
      const a = Math.round(255 - t * 40);
      set(x, y, r, g, b, a);
    }
  }
}

// Spikes
const spikes = [
  [7.5, 0.6, 0, -1], [7.5, 10.4, 0, 1],
  [0.6, 5.5, -1, 0], [14.4, 5.5, 1, 0],
  [2.6, 1.8, -0.7, -0.7], [12.4, 1.8, 0.7, -0.7],
  [2.6, 9.2, -0.7, 0.7], [12.4, 9.2, 0.7, 0.7],
];
for (const [sx, sy, dx, dy] of spikes) {
  for (let t = 0; t <= 3; t++) {
    const px = sx + dx * t, py = sy + dy * t;
    set(Math.round(px), Math.round(py), 166, 18, 18, 255);
  }
}

function crc32(buf) {
  let c = ~0;
  for (let i = 0; i < buf.length; i++) {
    c ^= buf[i];
    for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xEDB88320 & -(c & 1));
  }
  return (~c) >>> 0;
}
function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const typeBuf = Buffer.from(type, 'ascii');
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
  return Buffer.concat([len, typeBuf, data, crc]);
}

const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
const ihdr = Buffer.alloc(13);
ihdr.writeUInt32BE(W, 0);
ihdr.writeUInt32BE(H, 4);
ihdr[8] = 8;
ihdr[9] = 6;
ihdr[10] = 0;
ihdr[11] = 0;
ihdr[12] = 0;

const raw = [];
for (let y = 0; y < H; y++) {
  raw.push(0);
  for (let x = 0; x < W; x++) {
    const i = (y * W + x) * 4;
    raw.push(img[i], img[i + 1], img[i + 2], img[i + 3]);
  }
}
const idat = zlib.deflateSync(Buffer.from(raw));

const out = Buffer.concat([
  sig,
  chunk('IHDR', ihdr),
  chunk('IDAT', idat),
  chunk('IEND', Buffer.alloc(0)),
]);

const outDir = path.join(__dirname, 'src', 'main', 'resources', 'assets', 'minecraft', 'textures', 'item');
fs.mkdirSync(outDir, { recursive: true });
fs.writeFileSync(path.join(outDir, 'plutus_mace.png'), out);
console.log('Wrote plutus_mace.png (' + W + 'x' + H + ', ' + out.length + ' bytes)');
