# Squadify Fast Edge Media Token Backend

This lightweight Vercel Edge endpoint (`api/blob-token.ts`) mints short-lived, per-file Vercel Blob client tokens (`vercel_blob_client_...`) after verifying the caller's Firebase Auth ID token via cached Google JWKS—so the Android app uploads photos and videos **directly** to `blob.vercel-storage.com` at full speed with zero size proxy limits and without bundling `BLOB_READ_WRITE_TOKEN` in the APK.

## Deploy in 2 steps

1. Inside `vercel-backend/`, deploy to Vercel and add your `BLOB_READ_WRITE_TOKEN` in **Vercel Project Settings -> Environment Variables** (or `vercel env add BLOB_READ_WRITE_TOKEN production`):
   ```bash
   cd vercel-backend
   npm install
   npx vercel --prod
   ```
2. Copy your deployed endpoint URL (e.g. `https://<your-project>.vercel.app/api/blob-token`) and set `MEDIA_TOKEN_ENDPOINT_URL` in your Android build environment (e.g. `MEDIA_TOKEN_ENDPOINT_URL=https://<your-project>.vercel.app/api/blob-token`) and rebuild the APK.
