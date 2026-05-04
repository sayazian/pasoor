import { API_BASE, request } from './http';
import type { CurrentUser, PreferredTheme } from '../types/auth';

export function googleLoginUrl() {
  return `${API_BASE}/oauth2/authorization/google`;
}

export async function getCurrentUser(): Promise<CurrentUser | null> {
  const response = await fetch(`${API_BASE}/api/me`, {
    credentials: 'include'
  });

  if (response.status === 401 || response.status === 403) {
    return null;
  }
  if (!response.ok) {
    throw new Error(await response.text());
  }

  return response.json();
}

export async function logout(): Promise<void> {
  await fetch(`${API_BASE}/api/logout`, {
    method: 'POST',
    credentials: 'include'
  });
}

export function requireCurrentUser(): Promise<CurrentUser> {
  return request<CurrentUser>('/api/me');
}

export function updateProfile(profile: { name: string; preferredTheme: PreferredTheme }): Promise<CurrentUser> {
  return request<CurrentUser>('/api/me/profile', {
    method: 'PATCH',
    body: JSON.stringify(profile)
  });
}
