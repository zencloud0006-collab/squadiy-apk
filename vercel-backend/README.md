# Squadify Fast Edge Media Token Backend

This lightweight Vercel Edge endpoint (`api/blob-token.ts`) mints short-lived, per-file Vercel Blob client tokens (`vercel_blob_client_...`) after verifying the caller's Firebase Auth ID token via cached Google JWKS—so the Android app uploads photos and videos **directly** to `blob.vercel-storage.com` at full speed with zero size proxy limits and without bundling `BLOB_READ_WRITE_TOKEN` in the APK.

## Deploy via Vercel Dashboard (GitHub Import)

1. **Root Directory**: `vercel-backend`
2. **Framework Preset**: `Other`
3. **Build Command**: leave default (or `npm run build`)
4. **Output Directory**: `public` (or leave default)
5. **Install Command**: `npm install` (default)
6. **Environment Variables**:
   - `BLOB_READ_WRITE_TOKEN` = your Vercel Blob read/write token
   - `FIREBASE_PROJECT_ID` = `gen-lang-client-0409270927` *(optional, already defaulted)*

## After Deploying

Copy your deployed endpoint URL:
`https://<your-vercel-app>.vercel.app/api/blob-token`

Then pass `MEDIA_TOKEN_ENDPOINT_URL` into the Android build (public URL, safe for APKs).
