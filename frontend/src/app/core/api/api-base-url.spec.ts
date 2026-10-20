import { applyApiBaseUrl, normalizeBackendUrl } from './api-base-url';

describe('normalizeBackendUrl', () => {
  it('returns empty when unset', () => {
    expect(normalizeBackendUrl(undefined)).toBe('');
    expect(normalizeBackendUrl('')).toBe('');
    expect(normalizeBackendUrl('   ')).toBe('');
  });

  it('strips trailing slashes and an optional /api suffix', () => {
    expect(normalizeBackendUrl('https://api.example.com/')).toBe('https://api.example.com');
    expect(normalizeBackendUrl('https://api.example.com/api')).toBe('https://api.example.com');
    expect(normalizeBackendUrl('https://api.example.com/api/')).toBe('https://api.example.com');
  });
});

describe('applyApiBaseUrl', () => {
  const backend = 'https://schoolms.onrender.com';

  it('keeps relative /api paths when no backend URL is configured', () => {
    expect(applyApiBaseUrl('/api/auth/login', '')).toBe('/api/auth/login');
  });

  it('prefixes relative /api paths with the backend origin', () => {
    expect(applyApiBaseUrl('/api/auth/login', backend)).toBe(
      'https://schoolms.onrender.com/api/auth/login',
    );
  });

  it('leaves asset requests and absolute URLs unchanged', () => {
    expect(applyApiBaseUrl('/assets/i18n/en.json', backend)).toBe('/assets/i18n/en.json');
    expect(applyApiBaseUrl('https://cdn.example.com/api/x', backend)).toBe(
      'https://cdn.example.com/api/x',
    );
  });
});
