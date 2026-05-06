export const API_BASE = resolveApiBase(import.meta.env.VITE_API_BASE_URL, import.meta.env.PROD);

export function resolveApiBase(configuredApiBase: string | undefined, isProduction: boolean) {
  return configuredApiBase ?? (isProduction ? '' : 'http://localhost:8080');
}

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers
    },
    ...options
  });

  if (!response.ok) {
    throw new ApiError(await getErrorMessage(response), response.status);
  }

  return response.json();
}

async function getErrorMessage(response: Response): Promise<string> {
  const text = await response.text();

  if (!text) {
    return `Request failed with status ${response.status}.`;
  }

  try {
    const parsed = JSON.parse(text) as Partial<Record<'message' | 'detail' | 'error', string>>;
    return parsed.message ?? parsed.detail ?? parsed.error ?? text;
  } catch {
    return text;
  }
}
