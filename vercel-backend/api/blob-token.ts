import { generateClientTokenFromReadWriteToken } from '@vercel/blob/client';
import { createRemoteJWKSet, jwtVerify } from 'jose';

const FIREBASE_PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID?.trim() || 'gen-lang-client-0409270927';

// Module-cached JWKS keeps warm Firebase ID token verification under ~1ms without firebase-admin
const FIREBASE_JWKS = createRemoteJWKSet(
  new URL('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com'),
  { cacheMaxAge: 6 * 60 * 60 * 1000, cooldownDuration: 30 * 1000 }
);

interface RequestedFile {
  pathname?: string;
  contentType?: string;
}

interface TokenRequestBody {
  pathname?: string;
  contentType?: string;
  files?: RequestedFile[];
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'content-type': 'application/json; charset=utf-8',
      'cache-control': 'no-store',
    },
  });
}

function sanitizePathname(raw: string | undefined, uid: string): string | null {
  if (!raw || typeof raw !== 'string') return null;
  const normalized = raw.trim().replace(/^\/+/, '');
  if (!normalized || normalized.length > 240) return null;
  if (normalized.includes('..') || normalized.includes('\\') || normalized.includes('?')) {
    return null;
  }
  if (!/^[a-zA-Z0-9._\-\/]+$/.test(normalized)) return null;
  if (normalized.startsWith(`campus/${uid}/`)) return normalized;
  return `campus/${uid}/${normalized.replace(/^campus\//, '')}`;
}

function isAllowedMimeType(mime: string | undefined): boolean {
  if (!mime || typeof mime !== 'string') return false;
  const clean = mime.trim().toLowerCase();
  return clean.startsWith('image/') || clean.startsWith('video/');
}

async function verifyFirebaseUser(authHeader: string | null): Promise<string> {
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    throw new Error('Missing Firebase Authorization Bearer token');
  }
  const idToken = authHeader.slice(7).trim();
  if (!idToken) throw new Error('Empty Firebase ID token');

  const { payload } = await jwtVerify(idToken, FIREBASE_JWKS, {
    issuer: `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`,
    audience: FIREBASE_PROJECT_ID,
  });

  const uid = typeof payload.sub === 'string' ? payload.sub.trim() : '';
  if (!uid) throw new Error('Firebase token missing subject');
  return uid;
}

export async function GET(): Promise<Response> {
  return jsonResponse({ status: 'ok', message: 'Send POST with Firebase ID token to mint upload tokens.' }, 200);
}

export async function POST(request: Request): Promise<Response> {
  const rwToken = process.env.BLOB_READ_WRITE_TOKEN?.trim();
  if (!rwToken) {
    return jsonResponse(
      { error: 'Server missing BLOB_READ_WRITE_TOKEN environment variable' },
      500
    );
  }

  let uid: string;
  try {
    uid = await verifyFirebaseUser(request.headers.get('authorization'));
  } catch (err) {
    return jsonResponse(
      { error: err instanceof Error ? err.message : 'Unauthorized' },
      401
    );
  }

  let body: TokenRequestBody;
  try {
    body = (await request.json()) as TokenRequestBody;
  } catch {
    return jsonResponse({ error: 'Invalid JSON request body' }, 400);
  }

  const requestedFiles: RequestedFile[] = Array.isArray(body.files) && body.files.length > 0
    ? body.files
    : [{ pathname: body.pathname, contentType: body.contentType }];

  try {
    const tokens = await Promise.all(
      requestedFiles.map(async (item) => {
        const pathname = sanitizePathname(item.pathname, uid);
        if (!pathname) {
          throw new Error('Invalid upload pathname');
        }
        const allowedContentTypes = isAllowedMimeType(item.contentType)
          ? [item.contentType!.trim().toLowerCase(), 'image/*', 'video/*']
          : ['image/*', 'video/*'];

        const clientToken = await generateClientTokenFromReadWriteToken({
          token: rwToken,
          pathname,
          allowedContentTypes,
          addRandomSuffix: true,
          validUntil: Date.now() + 10 * 60 * 1000,
        });

        return { pathname, clientToken };
      })
    );

    const first = tokens[0];
    return jsonResponse({
      clientToken: first?.clientToken ?? '',
      pathname: first?.pathname ?? '',
      tokens,
    });
  } catch (err) {
    return jsonResponse(
      { error: err instanceof Error ? err.message : 'Failed to mint upload token' },
      400
    );
  }
}
