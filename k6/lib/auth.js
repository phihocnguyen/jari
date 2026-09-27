import http from 'k6/http';
import { check, fail } from 'k6';
import { getConfig } from './config.js';
import { JSON_HEADERS } from './http.js';

function extractToken(res) {
  try {
    return res.json('data.accessToken') || null;
  } catch {
    return null;
  }
}

function loginOnce(baseUrl, email, password) {
  return http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: JSON_HEADERS, tags: { name: 'POST /auth/login' } },
  );
}

function registerOnce(baseUrl, email, password) {
  const username = `k6_${String(email).split('@')[0]}`.replace(/[^a-zA-Z0-9_]/g, '_').slice(0, 50);
  return http.post(
    `${baseUrl}/api/v1/auth/register`,
    JSON.stringify({
      email,
      password,
      username,
      displayName: 'k6 load user',
    }),
    { headers: JSON_HEADERS, tags: { name: 'POST /auth/register' } },
  );
}

/**
 * Login with AUTH_EMAIL / AUTH_PASSWORD.
 * If login fails, try register once then login again.
 * Fails the script with a clear message when auth still cannot succeed.
 */
export function ensureAuth() {
  const { baseUrl, email, password } = getConfig();
  if (!email || !password) {
    fail('AUTH_EMAIL and AUTH_PASSWORD are required in k6/.env');
  }

  let res = loginOnce(baseUrl, email, password);
  let token = extractToken(res);

  check(res, {
    'login status 200': (r) => r.status === 200,
    'login returns token': () => !!token,
  });

  if (token) {
    return token;
  }

  // User missing / bad password → try register (201) then login
  const reg = registerOnce(baseUrl, email, password);
  check(reg, {
    'register status 201 or conflict': (r) => r.status === 201 || r.status === 409 || r.status === 400,
  });

  if (reg.status === 201) {
    token = extractToken(reg);
    if (token) {
      return token;
    }
  }

  res = loginOnce(baseUrl, email, password);
  token = extractToken(res);
  check(res, {
    'login after register status 200': (r) => r.status === 200,
    'login after register returns token': () => !!token,
  });

  if (!token) {
    fail(
      `Auth failed for ${email}. login=${res.status} body=${String(res.body).slice(0, 300)}. ` +
        `Register a user first or fix AUTH_EMAIL / AUTH_PASSWORD in k6/.env`,
    );
  }

  return token;
}

/** @deprecated use ensureAuth */
export function login() {
  return ensureAuth();
}
