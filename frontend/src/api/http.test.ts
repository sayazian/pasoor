import { describe, expect, it } from 'vitest';
import { resolveApiBase } from './http';

describe('resolveApiBase', () => {
  it('uses relative API URLs in production when no API base is configured', () => {
    expect(resolveApiBase(undefined, true)).toBe('');
  });

  it('uses the local backend URL in development when no API base is configured', () => {
    expect(resolveApiBase(undefined, false)).toBe('http://localhost:8080');
  });

  it('uses an explicitly configured API base', () => {
    expect(resolveApiBase('https://backend.example.com', true)).toBe('https://backend.example.com');
  });
});
