#!/usr/bin/env node
// Uploads an Android App Bundle to Google Play through the Google Play Developer API.
// No dependencies, Node 18+.
//
// Credentials: the JSON key of a Google Cloud service account, that has access to the app in
// Play Console (Users and permissions), in a file at PLAY_SERVICE_ACCOUNT_JSON_PATH or as raw
// JSON / base64 in PLAY_SERVICE_ACCOUNT_JSON.
//
//   node scripts/play-publish.mjs --validate
//       Checks the key and the app access, prints the tracks. Uploads nothing.
//   node scripts/play-publish.mjs --aab <file.aab> [--track internal] [--status completed]
//                                 [--notes "text"] [--lang en-US] [--package <id>]
//       Uploads the bundle and releases it on the track. --status draft is required while the app
//       is still a draft in Play Console. --track production releases to everyone.
//   node scripts/play-publish.mjs --listing [--lang en-US]
//       Prints the store listings (all languages, or the given one). Changes nothing.
//   node scripts/play-publish.mjs --set-listing --lang en-US [--title "..."] [--short "..."]
//                                 [--full-file description.txt] [--icon icon.png]
//                                 [--feature-graphic feature.png]
//       Updates the store listing texts and replaces the icon / feature graphic.

import { createSign } from 'node:crypto';
import { readFileSync, statSync } from 'node:fs';

const API = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications';
const UPLOAD_API = 'https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications';

function args() {
  const a = { track: 'internal', status: 'completed', lang: 'en-US', package: 'me.aap.fermata.mczernek.auto' };
  const v = process.argv.slice(2);
  for (let i = 0; i < v.length; i++) {
    const k = v[i];
    if (k === '--validate') a.validate = true;
    else if (k === '--listing') a.listing = true;
    else if (k === '--set-listing') a.setListing = true;
    else if (k.startsWith('--')) a[k.slice(2)] = v[++i];
    else throw new Error(`Unexpected argument: ${k}`);
  }
  if (!a.validate && !a.listing && !a.setListing && !a.aab) {
    throw new Error('One of --validate, --listing, --set-listing or --aab <file> is required');
  }
  if (a.setListing && !process.argv.includes('--lang')) throw new Error('--set-listing requires --lang');
  return a;
}

function serviceAccount() {
  const path = process.env.PLAY_SERVICE_ACCOUNT_JSON_PATH;
  let data = path ? readFileSync(path, 'utf8') : process.env.PLAY_SERVICE_ACCOUNT_JSON;
  if (!data) throw new Error('Set PLAY_SERVICE_ACCOUNT_JSON_PATH or PLAY_SERVICE_ACCOUNT_JSON');
  data = data.trim();
  if (!data.startsWith('{')) data = Buffer.from(data, 'base64').toString('utf8');
  return JSON.parse(data);
}

async function accessToken(sa) {
  const b64 = o => Buffer.from(JSON.stringify(o)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  const tokenUri = sa.token_uri || 'https://oauth2.googleapis.com/token';
  const unsigned = b64({ alg: 'RS256', typ: 'JWT' }) + '.' + b64({
    iss: sa.client_email, aud: tokenUri, iat: now, exp: now + 3600,
    scope: 'https://www.googleapis.com/auth/androidpublisher',
  });
  const sig = createSign('RSA-SHA256').update(unsigned).sign(sa.private_key, 'base64url');
  const r = await fetch(tokenUri, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer', assertion: `${unsigned}.${sig}` }),
  });
  const j = await r.json();
  if (!r.ok) throw new Error(`Token request failed: ${r.status} ${JSON.stringify(j)}`);
  return j.access_token;
}

function client(token) {
  return async (method, url, body, headers = {}) => {
    const init = { method, headers: { Authorization: `Bearer ${token}`, ...headers } };
    if (body !== undefined) {
      if (Buffer.isBuffer(body)) init.body = body;
      else { init.body = JSON.stringify(body); init.headers['Content-Type'] = 'application/json'; }
    }
    const r = await fetch(url, init);
    const text = await r.text();
    if (!r.ok) throw new Error(`${method} ${url.replace(/\?.*/, '')} failed: ${r.status} ${text}`);
    return text ? JSON.parse(text) : {};
  };
}

async function main() {
  const a = args();
  const sa = serviceAccount();
  const call = client(await accessToken(sa));
  const app = `${API}/${a.package}`;
  console.log(`Service account: ${sa.client_email}, app: ${a.package}`);

  const edit = await call('POST', `${app}/edits`, {});
  const editUrl = `${app}/edits/${edit.id}`;
  try {
    if (a.validate) {
      const { tracks = [] } = await call('GET', `${editUrl}/tracks`);
      for (const t of tracks) {
        const rel = (t.releases || []).map(r => `${r.name || '-'} [${r.status}] codes ${(r.versionCodes || []).join(',')}`);
        console.log(`  ${t.track}: ${rel.join('; ') || 'no releases'}`);
      }
      console.log('Play access OK');
      return;
    }

    if (a.listing) {
      const { listings = [] } = await call('GET', `${editUrl}/listings`);
      for (const l of listings.filter(l => !process.argv.includes('--lang') || l.language === a.lang)) {
        console.log(`--- ${l.language}\ntitle: ${l.title}\nshort: ${l.shortDescription}\nfull:\n${l.fullDescription}\n`);
      }
      const details = await call('GET', `${editUrl}/details`);
      console.log(`default language: ${details.defaultLanguage}`);
      return;
    }

    if (a.setListing) {
      const body = {};
      if (a.title) body.title = a.title;
      if (a.short) body.shortDescription = a.short;
      if (a['full-file']) body.fullDescription = readFileSync(a['full-file'], 'utf8').trim();
      if (Object.keys(body).length) {
        await call('PATCH', `${editUrl}/listings/${a.lang}`, body);
        console.log(`Listing ${a.lang} updated: ${Object.keys(body).join(', ')}`);
      }
      for (const [opt, type] of [['icon', 'icon'], ['feature-graphic', 'featureGraphic']]) {
        if (!a[opt]) continue;
        const imgs = `${editUrl}/listings/${a.lang}/${type}`;
        await call('DELETE', imgs);
        await call('POST', imgs.replace(API, UPLOAD_API) + '?uploadType=media', readFileSync(a[opt]),
          { 'Content-Type': 'image/png' });
        console.log(`${type} replaced (${a.lang})`);
      }
      const committed = await call('POST', `${editUrl}:commit`);
      console.log(`Committed edit ${committed.id}`);
      return;
    }

    const size = statSync(a.aab).size;
    console.log(`Uploading ${a.aab} (${Math.round(size / 1048576)} MB)...`);
    const bundle = await call('POST', `${UPLOAD_API}/${a.package}/edits/${edit.id}/bundles?uploadType=media`,
      readFileSync(a.aab), { 'Content-Type': 'application/octet-stream' });
    const code = String(bundle.versionCode);
    console.log(`Uploaded version code ${code}`);

    const release = { versionCodes: [code], status: a.status };
    if (a.name) release.name = a.name;
    if (a.notes) release.releaseNotes = [{ language: a.lang, text: a.notes }];
    await call('PUT', `${editUrl}/tracks/${a.track}`, { track: a.track, releases: [release] });
    const committed = await call('POST', `${editUrl}:commit`);
    console.log(`Released ${code} on the ${a.track} track (${a.status}), edit ${committed.id}`);
  } finally {
    // An uncommitted edit only blocks other edits, delete it (fails harmlessly after a commit)
    await call('DELETE', editUrl).catch(() => {});
  }
}

main().catch(e => { console.error(e.message); process.exit(1); });
