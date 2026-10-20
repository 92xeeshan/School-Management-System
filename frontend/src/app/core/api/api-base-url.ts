export function normalizeBackendUrl(raw: string | null | undefined): string {
  if (!raw) {
    return '';
  }
  return raw.trim().replace(/\/+$/, '').replace(/\/api$/i, '');
}

export function applyApiBaseUrl(url: string, backendUrl: string): string {
  if (!backendUrl) {
    return url;
  }
  if (/^https?:\/\//i.test(url)) {
    return url;
  }
  const path = url.startsWith('/') ? url : `/${url}`;
  if (!path.startsWith('/api/') && path !== '/api') {
    return url;
  }
  return `${backendUrl}${path}`;
}
