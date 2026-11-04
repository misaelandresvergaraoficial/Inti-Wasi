export async function onRequest({ request, env }) {
  let backend;
  try {
    backend = new URL(env.BACKEND_ORIGIN);
    if (backend.protocol !== 'https:' || backend.username || backend.password
        || backend.pathname !== '/' || backend.search || backend.hash) {
      throw new Error('Invalid backend origin');
    }
  } catch {
    return Response.json({ mensaje: 'El servicio no está configurado.' }, {
      status: 503,
      headers: { 'Cache-Control': 'no-store' },
    });
  }

  const incoming = new URL(request.url);
  backend.pathname = incoming.pathname;
  backend.search = incoming.search;
  const headers = new Headers(request.headers);
  for (const name of [...headers.keys()]) {
    if (name === 'host' || name === 'forwarded' || name.startsWith('x-forwarded-')) {
      headers.delete(name);
    }
  }

  let upstream;
  try {
    upstream = await fetch(new Request(backend, {
      method: request.method,
      headers,
      body: ['GET', 'HEAD'].includes(request.method) ? undefined : request.body,
      duplex: 'half',
      redirect: 'manual',
    }), { cf: { cacheEverything: false } });
  } catch {
    return Response.json({ mensaje: 'El servicio no está disponible. Inténtalo nuevamente.' }, {
      status: 502,
      headers: { 'Cache-Control': 'no-store' },
    });
  }

  if (upstream.status >= 300 && upstream.status < 400) {
    return Response.json({ mensaje: 'El servicio devolvió una respuesta inesperada.' }, {
      status: 502,
      headers: { 'Cache-Control': 'no-store' },
    });
  }

  const outHeaders = new Headers(upstream.headers);
  const cookies = typeof upstream.headers.getSetCookie === 'function'
    ? upstream.headers.getSetCookie()
    : upstream.headers.getAll('set-cookie') || [];
  if (cookies.length) {
    outHeaders.delete('set-cookie');
    for (const cookie of cookies) {
      outHeaders.append('set-cookie', cookie);
    }
  }
  outHeaders.set('Cache-Control', 'no-store');
  return new Response(upstream.body, {
    status: upstream.status,
    statusText: upstream.statusText,
    headers: outHeaders,
  });
}
